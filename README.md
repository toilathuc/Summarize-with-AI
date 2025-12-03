🧠 TECH NEWS SUMMARIZER – FULL README (A → Z)

Spring Boot + Redis + SQLite + Firecrawl + Gemini + React (Vite)

📌 1. Giới thiệu

Tech News Summarizer là một hệ thống tự động:

Lấy tin tức từ Techmeme RSS.

Crawl nội dung thật bằng Firecrawl.

Tóm tắt bằng Gemini (Google AI).

Lưu dữ liệu vào SQLite + Redis.

Cung cấp REST API cho frontend.

Có scheduler tự refresh định kỳ.

Có manual refresh kèm rate-limit & Redis lock.

👉 Hệ thống gồm:

Backend (/backend): Spring Boot

Frontend (/frontend): React (Vite)

Redis: rate-limit, lock, cache

SQLite: database local để lưu article & summaries

📌 2. Yêu cầu hệ thống
Backend

Java 17 hoặc 21

Maven 3.8+

Redis (Linux/macOS dùng redis-server, Windows dùng Memurai)

Frontend

Node.js ≥ 18

npm ≥ 9

API Keys bắt buộc

Bạn cần 2 API key:

Gemini API key – Google AI Studio

Firecrawl API key – firecrawl.dev

Không commit key vào Git.

📌 3. Clone Project
git clone <repo>
cd summarizer-project

📌 4. Cấu hình môi trường
4.1. Export API keys
macOS / Linux
export GEMINI_API_KEY="your-gemini-key"
export FIRECRAWL_API_KEY="your-firecrawl-key"

Windows PowerShell
$env:GEMINI_API_KEY="your-gemini-key"
$env:FIRECRAWL_API_KEY="your-firecrawl-key"

4.2. Start Redis
Linux/macOS
redis-server

Windows (Memurai)

Ví dụ : & "C:\Program Files\Memurai\memurai-cli.exe"
127.0.0.1:6379> ping
PONG
127.0.0.1:6379>

Cài Memurai Community

Chạy: Memurai Server (mặc định port 6379)

4.3. Backend config (application.properties)

File ở:
/backend/src/main/resources/application.properties

server.port=8080

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
redis.key-prefix=summarizer

# Gemini
gemini.apiKey=${GEMINI_API_KEY:}
gemini.provider=google
gemini.model=gemini-flash-latest
gemini.useApiKeyAsQuery=true
gemini.maxRetries=2
gemini.endpoint=https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent

# Firecrawl
firecrawl.apiKey=${FIRECRAWL_API_KEY:}
firecrawl.endpoint=https://api.firecrawl.dev/v1/scrape
firecrawl.timeout.ms=20000

# SQLite
storage.database-path=./data/feeds/articles.db
storage.summaries-path=./data/outputs/summaries.db

# Auto refresh (ms)
refresh.interval.ms=600000   # 10 phút - đổi ý muốn tắt: đặt -1

# Summarizer
summarizer.batchSize=8
summarizer.promptTemplate=You are an assistant... {items_json}

📌 5. Chạy backend
cd backend
mvn spring-boot:run


Nếu chạy thành công sẽ thấy:

Started Application in X seconds
Initializing Spring DispatcherServlet 'dispatcherServlet'
Completed initialization

📌 6. Chạy frontend
cd frontend
npm install
npm run dev


Truy cập:
👉 http://localhost:5173

Nếu backend ở port khác, chỉnh file:

/frontend/.env.local

VITE_API_BASE_URL=http://localhost:8080

📌 7. API chính
7.1. Manual Refresh (gọi tóm tắt mới)
POST /api/refresh?top=20

Response – nếu job được bắt đầu
{
  "status": "started",
  "running": true,
  "correlation_id": "uuid",
  "top": 20
}

Nếu bị chặn vì job đang chạy
{
  "status": "running",
  "message": "Refresh already in progress",
  "reason": "manual_blocked_already_running"
}

Nếu bị rate-limit (spam quá nhanh)
{
  "status": "rate_limited",
  "scope": "refresh"
}

7.2. Lấy trạng thái job hiện tại
GET /api/refresh/status

{
  "running": false,
  "lastRunAt": "2025-12-03T21:54:11Z",
  "reason": "success"
}

7.3. Lấy danh sách summary
GET /api/summaries


Example:

{
  "total_items": 16,
  "last_updated": "2025-12-03T12:20:11Z",
  "items": [
    {
      "title": "Apple ra mắt chip AI mới...",
      "bullets": ["Ý chính 1", "Ý chính 2"],
      "url": "...",
      "source": "techmeme"
    }
  ]
}

📌 8. Luồng hoạt động hệ thống
✔ 1. Scheduled refresh

Chạy theo interval (vd 10 phút)

Nếu đang bận → skip (scheduled_skip_busy)

✔ 2. Manual refresh

Rate-limit: Redis

Redis lock: chống chạy chồng pipeline

Async chạy background

FE poll status hoặc reload summaries

✔ 3. Pipeline refresh

Fetch RSS → diff 15 reused, 1 new

Firecrawl enrich content

Gemini summarize

Lưu vào SQLite + SummaryStore

Unlock redis và kết thúc

📌 9. Thư mục quan trọng trong backend
service/
  ├── lock/                # RedisLockService
  ├── ratelimit/           # RedisRateLimitService
  ├── RefreshCoordinator   # Điều phối job refresh
  ├── ScheduledRefresh...  # Tự động refresh
  ├── FeedService          # Lấy RSS + diff
  ├── ContentCrawler...    # Firecrawl
  ├── Summarization...     # Gemini orchestrator

ports/                     # Clean Architecture ports
repository/                # SQLite repositories
clients/                   # FirecrawlClient, GeminiClient


Đây là kiến trúc kiểu Clean-Architecture + Hexagonal.

📌 10. Troubleshooting
❗ Lỗi 429 (trong log)

Nguồn Firecrawl → bị rate-limit Firecrawl
Giải pháp: tăng delay, giảm top, nâng plan Firecrawl.

❗ Lỗi 429 từ API refresh

Bạn spam refresh → Redis rate-limit đang hoạt động đúng.

❗ Lỗi Redis connect refused

Redis chưa chạy hoặc sai port.

❗ Gemini trả lỗi 403/401

Sai API key hoặc key không có quyền.

📌 11. Lệnh tóm tắt (TL;DR)
# Start Redis
redis-server                # hoặc chạy Memurai

# Backend
cd backend
export GEMINI_API_KEY="xxx"
export FIRECRAWL_API_KEY="yyy"
mvn spring-boot:run

# Frontend
cd frontend
npm install
npm run dev

# Test API
curl -X POST http://localhost:8080/api/refresh?top=20
curl http://localhost:8080/api/refresh/status
curl http://localhost:8080/api/summaries
