# Correlation ID - Hướng dẫn chi tiết từ A-Z

## 📚 Mục lục

1. [Correlation ID là gì?](#1-correlation-id-là-gì)
2. [Tại sao cần Correlation ID?](#2-tại-sao-cần-correlation-id)
3. [Correlation ID hoạt động như thế nào?](#3-correlation-id-hoạt-động-như-thế-nào)
4. [Giải quyết vấn đề gì?](#4-giải-quyết-vấn-đề-gì)
5. [Kiến trúc và luồng dữ liệu](#5-kiến-trúc-và-luồng-dữ-liệu)
6. [Cài đặt vào chương trình](#6-cài-đặt-vào-chương-trình)
7. [Cách chạy và kiểm tra](#7-cách-chạy-và-kiểm-tra)
8. [Troubleshooting](#8-troubleshooting)
9. [Best Practices](#9-best-practices)

---

> **Note (Oct 2025):** Examples mentioning /api/refresh describe the legacy HTTP trigger. That endpoint is disabled by default; run python update_news.py (or add your own secured admin API) to refresh data in the current build.

## 1. Correlation ID là gì?

### Định nghĩa đơn giản

**Correlation ID** (còn gọi là **Request ID**, **Trace ID**) là một **mã định danh duy nhất** (unique identifier) được gán cho mỗi request/giao dịch trong hệ thống.

### Ví dụ thực tế

Giống như **mã vận đơn** khi bạn gửi hàng:

- Bạn gửi 1 kiện hàng → nhận mã vận đơn `VD123456`
- Kiện hàng đi qua nhiều kho (xử lý, vận chuyển, giao hàng)
- Mỗi bước đều ghi nhật ký với mã `VD123456`
- Khi có vấn đề, bạn dùng `VD123456` để tra cứu toàn bộ hành trình

**Trong hệ thống:**

- Client gửi HTTP request → hệ thống tạo correlation ID `req-abc-123`
- Request đi qua nhiều tầng (API → service → database → queue → worker)
- Mỗi log đều ghi `[req-abc-123]`
- Khi có lỗi, dev dùng `req-abc-123` để xem toàn bộ logs liên quan

### Format thông dụng

```
UUID v4: 550e8400-e29b-41d4-a716-446655440000
Custom:  req-2024-10-18-abc123
Short:   7f3a9b2c
```

---

## 2. Tại sao cần Correlation ID?

### Vấn đề khi KHÔNG có Correlation ID

#### Ví dụ: User báo lỗi "Trang không load được lúc 10:30 sáng"

**Logs không có Correlation ID:**

```
10:30:01 INFO API received request to /api/refresh
10:30:01 INFO Fetching news from Techmeme
10:30:02 ERROR Database connection failed
10:30:02 INFO API received request to /api/summary
10:30:03 INFO Successfully summarized 10 articles
10:30:03 ERROR Timeout calling Gemini API
10:30:04 INFO API received request to /api/refresh
```

**Câu hỏi:**

- Lỗi `Database connection failed` thuộc request nào?
- Lỗi `Timeout calling Gemini API` có liên quan đến user báo lỗi không?
- Request lỗi đi qua những service nào?

❌ **Không thể trả lời** → phải đọc hàng ngàn dòng log, đoán mò!

---

#### Cùng logs NHƯNG CÓ Correlation ID:

```
10:30:01 INFO [req-aaa] API received request to /api/refresh
10:30:01 INFO [req-aaa] Fetching news from Techmeme
10:30:02 ERROR [req-bbb] Database connection failed
10:30:02 INFO [req-bbb] API received request to /api/summary
10:30:03 INFO [req-bbb] Successfully summarized 10 articles
10:30:03 ERROR [req-ccc] Timeout calling Gemini API
10:30:04 INFO [req-ddd] API received request to /api/refresh
```

**Câu hỏi:**

- Lỗi `Database connection failed` thuộc request nào? → `req-bbb`
- User báo lỗi lúc 10:30, request ID nào? → Grep `[req-ccc]` → tìm ra lỗi Timeout

✅ **Dễ dàng debug** → filter logs theo `[req-ccc]`, thấy toàn bộ luồng xử lý!

---

### Lợi ích cụ thể

| Vấn đề                   | Không có Correlation ID            | Có Correlation ID                           |
| ------------------------ | ---------------------------------- | ------------------------------------------- |
| **Debug lỗi**            | Đọc 10,000 dòng log, đoán mò       | Grep theo ID → 10 dòng log liên quan        |
| **Trace request**        | Không biết request đi đâu          | Thấy rõ: API → Worker → AI → Database       |
| **Performance analysis** | Không biết step nào chậm           | So sánh timestamp theo ID → biết bottleneck |
| **Distributed tracing**  | Không track qua services           | ID propagate qua nhiều service/microservice |
| **Customer support**     | "Lỗi lúc 10:30" → tìm mãi không ra | User gửi ID → tìm ngay                      |

---

## 3. Correlation ID hoạt động như thế nào?

### Luồng hoạt động cơ bản

```
┌──────────┐
│  Client  │
└────┬─────┘
     │ 1. HTTP Request (header: X-Correlation-ID: abc hoặc không có)
     ▼
┌─────────────────────────────────────────────┐
│         FastAPI Server (Middleware)         │
│  2. Check header:                           │
│     - Có X-Correlation-ID → dùng luôn       │
│     - Không có → tạo mới (UUID)             │
│  3. Lưu vào request.state.correlation_id    │
│  4. Lưu vào ContextVar (thread-safe)        │
└────┬────────────────────────────────────────┘
     │ 5. Request xử lý qua các tầng
     ▼
┌──────────────────────────────────────────┐
│  Logging System                          │
│  6. Mỗi log.info/error tự động gắn ID    │
│     → "INFO [abc] Processing request"    │
└──────────────────────────────────────────┘
     │
     ▼
┌──────────────────────────────────────────┐
│  Background Task / Subprocess            │
│  7. Truyền ID qua:                       │
│     - Function param                     │
│     - Environment variable               │
│     - Message queue metadata             │
└──────────────────────────────────────────┘
     │
     ▼
┌──────────────────────────────────────────┐
│  Response                                │
│  8. Gắn header: X-Correlation-ID: abc    │
│  9. Client nhận được (có thể dùng debug) │
└──────────────────────────────────────────┘
```

### Chi tiết từng bước

#### **Bước 1-2: Request arrives → Middleware checks**

```python
# Client gửi request (có thể có hoặc không có header)
curl -H "X-Correlation-ID: my-test-123" http://localhost:8000/api/refresh

# Middleware xử lý:
async def dispatch(request, call_next):
    correlation_id = request.headers.get("X-Correlation-ID")
    if not correlation_id:
        correlation_id = str(uuid.uuid4())  # tạo mới nếu không có
```

#### **Bước 3-4: Lưu vào State và ContextVar**

```python
# Lưu vào request state (truy cập trong handler)
request.state.correlation_id = correlation_id

# Lưu vào ContextVar (thread-safe, truy cập mọi nơi)
_correlation_var.set(correlation_id)
```

**Tại sao cần ContextVar?**

- `request.state` chỉ truy cập trong request handler
- `ContextVar` truy cập được ở **mọi nơi** (logs, background tasks, nested functions)

#### **Bước 5-6: Logging tự động gắn ID**

```python
# Setup logging filter
class CorrelationFilter(logging.Filter):
    def filter(self, record):
        record.correlation_id = get_current_correlation_id() or "-"
        return True

# Format log
LOG_FORMAT = "%(asctime)s [%(correlation_id)s] %(levelname)s: %(message)s"

# Kết quả:
# 2024-10-18 10:30:01 [my-test-123] INFO: Processing request
# 2024-10-18 10:30:02 [my-test-123] ERROR: Failed to fetch news
```

#### **Bước 7: Propagate vào Background Tasks**

```python
# Trong endpoint
correlation_id = get_current_correlation_id()
background_tasks.add_task(process_job, correlation_id=correlation_id)

# Trong background task
def process_job(correlation_id: str):
    set_current_correlation_id(correlation_id)  # restore context
    logger.info("Job started")  # log sẽ có [my-test-123]
```

#### **Bước 8-9: Response header**

```python
response.headers["X-Correlation-ID"] = correlation_id
# Client nhận được header, có thể dùng để báo lỗi hoặc debug
```

---

## 4. Giải quyết vấn đề gì?

### Vấn đề 1: Debug distributed systems

**Tình huống:** Hệ thống có nhiều services (API → Worker → AI → Database)

**Không có Correlation ID:**

- Logs của mỗi service riêng biệt
- Không biết request từ API nào trigger worker nào
- Debug = đoán mò

**Có Correlation ID:**

```
# API Service logs
10:30:01 [req-abc] INFO: Received /api/refresh request
10:30:01 [req-abc] INFO: Enqueued job to worker

# Worker Service logs
10:30:02 [req-abc] INFO: Worker picked up job
10:30:02 [req-abc] INFO: Calling Gemini API

# AI Service logs
10:30:03 [req-abc] ERROR: Gemini timeout after 30s
```

→ Thấy rõ: Request `req-abc` đi từ API → Worker → AI → Timeout

---

### Vấn đề 2: Performance bottleneck

**Tình huống:** Một số request chậm, cần tìm bước nào chậm

**Phân tích:**

```
# Grep logs theo correlation ID
[req-slow-1] 10:30:00.100 API: Request received
[req-slow-1] 10:30:00.150 API: Fetching feed (50ms)
[req-slow-1] 10:30:15.200 Worker: AI summarization (15s ← BOTTLENECK!)
[req-slow-1] 10:30:15.300 Worker: Saved to database (100ms)
```

→ Phát hiện: AI summarization mất 15s → cần cache hoặc optimize prompt

---

### Vấn đề 3: Customer support

**Tình huống:** User báo lỗi "Không load được tin tức"

**Không có Correlation ID:**

- Support: "Bạn bị lỗi lúc nào?" → "Khoảng 10:30 sáng"
- Dev: Tìm logs từ 10:25-10:35 → 5000 dòng log
- Dev: Không biết dòng nào là của user → hỏi thêm "IP? Browser?"

**Có Correlation ID:**

- Support: "Bạn copy đoạn này gửi cho tôi: `req-abc-123` (hiển thị trên trang lỗi)"
- Dev: `grep "req-abc-123" logs/*.log` → 10 dòng log
- Dev: Thấy ngay `[req-abc-123] ERROR: Gemini API key invalid`

---

### Vấn đề 4: Concurrent requests

**Tình huống:** Server xử lý 100 requests/giây, logs xen kẽ nhau

**Logs không có ID:**

```
INFO: Started processing
INFO: Fetching news
INFO: Started processing  ← request khác
ERROR: Failed to fetch    ← lỗi của request nào?
INFO: Fetching news       ← tiếp tục request nào?
```

**Logs có ID:**

```
INFO [req-1]: Started processing
INFO [req-1]: Fetching news
INFO [req-2]: Started processing
ERROR [req-1]: Failed to fetch  ← rõ ràng là req-1
INFO [req-2]: Fetching news     ← rõ ràng là req-2
```

---

## 5. Kiến trúc và luồng dữ liệu

### Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                  │
│  - Browser / Mobile App / API consumer                          │
│  - Gửi request (có thể gửi kèm X-Correlation-ID hoặc không)     │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ HTTP Request
                         │ Header: X-Correlation-ID: abc (optional)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    FASTAPI SERVER                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  1. CorrelationIdMiddleware                               │  │
│  │     - Extract or generate correlation_id                  │  │
│  │     - Set request.state.correlation_id                    │  │
│  │     - Set ContextVar _correlation_var                     │  │
│  └────────────────────────┬──────────────────────────────────┘  │
│                           │                                     │
│  ┌────────────────────────▼──────────────────────────────────┐  │
│  │  2. Logging System                                        │  │
│  │     - CorrelationFilter injects ID into log records       │  │
│  │     - Format: [correlation_id] message                    │  │
│  └────────────────────────┬──────────────────────────────────┘  │
│                           │                                     │
│  ┌────────────────────────▼──────────────────────────────────┐  │
│  │  3. Request Handler / Endpoint                            │  │
│  │     - Business logic                                      │  │
│  │     - Access ID: get_current_correlation_id()             │  │
│  └────────────────────────┬──────────────────────────────────┘  │
│                           │                                     │
│  ┌────────────────────────▼──────────────────────────────────┐  │
│  │  4. Response                                              │  │
│  │     - Middleware adds header: X-Correlation-ID            │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Background   │  │  Subprocess  │  │   Worker     │
│   Task       │  │              │  │   (Celery)   │
├──────────────┤  ├──────────────┤  ├──────────────┤
│ - Receive ID │  │ - Receive ID │  │ - Receive ID │
│   as param   │  │   via env    │  │   via queue  │
│              │  │   variable   │  │   metadata   │
│ - Restore    │  │              │  │              │
│   ContextVar │  │ - Set logger │  │ - Restore    │
│              │  │   adapter    │  │   ContextVar │
│ - Logs with  │  │              │  │              │
│   [ID]       │  │ - Logs with  │  │ - Logs with  │
│              │  │   [ID]       │  │   [ID]       │
└──────────────┘  └──────────────┘  └──────────────┘
```

---

### Luồng dữ liệu chi tiết

#### Scenario 1: Request đơn giản (API only)

```
Client                    Middleware               Handler                  Logger
  │                           │                        │                        │
  ├─ GET /api/refresh ──────►│                        │                        │
  │  (no header)              │                        │                        │
  │                           ├─ Generate UUID ────────┤                        │
  │                           │   "req-xyz-789"        │                        │
  │                           │                        │                        │
  │                           ├─ Set request.state ────┤                        │
  │                           ├─ Set ContextVar ───────┤                        │
  │                           │                        │                        │
  │                           ├─ Call handler ────────►│                        │
  │                           │                        ├─ logger.info() ──────►│
  │                           │                        │                        ├─ Filter adds ID
  │                           │                        │                        ├─ "INFO [req-xyz-789] Processing"
  │                           │                        │◄───────────────────────┤
  │                           │                        │                        │
  │                           │◄─ Return response ─────┤                        │
  │                           ├─ Add header ───────────┤                        │
  │◄─ 200 OK ────────────────┤   X-Correlation-ID     │                        │
  │   X-Correlation-ID:       │                        │                        │
  │   req-xyz-789             │                        │                        │
```

---

#### Scenario 2: Request with Background Task

```
Client          Middleware          Handler             Background Task          Logger
  │                 │                   │                        │                  │
  ├─ POST /refresh ┤                   │                        │                  │
  │                 ├─ Generate ID ────┤ "req-abc-123"          │                  │
  │                 ├─ Set ContextVar ─┤                        │                  │
  │                 │                   │                        │                  │
  │                 ├─ Call handler ───►│                        │                  │
  │                 │                   ├─ Get ID ───────────────┤                  │
  │                 │                   │   correlation_id =     │                  │
  │                 │                   │   get_current_...()    │                  │
  │                 │                   │                        │                  │
  │                 │                   ├─ Queue task ──────────►│                  │
  │                 │                   │   add_task(            │                  │
  │                 │                   │     func,              │                  │
  │                 │                   │     correlation_id=... │                  │
  │                 │                   │   )                    │                  │
  │                 │                   │                        │                  │
  │                 │◄─ Return 202 ─────┤                        │                  │
  │◄─ 202 Accepted ─┤                   │                        │                  │
  │                 │                   │                        │                  │
  │                                     │                        ├─ Task runs ─────►│
  │                                     │                        ├─ Restore ID     │
  │                                     │                        │   set_current... │
  │                                     │                        │                  │
  │                                     │                        ├─ logger.info() ─►│
  │                                     │                        │                  ├─ "[req-abc-123] Task started"
  │                                     │                        │◄─────────────────┤
```

---

#### Scenario 3: Request with Subprocess

```
API Server              Subprocess (update_news.py)           Logger
    │                              │                            │
    ├─ Request [req-def-456] ─────┤                            │
    │                              │                            │
    ├─ Capture correlation_id ────┤                            │
    │   from ContextVar            │                            │
    │                              │                            │
    ├─ subprocess.run() ──────────►│                            │
    │   env={                      │                            │
    │     "X_CORRELATION_ID":      │                            │
    │     "req-def-456"            │                            │
    │   }                          │                            │
    │                              ├─ Read env var ────────────┤
    │                              │   CORRELATION_ID =         │
    │                              │   os.environ.get(...)      │
    │                              │                            │
    │                              ├─ Setup LoggerAdapter ─────┤
    │                              │   with correlation_id      │
    │                              │                            │
    │                              ├─ logger.info() ──────────►│
    │                              │                            ├─ "[req-def-456] Pipeline started"
    │                              │◄───────────────────────────┤
    │                              │                            │
    │◄─ Subprocess completes ──────┤                            │
```

---

## 6. Cài đặt vào chương trình

### 6.1. Cấu trúc files cần tạo/sửa

```
src/
├── middlewares/
│   ├── __init__.py                    ← Step 1: Create/update
│   └── correlation.py                 ← Step 2: Create middleware
├── api/
│   └── app.py                         ← Step 3: Register middleware
└── tasks/
    └── celery_tasks.py                ← Step 4 (optional): Worker tasks
```

---

### 6.2. Step-by-step Implementation

#### **STEP 1: Tạo file `src/middlewares/__init__.py`**

```python
# src/middlewares/__init__.py

# Export middleware và helper functions để dễ import
from .correlation import (
    CorrelationIdMiddleware,
    get_current_correlation_id,
    set_current_correlation_id,
    get_request_id,
)

__all__ = [
    "CorrelationIdMiddleware",
    "get_current_correlation_id",
    "set_current_correlation_id",
    "get_request_id",
]
```

---

#### **STEP 2: Tạo file `src/middlewares/correlation.py`**

```python
# src/middlewares/correlation.py

import uuid
import contextvars
from starlette.middleware.base import BaseHTTPMiddleware
from fastapi import Request

# ContextVar: thread-safe storage cho correlation ID
# Cho phép truy cập ID từ bất kỳ đâu trong cùng context (request/task)
_correlation_var = contextvars.ContextVar("correlation_id", default=None)


class CorrelationIdMiddleware(BaseHTTPMiddleware):
    """
    Middleware để generate/propagate correlation ID cho mỗi request.

    Luồng hoạt động:
    1. Extract X-Correlation-ID từ request header (nếu có)
    2. Nếu không có → generate UUID mới
    3. Lưu vào request.state (truy cập trong handler)
    4. Lưu vào ContextVar (truy cập ở mọi nơi)
    5. Gắn vào response header
    """

    async def dispatch(self, request: Request, call_next):
        # Bước 1: Extract hoặc generate correlation ID
        correlation_id = request.headers.get("X-Correlation-ID")
        if not correlation_id:
            correlation_id = str(uuid.uuid4())

        # Bước 2: Lưu vào request.state (handler có thể dùng request.state.correlation_id)
        request.state.correlation_id = correlation_id

        # Bước 3: Lưu vào ContextVar (logs và nested functions có thể dùng)
        _correlation_var.set(correlation_id)

        # Bước 4: Xử lý request
        response = await call_next(request)

        # Bước 5: Gắn correlation ID vào response header
        response.headers["X-Correlation-ID"] = correlation_id

        return response


# Helper function: Lấy correlation ID từ ContextVar
def get_current_correlation_id() -> str | None:
    """
    Lấy correlation ID hiện tại từ context.
    Dùng trong logs, background tasks, hoặc bất kỳ đâu.

    Returns:
        str | None: Correlation ID hoặc None nếu không có
    """
    return _correlation_var.get()


# Helper function: Set correlation ID (dùng cho background tasks/workers)
def set_current_correlation_id(value: str | None):
    """
    Set correlation ID vào context.
    Dùng khi restore context trong background task hoặc worker.

    Args:
        value: Correlation ID cần set
    """
    _correlation_var.set(value)


# Helper function: Lấy correlation ID từ request object
def get_request_id(request: Request) -> str | None:
    """
    Lấy correlation ID từ request.state.
    Alternative cho get_current_correlation_id() khi có request object.

    Args:
        request: FastAPI Request object

    Returns:
        str | None: Correlation ID hoặc None
    """
    return getattr(request.state, "correlation_id", None)
```

---

#### **STEP 3: Update `src/api/app.py` (Register middleware + logging)**

```python
# src/api/app.py

import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Import middleware và helpers
from src.middlewares.correlation import (
    CorrelationIdMiddleware,
    get_current_correlation_id,
    set_current_correlation_id,
)

# ==================== LOGGING SETUP ====================

# Custom logging filter để inject correlation_id vào mọi log record
class _CorrelationFilter(logging.Filter):
    """
    Logging filter để tự động thêm correlation_id vào LogRecord.
    Mọi log.info(), log.error() sẽ có thể dùng %(correlation_id)s
    """
    def filter(self, record):
        # Lấy correlation ID từ ContextVar
        record.correlation_id = get_current_correlation_id() or "-"
        return True


# Setup log format với correlation_id
LOG_FORMAT = "%(asctime)s %(levelname)s [%(correlation_id)s] %(name)s: %(message)s"
logging.basicConfig(level=logging.INFO, format=LOG_FORMAT)

# Attach filter vào root logger
logging.getLogger().addFilter(_CorrelationFilter())

# ==================== FASTAPI APP ====================

app = FastAPI(title="News Summarizer API")

# Đăng ký CorrelationIdMiddleware (ĐẦU TIÊN để propagate qua hết)
app.add_middleware(CorrelationIdMiddleware)

# Các middleware khác (CORS, Security, etc.)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Trong production nên giới hạn
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==================== ENDPOINTS ====================

@app.get("/healthz")
def healthz():
    """Health check endpoint"""
    correlation_id = get_current_correlation_id()
    logging.info("Health check called")  # Log tự động có [correlation_id]
    return {"status": "ok", "correlation_id": correlation_id}


from fastapi import BackgroundTasks
import subprocess
import os

# Global state để lưu trạng thái refresh
refresh_status = {
    "is_running": False,
    "last_run": None,
    "status": "idle",
    "correlation_id": None,
}


def _run_update_job(job_started_at: str, correlation_id: str):
    """
    Background task để chạy update_news.py

    Args:
        job_started_at: Timestamp khi job bắt đầu
        correlation_id: Correlation ID từ request
    """
    try:
        # Restore correlation ID trong background task context
        set_current_correlation_id(correlation_id)

        logging.info("Starting news update job")

        # Chạy subprocess với correlation ID truyền qua env variable
        env = os.environ.copy()
        env["X_CORRELATION_ID"] = correlation_id  # Truyền ID cho subprocess

        result = subprocess.run(
            ["python", "update_news.py"],
            capture_output=True,
            text=True,
            env=env,  # Truyền env có correlation ID
        )

        if result.returncode == 0:
            logging.info("News update job completed successfully")
            refresh_status["status"] = "completed"
        else:
            logging.error(f"News update job failed: {result.stderr}")
            refresh_status["status"] = "failed"

    except Exception as e:
        logging.error(f"Error running news update job: {e}")
        refresh_status["status"] = "error"

    finally:
        refresh_status["is_running"] = False
        # Clear correlation ID sau khi task hoàn thành
        set_current_correlation_id(None)


@app.post("/api/refresh")
def trigger_refresh(background_tasks: BackgroundTasks):
    """
    Trigger news refresh job in background

    Returns correlation_id để client có thể track job
    """
    if refresh_status["is_running"]:
        return {
            "status": "already_running",
            "correlation_id": refresh_status.get("correlation_id"),
        }

    # Capture correlation ID từ request context
    correlation_id = get_current_correlation_id()

    # Update status
    refresh_status["is_running"] = True
    refresh_status["correlation_id"] = correlation_id
    refresh_status["status"] = "running"

    from datetime import datetime
    job_started_at = datetime.now().isoformat()
    refresh_status["last_run"] = job_started_at

    # Queue background task với correlation ID
    background_tasks.add_task(_run_update_job, job_started_at, correlation_id)

    logging.info("News refresh job queued")

    return {
        "status": "started",
        "correlation_id": correlation_id,  # Trả về ID cho client
        "started_at": job_started_at,
    }


@app.get("/api/refresh/status")
def get_refresh_status():
    """
    Get current refresh job status

    Includes correlation_id để client track job
    """
    return refresh_status


# ==================== STARTUP ====================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

---

#### **STEP 4: Update `update_news.py` (Subprocess với correlation ID)**

```python
# update_news.py

import os
import logging

# Setup logging
LOG_FORMAT = "%(asctime)s %(levelname)s [%(correlation_id)s] %(name)s: %(message)s"
logging.basicConfig(level=logging.INFO, format=LOG_FORMAT)
logger = logging.getLogger("update_news")

# Đọc correlation ID từ environment variable
CORRELATION_ID = os.environ.get("X_CORRELATION_ID")

# Dùng LoggerAdapter để inject correlation_id vào logs
if CORRELATION_ID:
    logger = logging.LoggerAdapter(logger, {"correlation_id": CORRELATION_ID})
else:
    logger = logging.LoggerAdapter(logger, {"correlation_id": "-"})


def main():
    """Main pipeline function"""
    logger.info("Starting news update pipeline")

    try:
        # Fetch news
        logger.info("Fetching news from Techmeme")
        # ... fetch logic ...

        # Summarize with AI
        logger.info("Summarizing articles with Gemini")
        # ... summarization logic ...

        # Save results
        logger.info("Saving results to database")
        # ... save logic ...

        logger.info("Pipeline completed successfully")

    except Exception as e:
        logger.error(f"Pipeline failed: {e}", exc_info=True)
        raise


if __name__ == "__main__":
    main()
```

---

#### **STEP 5 (Optional): Celery task với correlation ID**

```python
# src/tasks/celery_tasks.py

from celery import Celery
import logging
import os
from src.middlewares.correlation import set_current_correlation_id

app = Celery("tasks", broker="redis://localhost:6379/0")

LOG_FORMAT = "%(asctime)s %(levelname)s [%(correlation_id)s] %(name)s: %(message)s"
logging.basicConfig(level=logging.INFO, format=LOG_FORMAT)


class _CorrelationFilter(logging.Filter):
    def filter(self, record):
        from src.middlewares.correlation import get_current_correlation_id
        record.correlation_id = get_current_correlation_id() or "-"
        return True

logging.getLogger().addFilter(_CorrelationFilter())


@app.task
def run_pipeline_task(correlation_id: str):
    """
    Celery task để chạy pipeline với correlation ID

    Args:
        correlation_id: Correlation ID từ API request
    """
    try:
        # Restore correlation ID trong worker context
        set_current_correlation_id(correlation_id)

        logging.info("Worker task started")

        # Nếu gọi subprocess, truyền correlation ID qua env
        env = os.environ.copy()
        env["X_CORRELATION_ID"] = correlation_id

        # ... task logic ...

        logging.info("Worker task completed")

    finally:
        # Clear context
        set_current_correlation_id(None)
```

---

## 7. Cách chạy và kiểm tra

### 7.1. Start server

```powershell
# Activate venv
cd E:\Viscode\Demo_Skola
.\.venv\Scripts\Activate.ps1

# Start FastAPI server
uvicorn src.api.app:app --reload --port 8000
```

**Expected output:**

```
INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
INFO:     Started reloader process
INFO:     Started server process
INFO:     Waiting for application startup.
INFO:     Application startup complete.
```

---

### 7.2. Test Case 1: Request không có correlation ID

```powershell
# Call endpoint (không gửi header)
curl.exe -i http://127.0.0.1:8000/healthz
```

**Expected response:**

```
HTTP/1.1 200 OK
content-type: application/json
x-correlation-id: 7a8f3e12-4c5d-4e9f-a1b2-8d9f3e12c5d4  ← Server tạo mới

{"status":"ok","correlation_id":"7a8f3e12-4c5d-4e9f-a1b2-8d9f3e12c5d4"}
```

**Server logs:**

```
2024-10-18 10:30:01 INFO [7a8f3e12-4c5d-4e9f-a1b2-8d9f3e12c5d4] uvicorn.access: GET /healthz HTTP/1.1 200
2024-10-18 10:30:01 INFO [7a8f3e12-4c5d-4e9f-a1b2-8d9f3e12c5d4] app: Health check called
```

✅ **Verify:** Response header có `x-correlation-id`, logs có `[correlation-id]`

---

### 7.3. Test Case 2: Request CÓ correlation ID

```powershell
# Call endpoint với custom correlation ID
curl.exe -i -H "X-Correlation-ID: my-test-123" http://127.0.0.1:8000/healthz
```

**Expected response:**

```
HTTP/1.1 200 OK
x-correlation-id: my-test-123  ← Server dùng ID từ client

{"status":"ok","correlation_id":"my-test-123"}
```

**Server logs:**

```
2024-10-18 10:31:00 INFO [my-test-123] uvicorn.access: GET /healthz HTTP/1.1 200
2024-10-18 10:31:00 INFO [my-test-123] app: Health check called
```

✅ **Verify:** Server propagate ID từ client

---

### 7.4. Test Case 3: Background task với correlation ID

```powershell
# Trigger refresh job
curl.exe -i -H "X-Correlation-ID: bg-task-456" http://127.0.0.1:8000/api/refresh
```

**Expected response:**

```
HTTP/1.1 200 OK
x-correlation-id: bg-task-456

{
  "status": "started",
  "correlation_id": "bg-task-456",
  "started_at": "2024-10-18T10:32:00.123456"
}
```

**Server logs:**

```
2024-10-18 10:32:00 INFO [bg-task-456] app: News refresh job queued
2024-10-18 10:32:01 INFO [bg-task-456] app: Starting news update job
2024-10-18 10:32:05 INFO [bg-task-456] update_news: Starting news update pipeline
2024-10-18 10:32:10 INFO [bg-task-456] update_news: Fetching news from Techmeme
2024-10-18 10:32:15 INFO [bg-task-456] update_news: Pipeline completed successfully
2024-10-18 10:32:15 INFO [bg-task-456] app: News update job completed successfully
```

✅ **Verify:**

- Correlation ID propagate vào background task
- Subprocess (update_news.py) logs cũng có `[bg-task-456]`

---

### 7.5. Test Case 4: Check status với correlation ID

```powershell
# Check job status
curl.exe http://127.0.0.1:8000/api/refresh/status
```

**Expected response:**

```json
{
  "is_running": false,
  "last_run": "2024-10-18T10:32:00.123456",
  "status": "completed",
  "correlation_id": "bg-task-456"
}
```

✅ **Verify:** Status endpoint trả về correlation_id của job

---

### 7.6. Test Case 5: Multiple concurrent requests

```powershell
# Terminal 1
curl.exe -H "X-Correlation-ID: req-1" http://127.0.0.1:8000/healthz

# Terminal 2 (cùng lúc)
curl.exe -H "X-Correlation-ID: req-2" http://127.0.0.1:8000/healthz

# Terminal 3 (cùng lúc)
curl.exe -H "X-Correlation-ID: req-3" http://127.0.0.1:8000/healthz
```

**Server logs:**

```
2024-10-18 10:35:00 INFO [req-1] app: Health check called
2024-10-18 10:35:00 INFO [req-2] app: Health check called
2024-10-18 10:35:00 INFO [req-3] app: Health check called
```

✅ **Verify:** Logs phân biệt rõ ràng từng request

---

### 7.7. Grep logs theo correlation ID

```powershell
# Xem tất cả logs của một request cụ thể
Select-String -Path "logs/*.log" -Pattern "bg-task-456"
```

**Expected output:**

```
logs/app.log:123: 2024-10-18 10:32:00 INFO [bg-task-456] app: News refresh job queued
logs/app.log:124: 2024-10-18 10:32:01 INFO [bg-task-456] app: Starting news update job
logs/worker.log:45: 2024-10-18 10:32:05 INFO [bg-task-456] update_news: Starting pipeline
logs/worker.log:46: 2024-10-18 10:32:10 INFO [bg-task-456] update_news: Fetching news
logs/worker.log:47: 2024-10-18 10:32:15 INFO [bg-task-456] update_news: Completed
logs/app.log:125: 2024-10-18 10:32:15 INFO [bg-task-456] app: Job completed successfully
```

✅ **Verify:** Grep ra được toàn bộ journey của request

---

## 8. Troubleshooting

### Vấn đề 1: Logs không có correlation_id (hiển thị `[-]`)

**Nguyên nhân:**

- Logging filter chưa được attach
- ContextVar không được set

**Kiểm tra:**

```python
# Trong app.py
logging.getLogger().addFilter(_CorrelationFilter())  # Có dòng này chưa?

# Test:
from src.middlewares.correlation import get_current_correlation_id
print(get_current_correlation_id())  # Có trả về ID không?
```

**Fix:**

- Đảm bảo `_CorrelationFilter` được add vào root logger
- Đảm bảo middleware được register: `app.add_middleware(CorrelationIdMiddleware)`

---

### Vấn đề 2: Response header không có `X-Correlation-ID`

**Nguyên nhân:**

- Middleware không được register
- Middleware order sai (bị middleware khác ghi đè)

**Kiểm tra:**

```python
# app.py
app.add_middleware(CorrelationIdMiddleware)  # Phải có dòng này

# Test:
curl.exe -i http://127.0.0.1:8000/healthz | Select-String "x-correlation-id"
```

**Fix:**

- Register middleware đầu tiên (trước CORS, Security Headers, etc.)
- Check middleware implementation: `response.headers["X-Correlation-ID"] = correlation_id`

---

### Vấn đề 3: Background task logs không có correlation_id

**Nguyên nhân:**

- Không truyền correlation_id vào task
- Không restore ContextVar trong task

**Kiểm tra:**

```python
# Endpoint
correlation_id = get_current_correlation_id()
background_tasks.add_task(my_task, correlation_id=correlation_id)  # Truyền chưa?

# Task
def my_task(correlation_id: str):
    set_current_correlation_id(correlation_id)  # Restore chưa?
    logger.info("Task running")
```

**Fix:**

- Capture ID trước khi queue task: `correlation_id = get_current_correlation_id()`
- Truyền vào task params: `add_task(func, correlation_id=correlation_id)`
- Restore trong task: `set_current_correlation_id(correlation_id)`

---

### Vấn đề 4: Subprocess logs không có correlation_id

**Nguyên nhân:**

- Không truyền env variable
- Subprocess không đọc env variable

**Kiểm tra:**

```python
# Parent process
env = os.environ.copy()
env["X_CORRELATION_ID"] = correlation_id  # Có set chưa?
subprocess.run(..., env=env)  # Có truyền env chưa?

# Subprocess (update_news.py)
CORRELATION_ID = os.environ.get("X_CORRELATION_ID")  # Có đọc chưa?
print(f"Received ID: {CORRELATION_ID}")
```

**Fix:**

- Truyền env variable: `env["X_CORRELATION_ID"] = correlation_id`
- Subprocess đọc và setup logger: `LoggerAdapter(logger, {"correlation_id": CORRELATION_ID})`

---

### Vấn đề 5: Multiple requests có cùng correlation_id

**Nguyên nhân:**

- Dùng global variable thay vì ContextVar
- ContextVar không được clear

**Kiểm tra:**

```python
# SelfCheck: Có dùng ContextVar không?
_correlation_var = contextvars.ContextVar(...)  # ✅ Đúng
CORRELATION_ID = None  # ❌ Sai (global var không thread-safe)
```

**Fix:**

- Luôn dùng `contextvars.ContextVar`
- Clear sau khi task hoàn thành: `set_current_correlation_id(None)`

---

### Vấn đề 6: Logs quá nhiều, khó filter

**Giải pháp:**

**1. Grep theo correlation ID:**

```powershell
# Windows PowerShell
Select-String -Path "logs/*.log" -Pattern "\[req-abc-123\]"

# Linux/Mac
grep "req-abc-123" logs/*.log
```

**2. Structured logging (JSON format):**

```python
import json_log_formatter

formatter = json_log_formatter.JSONFormatter()
handler = logging.StreamHandler()
handler.setFormatter(formatter)

# Log output:
# {"time":"2024-10-18T10:30:00","level":"INFO","correlation_id":"req-123","message":"Processing"}

# Query bằng jq:
# cat logs/app.log | jq 'select(.correlation_id=="req-123")'
```

**3. Centralized logging (ELK, Splunk, CloudWatch):**

- Ship logs với correlation_id field
- Query: `correlation_id:"req-abc-123"`

---

## 9. Best Practices

### ✅ DO (Nên làm)

#### 1. **Luôn propagate correlation ID qua mọi tầng**

```python
# API → Background Task
correlation_id = get_current_correlation_id()
background_tasks.add_task(my_task, correlation_id=correlation_id)

# API → Subprocess
env["X_CORRELATION_ID"] = correlation_id

# API → Message Queue
queue.send(message, metadata={"correlation_id": correlation_id})
```

#### 2. **Include correlation ID trong error responses**

```python
@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    correlation_id = get_current_correlation_id() or "-"
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "correlation_id": correlation_id,  # Client có thể báo lỗi với ID này
        },
    )
```

#### 3. **Log correlation ID ở đầu và cuối mỗi operation**

```python
def process_request():
    logger.info("Request processing started")
    try:
        # ... logic ...
        logger.info("Request processing completed")
    except Exception as e:
        logger.error(f"Request processing failed: {e}")
```

#### 4. **Dùng meaningful correlation ID format (optional)**

```python
# Thay vì UUID random:
# 550e8400-e29b-41d4-a716-446655440000

# Có thể dùng format có ý nghĩa:
correlation_id = f"req-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"
# → req-20241018-a3f5e9c2

# Lợi ích: Nhìn ID biết ngay request của ngày nào
```

#### 5. **Set timeout cho background tasks**

```python
import asyncio

async def timed_task(correlation_id: str):
    set_current_correlation_id(correlation_id)
    try:
        await asyncio.wait_for(long_running_task(), timeout=30.0)
    except asyncio.TimeoutError:
        logger.error("Task timeout after 30s")
```

#### 6. **Store correlation ID trong database records**

```python
# Khi lưu dữ liệu vào DB
article = {
    "title": "...",
    "content": "...",
    "created_at": datetime.now(),
    "correlation_id": get_current_correlation_id(),  # Để audit sau này
}
db.insert(article)
```

---

### ❌ DON'T (Không nên làm)

#### 1. **Dùng global variable cho correlation ID**

```python
# ❌ SAI
CORRELATION_ID = None  # Không thread-safe, concurrent requests sẽ lỗi

def middleware(request):
    global CORRELATION_ID
    CORRELATION_ID = generate_id()

# ✅ ĐÚNG
_correlation_var = contextvars.ContextVar("correlation_id", default=None)
```

#### 2. **Hardcode correlation ID**

```python
# ❌ SAI
correlation_id = "static-id-123"  # Mọi request đều cùng ID

# ✅ ĐÚNG
correlation_id = str(uuid.uuid4())  # Mỗi request một ID unique
```

#### 3. **Ignore correlation ID từ client**

```python
# ❌ SAI
correlation_id = str(uuid.uuid4())  # Luôn tạo mới, bỏ qua client header

# ✅ ĐÚNG
correlation_id = request.headers.get("X-Correlation-ID") or str(uuid.uuid4())
```

#### 4. **Không clear ContextVar sau khi task done**

```python
# ❌ SAI
def background_task(correlation_id):
    set_current_correlation_id(correlation_id)
    do_work()
    # Không clear → task sau có thể dùng nhầm ID cũ

# ✅ ĐÚNG
def background_task(correlation_id):
    try:
        set_current_correlation_id(correlation_id)
        do_work()
    finally:
        set_current_correlation_id(None)
```

#### 5. **Log quá ít hoặc quá nhiều**

```python
# ❌ Quá ít (không đủ context)
logger.info("Processing")

# ❌ Quá nhiều (spam logs)
logger.info("Step 1")
logger.info("Step 1.1")
logger.info("Step 1.1.1")
logger.info("Step 1.1.1.1")

# ✅ Vừa đủ (key milestones + errors)
logger.info("Request processing started")
logger.info("Fetched 10 articles from feed")
logger.error("Failed to call AI API: timeout")
logger.info("Request processing completed in 2.3s")
```

#### 6. **Expose correlation ID trong public error messages**

```python
# ❌ SAI (security risk nếu ID có thông tin nhạy cảm)
return {"error": "Database failed", "correlation_id": correlation_id}

# ✅ ĐÚNG (chỉ trong internal logs, hoặc safe ID)
logger.error(f"Database failed")  # Log có [correlation_id] tự động
return {"error": "Internal error", "support_reference": correlation_id}  # OK nếu ID = UUID
```

---

## 10. Tổng kết

### Correlation ID là gì?

- Mã định danh duy nhất cho mỗi request/transaction
- Giống "mã vận đơn" để track request qua nhiều tầng hệ thống

### Tại sao cần?

- **Debug:** Filter logs theo ID → tìm lỗi trong giây lát
- **Trace:** Xem request đi qua service nào, step nào chậm
- **Support:** User báo lỗi với ID → dev tra ngay được

### Hoạt động như thế nào?

1. Request arrives → Middleware generate/extract ID
2. Lưu vào ContextVar (thread-safe)
3. Mọi log tự động include ID (via logging filter)
4. Propagate vào background tasks, subprocess, workers
5. Response trả về ID trong header

### Cài đặt (5 steps):

1. Tạo `src/middlewares/correlation.py` (middleware + helpers)
2. Update `src/api/app.py` (register middleware + logging filter)
3. Update `update_news.py` (đọc env var, setup logger)
4. (Optional) Update workers/Celery tasks
5. Test với curl → verify logs và response headers

### Kiểm tra:

```powershell
# Start server
uvicorn src.api.app:app --reload --port 8000

# Test
curl.exe -i -H "X-Correlation-ID: test-123" http://127.0.0.1:8000/healthz

# Verify logs
# → Thấy [test-123] trong logs
# → Response header có x-correlation-id: test-123
```

### Troubleshooting:

- Logs không có ID → Check logging filter và middleware registration
- Background task không có ID → Truyền param + restore ContextVar
- Subprocess không có ID → Set env var + LoggerAdapter

### Best practices:

- ✅ Propagate ID qua mọi tầng
- ✅ Include ID trong error responses
- ✅ Dùng ContextVar (thread-safe)
- ❌ Không dùng global variable
- ❌ Không hardcode ID

---

## 11. Resources & Next Steps

### Tài liệu liên quan:

- [MIDDLEWARE_PATTERN_GUIDE.md](./MIDDLEWARE_PATTERN_GUIDE.md) - Template để thêm patterns khác
- [IMPLEMENTATION_CHECKLIST.md](./IMPLEMENTATION_CHECKLIST.md) - Roadmap tổng thể

### Patterns khác có thể implement tiếp:

1. **Global Exception Handler** (centralized error responses với correlation ID)
2. **Request Guard** (validate size/content-type)
3. **Security Headers** (CSP, X-Frame-Options, HSTS)
4. **Rate Limiting** (protect endpoints với slowapi)
5. **Audit Logging** (log request/response metadata)

### Advanced topics:

- **Distributed Tracing:** OpenTelemetry để trace qua microservices
- **Structured Logging:** JSON format + ELK stack
- **Correlation ID propagation:** HTTP client (requests, httpx) tự động gửi header

---

## 12. FAQ

**Q: Correlation ID có bắt buộc không?**  
A: Không bắt buộc, nhưng rất khuyến khích cho production systems. Không có ID = debug nightmare.

**Q: Client có bắt buộc phải gửi X-Correlation-ID không?**  
A: Không. Nếu client không gửi, server tự động tạo UUID mới.

**Q: Có thể dùng correlation ID để security authentication không?**  
A: KHÔNG. Correlation ID chỉ để tracking, không phải security token. Dùng JWT/API-key cho auth.

**Q: Correlation ID có được lưu vĩnh viễn không?**  
A: Tùy use case. Thường lưu trong logs (30-90 ngày), có thể lưu DB cho audit trail.

**Q: Performance impact khi dùng correlation ID?**  
A: Rất nhỏ (~1-2ms per request). Lợi ích >>> cost.

**Q: Có thể dùng correlation ID cho analytics không?**  
A: Có. Track user journey, conversion funnel với correlation ID.

---

**Hết. Chúc bạn implement thành công! 🚀**
