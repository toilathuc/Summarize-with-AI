# Hướng dẫn implement Middleware & Design Patterns

Tài liệu này giải thích cấu trúc files và quy trình để triển khai các middleware patterns và cross-cutting concerns trong dự án FastAPI.

---

## Phần 1: Cấu trúc để cài đặt Correlation ID (đã hoàn thành)

### Files cần thiết

```
src/
├── middlewares/
│   ├── __init__.py                    # empty hoặc export middleware
│   └── correlation.py                 # Correlation ID middleware
├── api/
│   └── app.py                         # FastAPI app, đăng ký middleware
└── tasks/
    └── celery_tasks.py                # (optional) Celery task với correlation propagation
```

### 1.1. Middleware file (`src/middlewares/correlation.py`)

**Mục đích:** Intercept mọi HTTP request, sinh/propagate correlation ID, gắn vào context và response.

**Thành phần chính:**

- `CorrelationIdMiddleware` (BaseHTTPMiddleware): xử lý request/response
- `_correlation_var` (ContextVar): lưu ID trong context để threads/tasks truy xuất
- Helper functions:
  - `get_current_correlation_id()` → đọc ID từ context
  - `set_current_correlation_id(value)` → set ID (cho background tasks/workers)

**Luồng:**

```
Request arrives
  → Middleware reads header "X-Correlation-ID" (or generate UUID)
  → Set request.state.correlation_id = id
  → Set contextvar _correlation_var = id
  → Call next middleware/handler
  → Attach "X-Correlation-ID" header to response
  → Return response
```

### 1.2. App integration (`src/api/app.py`)

**Thành phần chính:**

- Import middleware: `from src.middlewares.correlation import CorrelationIdMiddleware, get_current_correlation_id`
- Đăng ký middleware: `app.add_middleware(CorrelationIdMiddleware)` (đặt **sớm** để downstream middleware có ID)
- Logging filter: class `_CorrelationFilter` để inject `correlation_id` vào LogRecord
- Attach filter vào logger: `logging.getLogger().addFilter(_CorrelationFilter())`
- Propagate vào background tasks: khi gọi `background_tasks.add_task(func, ...)`, truyền `get_current_correlation_id()` làm argument
- Propagate vào subprocess: set env var `X_CORRELATION_ID` khi gọi `subprocess.run(..., env={...})`

**Log format:**

```python
LOG_FORMAT = "%(asctime)s %(levelname)s [%(correlation_id)s] %(name)s: %(message)s"
logging.basicConfig(level=logging.INFO, format=LOG_FORMAT)
```

### 1.3. Background tasks / Workers (optional)

**File:** `src/tasks/celery_tasks.py` hoặc tương tự

**Luồng:**

- API enqueue job với `correlation_id` làm param
- Worker task restore ID: `set_current_correlation_id(correlation_id)`
- Nếu gọi subprocess, set env var `X_CORRELATION_ID`
- Logs trong worker tự động include ID (do contextvar và filter)

### 1.4. Subprocess / Script (`update_news.py`)

**Thành phần:**

- Đọc env var: `os.environ.get("X_CORRELATION_ID")`
- Dùng LoggerAdapter hoặc set contextvar để log có ID

**Ví dụ:**

```python
import os
import logging

CORRELATION_ID = os.environ.get("X_CORRELATION_ID")
logger = logging.getLogger("update_news")
if CORRELATION_ID:
    logger = logging.LoggerAdapter(logger, {"correlation_id": CORRELATION_ID})

logger.info("Pipeline started")  # log sẽ có correlation_id
```

---

## Phần 2: Template để thêm Design Pattern khác

Khi muốn thêm một pattern mới (ví dụ: **Rate Limiting**, **Security Headers**, **Exception Handler**), làm theo các bước:

### Bước 1: Tạo file middleware (nếu là middleware)

**File:** `src/middlewares/<pattern_name>.py`

**Template:**

