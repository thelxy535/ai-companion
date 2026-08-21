from __future__ import annotations

import asyncio
import base64
import io
import json

import pytest
from PIL import Image

from app.config import MAX_IMAGE_PIXELS
from app.controls import InferenceGate
from app.validation import ImagePayload, InputRejected, parse_analyze_request, validate_image


def encoded_image(format_name: str, size: tuple[int, int] = (2, 2)) -> str:
    output = io.BytesIO()
    Image.new("RGB", size, (1, 2, 3)).save(output, format=format_name)
    return base64.b64encode(output.getvalue()).decode("ascii")


def test_base64_is_strictly_decoded():
    with pytest.raises(InputRejected, match="encoding"):
        validate_image(ImagePayload(mime_type="image/png", data_base64="aGVs bG8="))


def test_declared_mime_must_match_magic_and_decoder():
    jpeg = encoded_image("JPEG")
    with pytest.raises(InputRejected, match="does not match"):
        validate_image(ImagePayload(mime_type="image/png", data_base64=jpeg))


def test_corrupt_payload_with_valid_magic_is_rejected():
    corrupt = base64.b64encode(b"\x89PNG\r\n\x1a\nnot-a-real-png").decode("ascii")
    with pytest.raises(InputRejected, match="Invalid image data"):
        validate_image(ImagePayload(mime_type="image/png", data_base64=corrupt))


def test_jpeg_png_and_webp_are_accepted_when_available():
    for mime_type, image_format in (("image/jpeg", "JPEG"), ("image/png", "PNG"), ("image/webp", "WEBP")):
        try:
            encoded = encoded_image(image_format)
        except OSError:
            if image_format == "WEBP":
                pytest.skip("Pillow build has no WebP encoder")
            raise
        result = validate_image(ImagePayload(mime_type=mime_type, data_base64=encoded))
        assert result.width == 2
        assert result.height == 2


def test_pixel_limit_rejects_decompression_bomb_shape():
    width = 4_001
    height = MAX_IMAGE_PIXELS // width + 1
    encoded = encoded_image("PNG", (width, height))
    with pytest.raises(InputRejected, match="pixel limit"):
        validate_image(ImagePayload(mime_type="image/png", data_base64=encoded))


def test_android_text_and_context_bounds_are_strict():
    base = {"image_base64": "AA==", "mime_type": "image/png", "user_text": "x" * 2_001, "conversation_context": []}
    with pytest.raises(InputRejected, match="Invalid request body"):
        parse_analyze_request(json.dumps(base).encode())
    base["user_text"] = "ok"
    base["conversation_context"] = ["x" * 1_001]
    with pytest.raises(InputRejected, match="Invalid request body"):
        parse_analyze_request(json.dumps(base).encode())
    base["conversation_context"] = ["a", "b", "c", "d"]
    with pytest.raises(InputRejected, match="Invalid request body"):
        parse_analyze_request(json.dumps(base).encode())


def test_duplicate_json_keys_are_rejected():
    with pytest.raises(InputRejected, match="Invalid request body"):
        parse_analyze_request(b'{"mime_type":"image/png","mime_type":"image/jpeg"}')


def test_gate_allows_one_running_one_waiting_and_rejects_third():
    async def scenario() -> None:
        gate = InferenceGate()
        assert await gate.acquire()
        waiting = asyncio.create_task(gate.acquire())
        await asyncio.sleep(0)
        assert await gate.acquire() is False
        await gate.release()
        assert await asyncio.wait_for(waiting, timeout=1.0)
        await gate.release()

    asyncio.run(scenario())
