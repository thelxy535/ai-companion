from __future__ import annotations

import sqlite3

from conftest import analyze_payload, pair_device
from app.config import MAX_REQUEST_BYTES
from app.ollama import ANALYSIS_FIELDS, UpstreamUnavailable
from app.security import credential_hash


def test_health_is_minimal(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
    assert set(response.headers).isdisjoint({"server-version", "x-model"})


def test_pairing_code_is_one_time_and_credentials_are_hashed(client):
    code, token, _ = pair_device(client)
    repeated = client.post(
        "/v1/pair",
        json={"pairing_code": code, "installation_id": "install-2"},
    )
    assert repeated.status_code == 401

    repository = client.app.state.repository
    with sqlite3.connect(repository.database_path) as conn:
        stored_code_hash = conn.execute("SELECT code_hash FROM pairing_codes").fetchone()[0]
        stored_token_hash = conn.execute("SELECT token_hash FROM devices").fetchone()[0]
    assert stored_code_hash == credential_hash("pairing-code", code, repository.pepper)
    assert stored_token_hash == credential_hash("device-token", token, repository.pepper)
    assert code != stored_code_hash
    assert token != stored_token_hash


def test_repairing_installation_revokes_previous_token(client, png_base64):
    _, first_token, _ = pair_device(client, "same-installation")
    _, second_token, _ = pair_device(client, "same-installation")
    first = client.post("/v1/vision/analyze", headers={"Authorization": f"Bearer {first_token}"}, json=analyze_payload(png_base64))
    second = client.post("/v1/vision/analyze", headers={"Authorization": f"Bearer {second_token}"}, json=analyze_payload(png_base64))
    assert first.status_code == 401
    assert second.status_code == 200


def test_revoked_device_token_is_rejected(client, png_base64):
    _, token, device_id = pair_device(client)
    assert client.app.state.repository.revoke_device(device_id)
    response = client.post("/v1/vision/analyze", headers={"Authorization": f"Bearer {token}"}, json=analyze_payload(png_base64))
    assert response.status_code == 401
    assert response.headers["www-authenticate"] == "Bearer"


def test_android_request_and_six_line_chinese_response_contract(client, png_base64):
    _, token, _ = pair_device(client)
    response = client.post(
        "/v1/vision/analyze",
        headers={"Authorization": f"Bearer {token}"},
        json=analyze_payload(png_base64),
    )
    assert response.status_code == 200
    assert set(response.json()) == {"analysis"}
    analysis = response.json()["analysis"]
    assert isinstance(analysis, str)
    assert analysis.splitlines() == [f"{field}：test-{field}" for field in ANALYSIS_FIELDS]


def test_legacy_or_ambiguous_nested_request_is_rejected(client, png_base64):
    _, token, _ = pair_device(client)
    response = client.post(
        "/v1/vision/analyze",
        headers={"Authorization": f"Bearer {token}"},
        json={"image": {"mime_type": "image/png", "data_base64": png_base64}, "user_text": "x", "context": []},
    )
    assert response.status_code == 400


def test_token_rate_limit_is_per_device(client, png_base64):
    _, token_one, _ = pair_device(client, "install-one")
    _, token_two, _ = pair_device(client, "install-two")
    headers_one = {"Authorization": f"Bearer {token_one}"}
    payload = analyze_payload(png_base64)
    assert client.post("/v1/vision/analyze", headers=headers_one, json=payload).status_code == 200
    assert client.post("/v1/vision/analyze", headers=headers_one, json=payload).status_code == 200
    limited = client.post("/v1/vision/analyze", headers=headers_one, json=payload)
    other = client.post("/v1/vision/analyze", headers={"Authorization": f"Bearer {token_two}"}, json=payload)
    assert limited.status_code == 429
    assert int(limited.headers["retry-after"]) >= 1
    assert other.status_code == 200


def test_request_over_transport_limit_is_rejected_before_parsing(client):
    response = client.post(
        "/v1/pair",
        content=b"x" * (MAX_REQUEST_BYTES + 1),
        headers={"Content-Type": "application/json"},
    )
    assert response.status_code == 413


def test_validation_error_is_generic_and_does_not_echo_input(client):
    secret_text = "DO-NOT-ECHO-THIS-TEXT"
    response = client.post("/v1/pair", json={"pairing_code": secret_text, "installation_id": "invalid space"})
    assert response.status_code == 400
    assert secret_text not in response.text


def test_logs_do_not_contain_pairing_token_image_or_full_user_text(client, png_base64, caplog):
    caplog.set_level("INFO", logger="cc_vision_gateway")
    code, token, _ = pair_device(client)
    user_text = "sensitive complete user prompt"
    response = client.post("/v1/vision/analyze", headers={"Authorization": f"Bearer {token}"}, json=analyze_payload(png_base64, user_text=user_text))
    assert response.status_code == 200
    logs = caplog.text
    assert code not in logs
    assert token not in logs
    assert png_base64 not in logs
    assert user_text not in logs


def test_upstream_timeout_and_failure_are_sanitized(client, png_base64):
    class FailingOllama:
        def __init__(self, timeout: bool) -> None:
            self.timeout = timeout

        async def analyze(self, image, user_text, context):
            raise UpstreamUnavailable(timeout=self.timeout)

    _, token, _ = pair_device(client)
    headers = {"Authorization": f"Bearer {token}"}
    client.app.state.ollama = FailingOllama(timeout=True)
    timed_out = client.post("/v1/vision/analyze", headers=headers, json=analyze_payload(png_base64))
    assert timed_out.status_code == 504
    assert "Ollama" not in timed_out.text
    client.app.state.ollama = FailingOllama(timeout=False)
    unavailable = client.post("/v1/vision/analyze", headers=headers, json=analyze_payload(png_base64))
    assert unavailable.status_code == 502
    assert "Ollama" not in unavailable.text


def test_rate_limit_rejects_before_expensive_image_validation(client, png_base64):
    _, token, _ = pair_device(client)
    headers = {"Authorization": f"Bearer {token}"}
    payload = analyze_payload(png_base64)
    assert client.post("/v1/vision/analyze", headers=headers, json=payload).status_code == 200
    assert client.post("/v1/vision/analyze", headers=headers, json=payload).status_code == 200

    malformed = analyze_payload("not-valid-base64")
    limited = client.post("/v1/vision/analyze", headers=headers, json=malformed)
    assert limited.status_code == 429