```python
from starlette.middleware.base import BaseHTTPMiddleware
from fastapi import Request, Response

class MyPatternMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, config=None):
        super().__init__(app)
        self.config = config or {}

    async def dispatch(self, request: Request, call_next):
        # Logic BEFORE request handler
        # e.g., validate, check limits, add headers

        # Call next middleware/handler
        response = await call_next(request)

        # Logic AFTER handler (modify response)
        # e.g., add headers, log metrics

        return response
```

**Ví dụ cụ thể: Security Headers Middleware**

```python
# src/middlewares/security_headers.py
from starlette.middleware.base import BaseHTTPMiddleware
from fastapi import Request

class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)

        # Add security headers
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        response.headers["Content-Security-Policy"] = "default-src 'self'"

        return response
```

### Bước 2: Đăng ký middleware trong app

**File:** `src/api/app.py`

**Thêm import:**

```python
from src.middlewares.security_headers import SecurityHeadersMiddleware
```

**Đăng ký middleware:**

```python
app = FastAPI(...)

# Thứ tự middleware quan trọng: từ ngoài vào trong
# 1. Correlation ID (đầu tiên để propagate qua hết)
app.add_middleware(CorrelationIdMiddleware)

# 2. Security headers (thêm headers vào response)
app.add_middleware(SecurityHeadersMiddleware)

# 3. CORS (cho phép cross-origin)
app.add_middleware(CORSMiddleware, allow_origins=["*"], ...)

# 4. Rate limiting, auth, etc.
```

### Bước 3: Config (nếu cần)

Nếu pattern cần config động (ví dụ: rate limits, allowed origins), đưa config vào `src/config/settings.py`:

```python
# src/config/settings.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    GEMINI_API_KEY: str | None = None
    REDIS_URL: str | None = None

    # Security headers config
    SECURITY_HEADERS_ENABLED: bool = True
    CSP_POLICY: str = "default-src 'self'"

    # Rate limiting config
    RATE_LIMIT_ENABLED: bool = True
    RATE_LIMIT_PER_MINUTE: int = 60

    class Config:
        env_file = ".env"

settings = Settings()
```

Rồi inject vào middleware:

```python
from src.config.settings import settings

if settings.SECURITY_HEADERS_ENABLED:
    app.add_middleware(SecurityHeadersMiddleware, csp_policy=settings.CSP_POLICY)
```

### Bước 4: Test (unit + integration)

**File:** `tests/test_<pattern>.py`

**Template unit test:**

```python
from fastapi import FastAPI
from fastapi.testclient import TestClient
from src.middlewares.security_headers import SecurityHeadersMiddleware

app = FastAPI()
app.add_middleware(SecurityHeadersMiddleware)

@app.get("/ping")
def ping():
    return {"ok": True}

client = TestClient(app)

def test_security_headers_present():
    resp = client.get("/ping")
    assert resp.status_code == 200
    assert "X-Content-Type-Options" in resp.headers
    assert resp.headers["X-Content-Type-Options"] == "nosniff"
    assert "X-Frame-Options" in resp.headers
```

---

## Phần 3: Checklist cho mỗi Pattern mới

### Khi thêm một pattern (ví dụ: Rate Limiting)

- [ ] **1. Design:** Xác định mục đích (protect endpoints, bảo vệ tài nguyên, chuẩn hoá output...)
- [ ] **2. File structure:**
  - Middleware: `src/middlewares/<name>.py`
  - Config (nếu cần): thêm vào `src/config/settings.py`
  - Helper/utils (nếu phức tạp): `src/utils/<name>_helper.py`
- [ ] **3. Implementation:**
  - Viết class middleware (BaseHTTPMiddleware hoặc Depends cho endpoint-level)
  - Expose helper functions nếu cần (như `get_current_correlation_id()`)
- [ ] **4. Integration:**
  - Import vào `src/api/app.py`
  - Đăng ký middleware hoặc dependency
  - Thứ tự middleware: correlation first → security → CORS → rate-limit → auth
