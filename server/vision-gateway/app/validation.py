"""Strict, bounded request and image validation."""

from __future__ import annotations

import base64
import binascii
import io
import json
import warnings
from dataclasses import dataclass

from PIL import Image, UnidentifiedImageError
from pydantic import BaseModel, ConfigDict, Field, StrictStr, ValidationError

from .config import (
    MAX_CONTEXT_ITEMS,
    MAX_CONTEXT_ITEM_CHARS,
    MAX_IMAGE_BYTES,
    MAX_IMAGE_PIXELS,
    MAX_USER_TEXT_CHARS,
)

ALLOWED_MIME_FORMATS = {
    "image/jpeg": "JPEG",
    "image/png": "PNG",
    "image/webp": "WEBP",
}
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


class InputRejected(Exception):
    """A client supplied malformed, excessive, or unsafe input."""

    def __init__(self, detail: str, status_code: int = 400) -> None:
        self.detail = detail
        self.status_code = status_code
        super().__init__(detail)


class DuplicateJsonKey(ValueError):
    pass


class ImagePayload(BaseModel):
    """Internal image representation after the public request is parsed."""

    model_config = ConfigDict(extra="forbid", strict=True)

    mime_type: StrictStr
    data_base64: StrictStr


class PairRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    pairing_code: StrictStr = Field(min_length=20, max_length=128, pattern=r"^[A-Za-z0-9_-]+$")
    installation_id: StrictStr = Field(
        min_length=1, max_length=128, pattern=r"^[A-Za-z0-9._:-]+$"
    )


class AnalyzeRequest(BaseModel):
    """Public Android request shape; keep it aligned with SelfHostedVisionProvider."""

    model_config = ConfigDict(extra="forbid", strict=True)

    image_base64: StrictStr
    mime_type: StrictStr
    user_text: StrictStr = Field(max_length=MAX_USER_TEXT_CHARS)
    conversation_context: list[StrictStr] = Field(
        default_factory=list, max_length=MAX_CONTEXT_ITEMS
    )

    def image_payload(self) -> ImagePayload:
        return ImagePayload(
            mime_type=self.mime_type,
            data_base64=self.image_base64,
        )


@dataclass(frozen=True, slots=True)
class ValidatedImage:
    mime_type: str
    data: bytes
    width: int
    height: int


def parse_pair_request(raw_body: bytes) -> PairRequest:
    return _parse_model(raw_body, PairRequest)


def parse_analyze_request(raw_body: bytes) -> AnalyzeRequest:
    request = _parse_model(raw_body, AnalyzeRequest)
    if any(len(item) > MAX_CONTEXT_ITEM_CHARS for item in request.conversation_context):
        raise InputRejected("Invalid request body")
    return request


def validate_image(payload: ImagePayload) -> ValidatedImage:
    """Decode only canonical base64 and prove MIME, magic bytes, and pixels agree."""
    if payload.mime_type not in ALLOWED_MIME_FORMATS:
        raise InputRejected("Unsupported image type")
    if not payload.data_base64:
        raise InputRejected("Invalid image encoding")

    try:
        data = base64.b64decode(payload.data_base64.encode("ascii"), validate=True)
    except (UnicodeEncodeError, binascii.Error, ValueError):
        raise InputRejected("Invalid image encoding") from None

    if not data or len(data) > MAX_IMAGE_BYTES:
        raise InputRejected("Image exceeds 4 MiB limit", status_code=413)

    magic_mime = _mime_from_signature(data)
    if magic_mime != payload.mime_type:
        raise InputRejected("Image type does not match its content")

    expected_format = ALLOWED_MIME_FORMATS[payload.mime_type]
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(io.BytesIO(data)) as image:
                image_format = image.format
                width, height = image.size
                if getattr(image, "n_frames", 1) != 1:
                    raise InputRejected("Animated images are not supported")
                if width < 1 or height < 1 or width * height > MAX_IMAGE_PIXELS:
                    raise InputRejected("Image exceeds pixel limit")
                image.verify()
            # Re-open and fully decode after verify() invalidates the first handle.
            with Image.open(io.BytesIO(data)) as image:
                image.load()
    except InputRejected:
        raise
    except (Image.DecompressionBombError, Image.DecompressionBombWarning):
        raise InputRejected("Image exceeds pixel limit") from None
    except (UnidentifiedImageError, OSError, SyntaxError, ValueError):
        raise InputRejected("Invalid image data") from None

    if image_format != expected_format:
        raise InputRejected("Image type does not match its content")
    return ValidatedImage(payload.mime_type, data, width, height)


def _parse_model(raw_body: bytes, model_type: type[PairRequest] | type[AnalyzeRequest]):
    try:
        decoded = json.loads(raw_body, object_pairs_hook=_reject_duplicate_keys)
        return model_type.model_validate(decoded)
    except (
        UnicodeDecodeError,
        json.JSONDecodeError,
        RecursionError,
        DuplicateJsonKey,
        ValidationError,
    ):
        raise InputRejected("Invalid request body") from None


def _reject_duplicate_keys(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result: dict[str, object] = {}
    for key, value in pairs:
        if key in result:
            raise DuplicateJsonKey(key)
        result[key] = value
    return result


def _mime_from_signature(data: bytes) -> str | None:
    if data.startswith(PNG_SIGNATURE):
        return "image/png"
    if len(data) >= 12 and data.startswith(b"RIFF") and data[8:12] == b"WEBP":
        return "image/webp"
    if data.startswith(b"\xff\xd8\xff"):
        return "image/jpeg"
    return None
