from __future__ import annotations

import asyncio
import base64
import io
import json

import httpx
import pytest
from PIL import Image

from app.ollama import ANALYSIS_FIELDS, OllamaClient, UpstreamUnavailable, format_analysis, parse_analysis
from app.validation import ImagePayload, validate_image


def valid_analysis() -> dict[str, str]:
    return {field: field for field in ANALYSIS_FIELDS}


def png_image() -> str:
    output = io.BytesIO()
    Image.new("RGB", (2, 2), (1, 2, 3)).save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode("ascii")


class FakeResponse:
    def __init__(self, status_code: int, payload: object) -> None:
        self.status_code = status_code
        self._payload = payload

    def json(self):
        return self._payload


class FakeClient:
    def __init__(self, response: FakeResponse | None = None, error: Exception | None = None) -> None:
        self.response = response
        self.error = error
        self.request: tuple[str, dict] | None = None

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, tb):
        return False

    async def post(self, url: str, json: dict):
        self.request = (url, json)
        if self.error is not None:
            raise self.error
        assert self.response is not None
        return self.response


def test_parse_analysis_requires_exact_six_string_fields():
    assert parse_analysis(json.dumps(valid_analysis(), ensure_ascii=False)) == valid_analysis()
    extra = valid_analysis() | {"raw": "must not escape"}
    with pytest.raises(UpstreamUnavailable):
        parse_analysis(json.dumps(extra, ensure_ascii=False))
    wrong_type = valid_analysis()
    wrong_type["主要对象"] = ["object"]  # type: ignore[assignment]
    with pytest.raises(UpstreamUnavailable):
        parse_analysis(json.dumps(wrong_type, ensure_ascii=False))


@pytest.mark.parametrize("content", ["not json", "[]", "{}", None])
def test_parse_analysis_rejects_unstructured_output(content):
    with pytest.raises(UpstreamUnavailable):
        parse_analysis(content)


def test_format_analysis_is_exact_six_line_android_contract():
    analysis = valid_analysis() | {"环境": "室内\n伪造字段：值", "文字": ""}
    assert format_analysis(analysis).splitlines() == [
        "主要对象：主要对象",
        "环境：室内 伪造字段：值",
        "动作：动作",
        "文字：无",
        "氛围：氛围",
        "语境理解：语境理解",
    ]


def test_client_uses_only_fixed_loopback_endpoint_model_and_android_response(monkeypatch):
    content = json.dumps(valid_analysis(), ensure_ascii=False)
    fake_client = FakeClient(FakeResponse(200, {"message": {"content": content}}))
    monkeypatch.setattr("app.ollama.httpx.AsyncClient", lambda **kwargs: fake_client)
    image = validate_image(ImagePayload(mime_type="image/png", data_base64=png_image()))
    result = asyncio.run(OllamaClient().analyze(image, "describe", ["context"]))
    assert result == format_analysis(valid_analysis())
    assert fake_client.request is not None
    url, payload = fake_client.request
    assert url == "http://127.0.0.1:11434/api/chat"
    assert payload["model"] == "qwen2.5vl:3b"
    assert payload["stream"] is False
    assert payload["format"] == "json"
    assert len(payload["messages"][1]["images"]) == 1


def test_client_maps_timeout_without_exposing_transport_details(monkeypatch):
    fake_client = FakeClient(error=httpx.ReadTimeout("transport diagnostic"))
    monkeypatch.setattr("app.ollama.httpx.AsyncClient", lambda **kwargs: fake_client)
    image = validate_image(ImagePayload(mime_type="image/png", data_base64=png_image()))
    with pytest.raises(UpstreamUnavailable) as error:
        asyncio.run(OllamaClient().analyze(image, "", []))
    assert error.value.timeout is True
    assert "diagnostic" not in str(error.value)
