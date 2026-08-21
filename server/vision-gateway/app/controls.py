"""In-process admission control for the single local model instance."""

from __future__ import annotations

import asyncio
import time
from collections import defaultdict, deque


class DeviceRateLimiter:
    """Fixed-window-in-practice sliding timestamp limiter keyed by device UUID."""

    def __init__(self, maximum: int, window_seconds: float) -> None:
        self.maximum = maximum
        self.window_seconds = window_seconds
        self._events: dict[str, deque[float]] = defaultdict(deque)
        self._lock = asyncio.Lock()

    async def allow(self, device_id: str) -> tuple[bool, int]:
        now = time.monotonic()
        async with self._lock:
            events = self._events[device_id]
            cutoff = now - self.window_seconds
            while events and events[0] <= cutoff:
                events.popleft()
            if len(events) >= self.maximum:
                retry_after = max(1, int(events[0] + self.window_seconds - now) + 1)
                return False, retry_after
            events.append(now)
            # Remove empty expired queues for other devices opportunistically.
            if len(self._events) > 1024:
                for key in list(self._events):
                    queue = self._events[key]
                    while queue and queue[0] <= cutoff:
                        queue.popleft()
                    if not queue:
                        del self._events[key]
            return True, 0


class InferenceGate:
    """Permit one active inference and exactly one waiting inference globally."""

    def __init__(self) -> None:
        self._semaphore = asyncio.Semaphore(1)
        self._admitted = 0
        self._lock = asyncio.Lock()

    async def acquire(self) -> bool:
        async with self._lock:
            if self._admitted >= 2:
                return False
            self._admitted += 1
        try:
            await self._semaphore.acquire()
        except BaseException:
            async with self._lock:
                self._admitted -= 1
            raise
        return True

    async def release(self) -> None:
        async with self._lock:
            self._admitted -= 1
            self._semaphore.release()
