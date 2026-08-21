"""Configuration for the local-only vision gateway."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


DATA_DIR_ENV = "CC_VISION_DATA_DIR"
PEPPER_ENV = "CC_VISION_TOKEN_PEPPER"
DEFAULT_DATA_DIR = Path("/data/cc-vision-gateway")
# A 4 MiB binary image becomes about 5.34 MiB when Base64 encoded. This is a
# transport limit only; validate_image still enforces the 4 MiB image limit.
MAX_REQUEST_BYTES = 6 * 1024 * 1024
MAX_IMAGE_BYTES = 4 * 1024 * 1024
MAX_IMAGE_PIXELS = 16_000_000
MAX_USER_TEXT_CHARS = 2_000
MAX_CONTEXT_ITEMS = 3
MAX_CONTEXT_ITEM_CHARS = 1_000
MODEL_RESPONSE_MAX_CHARS = 12_000
OLLAMA_URL = "http://127.0.0.1:11434/api/chat"
OLLAMA_MODEL = "qwen2.5vl:3b"
OLLAMA_TIMEOUT_SECONDS = 240.0
RATE_LIMIT_REQUESTS = 6
RATE_LIMIT_WINDOW_SECONDS = 60.0


@dataclass(frozen=True, slots=True)
class Settings:
    """Immutable runtime configuration sourced only from explicit env vars."""

    data_dir: Path
    pepper: str
    rate_limit_requests: int = RATE_LIMIT_REQUESTS
    rate_limit_window_seconds: float = RATE_LIMIT_WINDOW_SECONDS

    @property
    def database_path(self) -> Path:
        return self.data_dir / "gateway.sqlite3"

    @classmethod
    def from_env(cls) -> "Settings":
        raw_data_dir = os.environ.get(DATA_DIR_ENV, str(DEFAULT_DATA_DIR))
        data_dir = Path(raw_data_dir).expanduser()
        if not data_dir.is_absolute():
            raise ValueError(f"{DATA_DIR_ENV} must be an absolute path")

        pepper = os.environ.get(PEPPER_ENV, "")
        if len(pepper) < 32:
            raise ValueError(f"{PEPPER_ENV} must contain at least 32 characters")
        if pepper.lower().startswith("replace-with-"):
            raise ValueError(f"{PEPPER_ENV} still contains the example value")

        return cls(data_dir=data_dir, pepper=pepper)
