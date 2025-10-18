from fastapi import FastAPI, Request
from starlette.middleware.base import BaseHTTPMiddleware
import uuid

app = FastAPI()

class CorrelationIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        # prefer incoming header, otherwise generate a new UUID
        rid = request.headers.get("X-Correlation-ID") or request.headers.get("X-Request-ID") or str(uuid.uuid4())
        request.state.correlation_id = rid
        response = await call_next(request)
        response.headers["X-Correlation-ID"] = rid
        return response

app.add_middleware(CorrelationIdMiddleware)

@app.get("/ping")
async def ping(request: Request):
    rid = getattr(request.state, "correlation_id", None)
    return {"message": "pong", "correlation_id": rid}