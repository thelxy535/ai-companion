from __future__ import annotations

import json

from app.admin import main
from app.config import DATA_DIR_ENV, PEPPER_ENV


def configure_admin(monkeypatch, tmp_path):
    monkeypatch.setenv(DATA_DIR_ENV, str(tmp_path / "state"))
    monkeypatch.setenv(
        PEPPER_ENV, "admin-test-pepper-that-is-at-least-thirty-two-characters"
    )


def test_admin_generates_lists_and_revokes_without_tokens(monkeypatch, tmp_path, capsys):
    configure_admin(monkeypatch, tmp_path)
    assert main(["generate-code", "--ttl-seconds", "60"]) == 0
    generated = json.loads(capsys.readouterr().out)
    assert set(generated) == {"pairing_code", "expires_at"}
    assert len(generated["pairing_code"]) >= 20

    assert main(["list-devices"]) == 0
    listed = json.loads(capsys.readouterr().out)
    assert listed == {"devices": []}
    assert "token" not in json.dumps(listed).lower()

    assert main(["revoke-device", "00000000-0000-0000-0000-000000000000"]) == 1
    assert json.loads(capsys.readouterr().out) == {"revoked": False}