- [ ] **5. Logging/observability:**
  - Log actions (ví dụ: rate limit exceeded, auth failed)
  - Include `correlation_id` trong logs
- [ ] **6. Tests:**
  - Unit test: mock app + middleware in isolation
  - Integration test: TestClient gọi endpoint và assert behavior
- [ ] **7. Docs:**
  - Update `IMPLEMENTATION_CHECKLIST.md` DoD
  - Ghi chú trong `README.md` hoặc API docs

---

## Phần 4: Các Pattern phổ biến & cấu trúc files

### 4.1. Rate Limiting

**Files:**

```
src/middlewares/rate_limit.py       # Middleware hoặc dependency
src/utils/rate_limiter.py           # Redis-based limiter hoặc in-memory
```

**Libs:** `slowapi`, `limits`, `redis`

**Đăng ký:**

```python
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_exception_handler(429, _rate_limit_exceeded_handler)

@app.get("/api/refresh")
@limiter.limit("5/minute")
def refresh():
    ...
```

### 4.2. Global Exception Handler

**Files:**

```
src/middlewares/exception_handler.py   # hoặc trực tiếp trong app.py
```

**Đăng ký:**

```python
from fastapi import Request, HTTPException
from fastapi.responses import JSONResponse
from src.middlewares.correlation import get_current_correlation_id

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    correlation_id = get_current_correlation_id() or "-"
    return JSONResponse(
        status_code=500,
        content={
            "code": "INTERNAL_ERROR",
            "message": str(exc),
            "correlation_id": correlation_id,
        },
    )

@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    correlation_id = get_current_correlation_id() or "-"
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "code": exc.detail,
            "message": exc.detail,
            "correlation_id": correlation_id,
        },
    )
```

### 4.3. Request Guard (size/content-type validation)

**Files:**

```
src/middlewares/request_guard.py
```

**Logic:**

- Check `Content-Length` header
- Validate `Content-Type` cho POST/PUT
- Return 413 (Payload Too Large) hoặc 415 (Unsupported Media Type)

**Đăng ký:**

```python
from src.middlewares.request_guard import RequestGuardMiddleware
app.add_middleware(RequestGuardMiddleware, max_body_size=10*1024*1024)  # 10MB
```

### 4.4. Audit Logging

**Files:**

```
src/middlewares/audit_log.py
```

**Logic:**

- Log metadata: user, correlation_id, route, method, status, latency
- Gửi vào log aggregator hoặc DB (audit table)

**Đăng ký:**

```python
from src.middlewares.audit_log import AuditLogMiddleware
app.add_middleware(AuditLogMiddleware, sensitive_routes=["/api/refresh"])
```

### 4.5. Metrics & Observability

**Files:**

```
src/middlewares/metrics.py         # Prometheus exporter
```

**Libs:** `prometheus-client`, `starlette-exporter`

**Đăng ký:**

```python
from starlette_exporter import PrometheusMiddleware, handle_metrics

app.add_middleware(PrometheusMiddleware, app_name="summarizer")
app.add_route("/metrics", handle_metrics)
```

### 4.6. Authentication & Authorization

**Files:**

```
src/middlewares/auth.py             # hoặc dependency
src/utils/jwt_helper.py             # JWT decode/validate
```

**Đăng ký (FastAPI Depends):**

```python
from fastapi import Depends, HTTPException, Header

async def verify_token(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid token")
    token = authorization.split(" ")[1]
    # decode and validate JWT
    return user_from_token(token)

@app.get("/api/refresh")
def refresh(user = Depends(verify_token)):
    # chỉ user hợp lệ mới gọi được
    ...
```

---

## Phần 5: Thứ tự middleware (order matters)

Middleware chạy theo thứ tự **từ ngoài vào trong** khi xử lý request, và **từ trong ra ngoài** khi xử lý response.

