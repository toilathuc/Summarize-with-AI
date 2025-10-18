## IMPLEMENTATION CHECKLIST — Lộ trình thực thi (chi tiết)

Tập hợp các yêu cầu và checklist chi tiết để bạn thực hiện kế hoạch nâng cấp dự án từ hiện trạng (monolith) sang kiến trúc modular, chuẩn hoá dev environment, tách adapter, storage, background jobs, caching, rate-limiting, auth và observability.

Mỗi phần gồm: Mục tiêu ngắn, các bước cụ thể, file liên quan, lệnh kiểm tra nhanh, và tiêu chí chấp nhận (Definition of Done).

---

## 0. Chuẩn hoá môi trường (Dev environment)

Mục tiêu: Có môi trường phát triển lặp lại được, tránh xung đột interpreter.

Các bước:

- Tạo virtualenv trong project: `.venv` (Windows)
- Kích hoạt venv và cài dependencies cơ bản
- Pin dependencies vào `requirements.txt`

Files liên quan:

- `requirements.txt`, `README.md`

Lệnh nhanh (PowerShell):

```
cd E:\Viscode\Demo_Skola
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install --upgrade pip
pip install -r requirements.txt    # hoặc pip install <packages> nếu chưa có
python -c "import google.generativeai; print('OK')"
```

Definition of Done (DoD):

- Venv sẵn sàng và `python -c "import google.generativeai"` không lỗi.
- `requirements.txt` có phiên bản khóa (pinned) hoặc `pip freeze` đã chạy.

Estimated effort: 0.5 day

---

## 1. Cấu hình tập trung (Pydantic Settings)

python -c "from src.config.settings import settings; print('APP_ENV=', settings.APP_ENV)"

Estimated effort: 0.5–1 day

---

## 2. Tạo Adapter / Interface cho AI client

Mục tiêu: Tách dependency trực tiếp lên `google.generativeai` bằng interface để dễ mock, test, và thay provider.

Các bước:

- Tạo `src/clients/ai_client.py` (abstract base class hoặc Protocol) với method `generate(prompt: str) -> str`.
- Tạo `src/clients/gemini_adapter.py` implement interface này, bọc logic gọi API, mapping inputs/outputs.
- Thêm mock `tests/mocks/mock_ai_client.py` để dùng trong unit tests.

Files liên quan:

- `src/clients/ai_client.py`, `src/clients/gemini_adapter.py`, tests.

Lệnh kiểm tra:

```
python -c "from src.clients.gemini_adapter import GeminiAdapter; print(callable(GeminiAdapter().generate))"
```

DoD:

- Service chỉ gọi `AIClient.generate(...)` thay vì gọi `google.generativeai` trực tiếp.
- Có unit-test mock được adapter và kiểm tra luồng tóm tắt.

Estimated effort: 1–2 days

---

## 3. Trừu tượng hoá Storage (Repository pattern)

Mục tiêu: Tách file-based persistence thành StorageService để dễ thay thế bằng S3/DB.

Các bước:

- Tạo `src/services/storage_service.py` với interface `StorageService` (save/read/list).
- Implement `LocalStorageService` dùng atomic write: write to `tmp` file -> os.replace/rename.
- Thêm optional `S3StorageService` (placeholder) nếu cần.

Files liên quan:

- `src/services/storage_service.py`, nơi gọi `summaries.json` (ví dụ `src/api/app.py`, `update_news.py`).

Lệnh kiểm tra:

```
python -c "from src.services.storage_service import LocalStorageService; s=LocalStorageService('data/outputs'); s.save('test.json', b'{}'); print('saved')"
```

DoD:

- Ghi/đọc file thực hiện qua `StorageService`.
- Atomic write bảo đảm file không bị partial write.

Estimated effort: 1 day

---

## 4. Refactor pipeline thành hàm importable & idempotency

Mục tiêu: `run_pipeline(job_id=None)` có thể gọi bởi worker hoặc API và dễ test.

Các bước:

- Di chuyển logic từ `update_news.py` vào `src/pipelines/news_pipeline.py` với function `run_pipeline(job_id=None)`.
- Thiết kế trả về `JobResult` object: `{job_id, status, total, errors, updated_file}`.
- Thêm idempotency: accept `job_id` và lưu dấu đã chạy (Redis key hoặc file lock) để tránh chạy trùng.

Files liên quan:

- `update_news.py` (caller), `src/pipelines/news_pipeline.py`.

Lệnh kiểm tra:

```
python -c "from src.pipelines.news_pipeline import run_pipeline; print(run_pipeline())"
```

DoD:

- `run_pipeline()` chạy từ import context và tạo/điền `data/outputs/summaries.json`.
- Nếu gọi 2 lần với cùng `job_id`, không tạo duplicate work.

Estimated effort: 1–2 days

---

## 5. Background worker (bước đầu & chuẩn bị Celery)

Mục tiêu: Không block request, tách xử lý AI vào background.

Các bước (incremental):

- Bước A (dev quick): implement local queue (thread or multiprocessing) trong FastAPI service (`src/api/app.py`) để enqueue `run_pipeline()` và return job_id ngay.
- Bước B (prod-ready): tích hợp Celery/Arq/RQ + Redis broker, viết task wrapper `src/tasks/celery_tasks.py`.
- Thêm endpoint `/api/refresh` (POST) để enqueue và `/api/refresh/status/{job_id}` để truy vấn.

