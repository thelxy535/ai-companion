"""Administrative CLI for pairing-code and device lifecycle operations."""

from __future__ import annotations

import argparse
import json

from .config import Settings
from .repository import GatewayRepository
from .security import generate_pairing_code


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="cc-vision-admin")
    subparsers = parser.add_subparsers(dest="command", required=True)

    generate = subparsers.add_parser(
        "generate-code", help="generate a short-lived one-time pairing code"
    )
    generate.add_argument(
        "--ttl-seconds", type=int, default=600, choices=range(1, 3601), metavar="1..3600"
    )

    subparsers.add_parser("list-devices", help="list devices without credential material")

    revoke = subparsers.add_parser("revoke-device", help="revoke one device by UUID")
    revoke.add_argument("device_id")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    settings = Settings.from_env()
    repository = GatewayRepository(settings.database_path, settings.pepper)
    repository.initialize()

    if args.command == "generate-code":
        code = generate_pairing_code()
        expires_at = repository.create_pairing_code(code, args.ttl_seconds)
        print(
            json.dumps(
                {"pairing_code": code, "expires_at": expires_at},
                separators=(",", ":"),
            )
        )
        return 0

    if args.command == "list-devices":
        devices = [
            {
                "device_id": device.id,
                "installation_id": device.installation_id,
                "created_at": device.created_at,
                "revoked_at": device.revoked_at,
            }
            for device in repository.list_devices()
        ]
        print(json.dumps({"devices": devices}, separators=(",", ":")))
        return 0

    if args.command == "revoke-device":
        revoked = repository.revoke_device(args.device_id)
        print(json.dumps({"revoked": revoked}, separators=(",", ":")))
        return 0 if revoked else 1

    raise AssertionError("argparse accepted an unknown command")


if __name__ == "__main__":
    raise SystemExit(main())