**Thứ tự khuyến nghị:**

1. **CorrelationIdMiddleware** — generate/propagate ID ngay (để downstream middleware log có ID)
2. **SecurityHeadersMiddleware** — thêm headers bảo mật
3. **CORSMiddleware** — cho phép cross-origin
4. **RequestGuardMiddleware** — validate size/content-type
5. **RateLimitMiddleware** — kiểm tra quota
6. **AuthenticationMiddleware** — xác thực user
7. **AuditLogMiddleware** — log metadata
8. **MetricsMiddleware** — thu thập metrics
9. **GZipMiddleware** — compress response

**Ví dụ:**

```python
app.add_middleware(CorrelationIdMiddleware)
app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(CORSMiddleware, ...)
app.add_middleware(RequestGuardMiddleware, max_body_size=10*1024*1024)
app.add_middleware(RateLimitMiddleware)
app.add_middleware(AuditLogMiddleware)
app.add_middleware(PrometheusMiddleware)
app.add_middleware(GZipMiddleware, minimum_size=1000)
```

---

## Phần 6: Testing Strategy

### Unit tests

- Test middleware isolation (mock `call_next`)
- Assert headers/state được set đúng

### Integration tests

- Dùng `TestClient` để gọi endpoint
- Assert response headers, status code, và side effects (logs, metrics)

### E2E tests (optional)

- Start server, gọi API thực
- Verify logs/metrics/DB entries

---

## Phần 7: Tóm tắt — Checklist nhanh khi thêm pattern mới

| Bước | Hành động                                         | File liên quan                             |
| ---- | ------------------------------------------------- | ------------------------------------------ |
| 1    | Tạo middleware file                               | `src/middlewares/<name>.py`                |
| 2    | Implement logic (BaseHTTPMiddleware hoặc Depends) | Class/function trong file middleware       |
| 3    | Thêm config (nếu cần)                             | `src/config/settings.py`                   |
| 4    | Import và đăng ký trong app                       | `src/api/app.py`                           |
| 5    | Viết unit tests                                   | `tests/test_<name>.py`                     |
| 6    | Viết integration tests                            | `tests/test_api_integration.py`            |
| 7    | Update docs                                       | `IMPLEMENTATION_CHECKLIST.md`, `README.md` |
| 8    | Verify locally                                    | Chạy server, test thủ công + pytest        |

---

## Ví dụ hoàn chỉnh: Thêm Security Headers Middleware

### 1. Tạo file middleware

```python
# src/middlewares/security_headers.py
from starlette.middleware.base import BaseHTTPMiddleware
from fastapi import Request

class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Strict-Transport-Security"] = "max-age=31536000"
        return response
```

### 2. Đăng ký trong app

```python
# src/api/app.py
from src.middlewares.security_headers import SecurityHeadersMiddleware

app.add_middleware(SecurityHeadersMiddleware)
```

### 3. Test

```python
# tests/test_security_headers.py
from fastapi.testclient import TestClient
from src.api.app import app

client = TestClient(app)

def test_security_headers():
    resp = client.get("/healthz")
    assert "X-Content-Type-Options" in resp.headers
    assert resp.headers["X-Frame-Options"] == "DENY"
```

### 4. Chạy

```powershell
uvicorn src.api.app:app --reload --port 8000
curl -i http://127.0.0.1:8000/healthz
# Xem headers trong response
```

---

## Kết luận

Để implement một pattern/middleware mới:

1. Tạo file trong `src/middlewares/<name>.py`
2. Viết class middleware (BaseHTTPMiddleware) hoặc dependency (Depends)
3. Import và đăng ký trong `app.py` (chú ý thứ tự)
4. Thêm config vào `settings.py` nếu cần
5. Viết tests
6. Chạy thử và verify

Correlation ID đã hoàn thành theo pattern này — các pattern khác (rate limit, auth, security headers, etc.) tuân theo cùng quy trình.