Files liên quan:

- FastAPI app (`src/api/app.py`), `src/tasks/*`.

Lệnh kiểm tra:

```
# start worker (local quick runner) và gọi API /api/refresh -> nhận job_id -> poll status
```

DoD:

- Enqueue trả job_id, không block HTTP thread, job thực hiện và cập nhật status/result.

Estimated effort: 2–5 days (dev->prod)

---

## 6. Retry/backoff & circuit-breaker

Mục tiêu: Làm cho các cuộc gọi bên ngoài (AI, fetching) bền bỉ hơn.

Các bước:

- Dùng `tenacity` để implement retry với backoff cho `AIClient.generate` và feed fetch.
- (Tuỳ chọn) Dùng `pybreaker` để tạm ngắt kết nối khi nhiều lỗi liên tiếp.

Files liên quan:

- `src/clients/gemini_adapter.py`, `src/services/feed_service.py`.

DoD:

- Retry policy config trong `settings` và log rõ các retry attempts.

Estimated effort: 1 day

---

## 7. Cache Aside + ETag cho `summaries.json`

Mục tiêu: Tăng tốc trả dữ liệu cho frontend, giảm IO.

Các bước:

- Cài Redis, tạo `src/services/cache_service.py` (get/set/invalidate).
- Khi pipeline hoàn tất, invalidate cache key và update stored value.
- API trả `ETag`/`Cache-Control` headers. Frontend có thể 304.

Files liên quan:

- `src/services/cache_service.py`, API endpoint for `/summaries.json`.

DoD:

- GET `/summaries.json` trả từ cache nếu có; invalidate on update.

Estimated effort: 1–2 days

---

## 8. Rate limiting & Security headers

Mục tiêu: Bảo vệ endpoint nhạy cảm (`/api/refresh`) và cứng hoá headers.

Các bước:

- Dev: `slowapi` with in-memory limiter or redis backend.
- Infra: Nginx rate-limiting (limit_req) tại edge.
- Thêm middleware để set security headers (CSP, X-Frame-Options, HSTS).

Files liên quan:

- FastAPI app, Nginx config (deploy).

DoD:

- Sau N requests / minute, API trả 429. Security headers có trong response.

Estimated effort: 1–2 days

---

## 9. Authentication & Authorization cho admin endpoints

Mục tiêu: Chỉ người có quyền mới trigger pipeline hoặc thao tác nhạy cảm.

Các bước:

- Implement simple API-key or OAuth2/JWT (FastAPI) cho `/api/refresh`.
- Tạo roles nếu cần (admin, readonly).

Files liên quan:

- FastAPI auth dependencies, `src/config/settings.py`.

DoD:

- Endpoint `/api/refresh` yêu cầu valid token/key; test coverage có kiểm tra auth.

Estimated effort: 1–2 days

---

## 10. Observability (logs, metrics, tracing) và Error tracking

Mục tiêu: Dễ debug, alert, và theo dõi sức khoẻ hệ thống.

Các bước:

- Structured logging: `python-json-logger` hoặc `structlog` + include `correlation_id`, `job_id`.
- Metrics: `prometheus_client` + `starlette_exporter` cho FastAPI.
- Tracing: OpenTelemetry collector (optional).
- Error reporting: `sentry-sdk`.

Files liên quan:

- app startup, middleware, logging config.

DoD:

- Có basic metrics endpoint, cấu hình Sentry, logs chứa correlation id.

Estimated effort: 2–3 days

---

## 11. Tests, CI và Containerization

Các bước:

- Viết unit tests cho adapter, storage, pipeline (pytest + mocks).
- Viết integration tests cho endpoints (FastAPI TestClient).
- Tạo `.github/workflows/ci.yml` để chạy lint + tests.
- Containerize: `Dockerfile` cho web/app, `docker-compose.yml` cho dev (web + worker + redis + postgres).

DoD:

- Tests pass locally and trên CI; `docker-compose up` khởi động stack dev.

Estimated effort: 3–7 days

---

## 12. Deployment checklist (production)

Ngắn gọn:

- Secrets: store in Vault / environment variables (no `.env` in git)
- TLS: terminate at Nginx / Load Balancer
- Reverse proxy: Nginx for static assets, gzip, HSTS
- Scale: web autoscale behind LB, workers scale separately
- Monitoring & alerts: Prometheus + Grafana + Sentry

---

## Appendix — Quick PR template (use for mỗi bước)

- Title: `feat: <area> — <short description>`
- Description: Why, What changed, How tested
- Files changed: list
- Tests: list and results
- Checklist: runs locally, passes unit tests, documentation updated

---

## Gợi ý tiến trình (sprint-sized)

- Sprint 1 (3–5 days): Env + settings + AI adapter + local storage impl + unit tests
- Sprint 2 (5–10 days): pipeline refactor (`run_pipeline`), local background runner, job status endpoint
- Sprint 3 (5–10 days): Redis + Celery or Arq integration, rate-limiting, auth
- Sprint 4 (5–10 days): DB migration (Postgres), query endpoints, pagination, metrics & Sentry
