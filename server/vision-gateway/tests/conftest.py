from __future__ import annotations

import base64
import io

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.config import Settings
from app.main import create_app
from app.ollama import ANALYSIS_FIELDS, format_analysis
from app.security import generate_pairing_code


class SuccessfulOllama:
    async def analyze(self, image, user_text, context):
        return format_analysis({field: f"test-{field}" for field in ANALYSIS_FIELDS})


@pytest.fixture
def settings(tmp_path):
    return Settings(
        data_dir=tmp_path / "state",
        pepper="unit-test-pepper-that-is-at-least-thirty-two-characters",
        rate_limit_requests=2,
        rate_limit_window_seconds=60.0,
    )


@pytest.fixture
def client(settings):
    app = create_app(settings)
    with TestClient(app) as test_client:
        test_client.app.state.ollama = SuccessfulOllama()
        yield test_client


@pytest.fixture
def png_base64() -> str:
    output = io.BytesIO()
    Image.new("RGB", (2, 2), (10, 20, 30)).save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode("ascii")


def pair_device(client: TestClient, installation_id: str = "install-1") -> tuple[str, str, str]:
    code = generate_pairing_code()
    client.app.state.repository.create_pairing_code(code, 600)
    response = client.post(
        "/v1/pair",
        json={"pairing_code": code, "installation_id": installation_id},
    )
    assert response.status_code == 201
    body = response.json()
    return code, body["device_token"], body["device_id"]


def analyze_payload(png_base64: str, *, user_text: str = "describe") -> dict:
    return {
        "image_base64": png_base64,
        "mime_type": "image/png",
        "user_text": user_text,
        "conversation_context": ["previous context"],
    }
