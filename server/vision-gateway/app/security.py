"""Small security primitives; plaintext credentials never reach persistence."""

from __future__ import annotations

import hashlib
import secrets


def credential_hash(kind: str, value: str, pepper: str) -> str:
    """Return a domain-separated SHA-256 hash for a credential."""
    material = f"cc-vision-gateway:{kind}\x00{pepper}\x00{value}".encode("utf-8")
    return hashlib.sha256(material).hexdigest()


def generate_pairing_code() -> str:
    """Generate an opaque URL-safe one-time pairing secret."""
    return secrets.token_urlsafe(24)


def generate_device_token() -> str:
    """Generate an opaque bearer token for a paired installation."""
    return "cvg_" + secrets.token_urlsafe(32)
