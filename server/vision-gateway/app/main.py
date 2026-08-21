"""FastAPI entry point for the controlled local vision gateway."""

from __future__ import annotations

import asyncio
import logging
import uuid
from contextlib import asynccontextmanager
from typing import AsyncIterator

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .config import MAX_REQUEST_BYTES, Settings
from .controls import DeviceRateLimiter, InferenceGate
from .ollama import OllamaClient, UpstreamUnavailable
from .repository import Device, GatewayRepository
from .security import generate_device_token
from .validation import (
    InputRejected,
    parse_analyze_request,
    parse_pair_request,
    validate_image,
)

logger = logging.getLogger("cc_vision_gateway")


class ApiError(Exception):
    def __init__(
        self,
        status_code: int,
        detail: str,
        *,
        headers: dict[str, str] | None = None,
        category: str = "client_error",
    ) -> None:
        self.status_code = status_code
        self.detail = detail
        self.headers = headers or {}
        self.category = category
        super().__init__(detail)


def create_app(settings: Settings | None = None) -> FastAPI:
    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        resolved_settings = settings or Settings.from_env()
        repository = GatewayRepository(
            resolved_settings.database_path, resolved_settings.pepper
        )
        repository.initialize()
        app.state.settings = resolved_settings
        app.state.repository = repository
        app.state.rate_limiter = DeviceRateLimiter(
            resolved_settings.rate_limit_requests,
            resolved_settings.rate_limit_window_seconds,
        )
        app.state.inference_gate = InferenceGate()
        app.state.ollama = OllamaClient()
        yield

    app = FastAPI(
        title="CC Vision Gateway",
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
        lifespan=lifespan,
    )

    @app.middleware("http")
    async def request_metadata(request: Request, call_next):
        request_id = uuid.uuid4().hex
        request.state.request_id = request_id
        response = await call_next(request)
        response.headers["X-Request-ID"] = request_id
        safe_path = request.url.path if request.url.path in {"/health", "/v1/pair", "/v1/vision/analyze"} else "/unmatched"
        logger.info(
            "request_id=%s method=%s path=%s status=%s",
            request_id,
            request.method,
            safe_path,
            response.status_code,
        )
        return response

    @app.exception_handler(ApiError)
    async def api_error_handler(request: Request, exc: ApiError) -> JSONResponse:
        if exc.status_code >= 500:
            logger.warning(
                "request_id=%s category=%s status=%s",
                getattr(request.state, "request_id", "unknown"),
                exc.category,
                exc.status_code,
            )
        return JSONResponse(
            status_code=exc.status_code,
            content={"detail": exc.detail},
            headers=exc.headers,
        )

    @app.exception_handler(RequestValidationError)
    async def request_validation_error_handler(
        request: Request, exc: RequestValidationError
    ) -> JSONResponse:
        return JSONResponse(status_code=400, content={"detail": "Invalid request body"})

    @app.get("/health")
    async def health() -> dict[str, str]:
        return {"status": "ok"}

    @app.post("/v1/pair")
    async def pair(request: Request) -> JSONResponse:
        _reject_oversized_content_length(request)
        content_type = request.headers.get("content-type", "").split(";", 1)[0].strip().lower()
        if content_type != "application/json":
            raise ApiError(415, "Unsupported media type")
        body = await _read_limited_body(request)
        try:
            pair_request = parse_pair_request(body)
        except InputRejected as exc:
            raise ApiError(exc.status_code, exc.detail) from None

        token = generate_device_token()
        repository: GatewayRepository = request.app.state.repository
        device = repository.consume_pairing_code_and_create_device(
            pair_request.pairing_code,
            pair_request.installation_id,
            token,
        )
        if device is None:
            raise ApiError(401, "Invalid or expired pairing code")
        return JSONResponse(
            status_code=201,
            content={
                "device_token": token,
                "token_type": "Bearer",
                "device_id": device.id,
            },
            headers={"Cache-Control": "no-store"},
        )

    @app.post("/v1/vision/analyze")
    async def analyze(request: Request) -> JSONResponse:
        _reject_oversized_content_length(request)
        content_type = request.headers.get("content-type", "").split(";", 1)[0].strip().lower()
        if content_type != "application/json":
            raise ApiError(415, "Unsupported media type")
        device = _authenticate(request)
        limiter: DeviceRateLimiter = request.app.state.rate_limiter
        allowed, retry_after = await limiter.allow(device.id)
        if not allowed:
            raise ApiError(
                429,
                "Rate limit exceeded",
                headers={"Retry-After": str(retry_after)},
                category="rate_limit",
            )

        body = await _read_limited_body(request)
        try:
            analysis_request = parse_analyze_request(body)
            image = validate_image(analysis_request.image_payload())
        except InputRejected as exc:
            raise ApiError(exc.status_code, exc.detail) from None

        gate: InferenceGate = request.app.state.inference_gate
        admitted = await gate.acquire()
        if not admitted:
            raise ApiError(
                429,
                "Inference queue is full",
                headers={"Retry-After": "1"},
                category="queue_full",
            )
        try:
            ollama: OllamaClient = request.app.state.ollama
            result = await ollama.analyze(
                image,
                analysis_request.user_text,
                list(analysis_request.conversation_context),
            )
        except UpstreamUnavailable as exc:
            if exc.timeout:
                raise ApiError(
                    504, "Vision analysis timed out", category="ollama_timeout"
                ) from None
            raise ApiError(
                502, "Vision analysis unavailable", category="ollama_failure"
            ) from None
        finally:
            await asyncio.shield(gate.release())

        return JSONResponse(
            content={"analysis": result}, headers={"Cache-Control": "no-store"}
        )

    return app


def _reject_oversized_content_length(request: Request) -> None:
    value = request.headers.get("content-length")
    if value is None:
        return
    try:
        length = int(value, 10)
    except ValueError:
        raise ApiError(400, "Invalid Content-Length") from None
    if length < 0:
        raise ApiError(400, "Invalid Content-Length")
    if length > MAX_REQUEST_BYTES:
        raise ApiError(413, "Request exceeds 6 MiB limit")


async def _read_limited_body(request: Request) -> bytes:
    chunks: list[bytes] = []
    size = 0
    async for chunk in request.stream():
        size += len(chunk)
        if size > MAX_REQUEST_BYTES:
            raise ApiError(413, "Request exceeds 6 MiB limit")
        chunks.append(chunk)
    return b"".join(chunks)


def _authenticate(request: Request) -> Device:
    value = request.headers.get("authorization", "")
    if not value.startswith("Bearer "):
        raise ApiError(
            401,
            "Invalid device token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    token = value[7:]
    if not 20 <= len(token) <= 256 or any(character.isspace() for character in token):
        raise ApiError(
            401,
            "Invalid device token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    repository: GatewayRepository = request.app.state.repository
    device = repository.active_device_for_token(token)
    if device is None:
        raise ApiError(
            401,
            "Invalid device token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return device


app = create_app()
