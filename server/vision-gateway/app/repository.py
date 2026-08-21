"""SQLite state repository for pairing codes and revocable device credentials."""

from __future__ import annotations

import os
import sqlite3
import uuid
from contextlib import closing
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path

from .security import credential_hash


@dataclass(frozen=True, slots=True)
class Device:
    id: str
    installation_id: str
    created_at: str
    revoked_at: str | None


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def utc_timestamp(value: datetime | None = None) -> str:
    return (value or utc_now()).isoformat(timespec="seconds")


class GatewayRepository:
    """Each operation uses a short SQLite transaction and stores hashes only."""

    def __init__(self, database_path: Path, pepper: str) -> None:
        self.database_path = database_path
        self.pepper = pepper

    def initialize(self) -> None:
        self.database_path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
        with closing(self._connect()) as conn:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS pairing_codes (
                    id TEXT PRIMARY KEY,
                    code_hash TEXT NOT NULL UNIQUE,
                    created_at TEXT NOT NULL,
                    expires_at TEXT NOT NULL,
                    used_at TEXT
                );
                CREATE INDEX IF NOT EXISTS idx_pairing_codes_lookup
                    ON pairing_codes(code_hash, expires_at);

                CREATE TABLE IF NOT EXISTS devices (
                    id TEXT PRIMARY KEY,
                    installation_id TEXT NOT NULL,
                    token_hash TEXT NOT NULL UNIQUE,
                    created_at TEXT NOT NULL,
                    revoked_at TEXT
                );
                CREATE INDEX IF NOT EXISTS idx_devices_token_active
                    ON devices(token_hash, revoked_at);
                CREATE INDEX IF NOT EXISTS idx_devices_installation_active
                    ON devices(installation_id, revoked_at);
                """
            )
        if os.name == "posix":
            os.chmod(self.database_path.parent, 0o700)
            os.chmod(self.database_path, 0o600)

    def create_pairing_code(self, code: str, ttl_seconds: int) -> str:
        if not 1 <= ttl_seconds <= 3600:
            raise ValueError("ttl_seconds must be between 1 and 3600")
        now = utc_now()
        expires_at = now + timedelta(seconds=ttl_seconds)
        with closing(self._connect()) as conn:
            conn.execute(
                "INSERT INTO pairing_codes (id, code_hash, created_at, expires_at, used_at) VALUES (?, ?, ?, ?, NULL)",
                (
                    str(uuid.uuid4()),
                    credential_hash("pairing-code", code, self.pepper),
                    utc_timestamp(now),
                    utc_timestamp(expires_at),
                ),
            )
        return utc_timestamp(expires_at)

    def consume_pairing_code_and_create_device(
        self, code: str, installation_id: str, token: str
    ) -> Device | None:
        """Atomically consume a currently valid code and issue one device record."""
        now = utc_timestamp()
        code_hash = credential_hash("pairing-code", code, self.pepper)
        token_hash = credential_hash("device-token", token, self.pepper)
        device = Device(
            id=str(uuid.uuid4()),
            installation_id=installation_id,
            created_at=now,
            revoked_at=None,
        )
        conn = self._connect()
        try:
            conn.execute("BEGIN IMMEDIATE")
            row = conn.execute(
                "SELECT id FROM pairing_codes WHERE code_hash = ? AND used_at IS NULL AND expires_at > ?",
                (code_hash, now),
            ).fetchone()
            if row is None:
                conn.rollback()
                return None
            # Re-pairing an installation invalidates its prior active token(s).
            conn.execute(
                "UPDATE devices SET revoked_at = ? WHERE installation_id = ? AND revoked_at IS NULL",
                (now, installation_id),
            )
            conn.execute(
                "UPDATE pairing_codes SET used_at = ? WHERE id = ? AND used_at IS NULL",
                (now, row["id"]),
            )
            conn.execute(
                "INSERT INTO devices (id, installation_id, token_hash, created_at, revoked_at) VALUES (?, ?, ?, ?, NULL)",
                (device.id, device.installation_id, token_hash, device.created_at),
            )
            conn.commit()
            return device
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    def active_device_for_token(self, token: str) -> Device | None:
        token_hash = credential_hash("device-token", token, self.pepper)
        with closing(self._connect()) as conn:
            row = conn.execute(
                "SELECT id, installation_id, created_at, revoked_at FROM devices "
                "WHERE token_hash = ? AND revoked_at IS NULL",
                (token_hash,),
            ).fetchone()
        return self._device_from_row(row)

    def list_devices(self) -> list[Device]:
        with closing(self._connect()) as conn:
            rows = conn.execute(
                "SELECT id, installation_id, created_at, revoked_at FROM devices ORDER BY created_at DESC"
            ).fetchall()
        return [self._device_from_row(row) for row in rows if row is not None]

    def revoke_device(self, device_id: str) -> bool:
        with closing(self._connect()) as conn:
            result = conn.execute(
                "UPDATE devices SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL",
                (utc_timestamp(), device_id),
            )
        return result.rowcount == 1

    def _connect(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.database_path, timeout=5.0, isolation_level=None)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA foreign_keys = ON")
        conn.execute("PRAGMA busy_timeout = 5000")
        if os.name != "nt":
            conn.execute("PRAGMA journal_mode = WAL")
        return conn

    @staticmethod
    def _device_from_row(row: sqlite3.Row | None) -> Device | None:
        if row is None:
            return None
        return Device(
            id=row["id"],
            installation_id=row["installation_id"],
            created_at=row["created_at"],
            revoked_at=row["revoked_at"],
        )
