import uuid
import contextvars
from typing import Optional
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware

HEADER_IN = "X-Correlation-ID"
HEADER_ALT = "X-Request-ID"

# context variable so background tasks and logging filters can access current id
_correlation_var: contextvars.ContextVar[Optional[str]] = contextvars.ContextVar("correlation_id", default=None)


class CorrelationIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        # prefer incoming header, otherwise generate a new UUID
        rid = (
            request.headers.get(HEADER_IN)
            or request.headers.get(HEADER_ALT)
            or str(uuid.uuid4())
        )
        # attach to request.state for handlers
        request.state.correlation_id = rid
        # set contextvar for logging/background tasks within this context
        token = _correlation_var.set(rid)
        try:
            response = await call_next(request)
        finally:
            # restore previous contextvar value
            _correlation_var.reset(token)

        # expose correlation id to clients
        response.headers[HEADER_IN] = rid
        return response


def get_request_id(request: Request) -> Optional[str]:
    return getattr(request.state, "correlation_id", None)


def get_current_correlation_id() -> Optional[str]:
    """Return the correlation id from the current context, if any.

    Useful in background tasks and logging filters.
    """
    return _correlation_var.get()


def set_current_correlation_id(value: Optional[str]) -> None:
    """Set the current correlation id in this context.

    Useful for background tasks and workers that receive the id as an argument.
    """
    if value is None:
        # clear to default
        _correlation_var.set(None)
    else:
        _correlation_var.set(str(value))