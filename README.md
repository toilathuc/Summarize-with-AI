# Summarize-with-AI

> Hệ thống tóm tắt tin tức công nghệ tự động bằng Groq, kiến trúc Hexagonal và Redis.

## Giới thiệu

Summarize-with-AI lấy tin từ Techmeme, trích link bài gốc, dùng Firecrawl để lấy nội dung bài viết và Groq để tóm tắt thành bullet points tiếng Việt.

## Tính năng chính

- Lấy feed Techmeme và chuẩn hóa link bài gốc
- Dùng Firecrawl để crawl nội dung bài viết
- Dùng Groq để tạo bản tóm tắt ngắn gọn
- Cache kết quả bằng Redis và SQLite
- Xử lý refresh bất đồng bộ

## Kiến trúc hiện tại

- `FeedClient`: đọc RSS Techmeme và lấy URL bài gốc
- `CrawlClient` + `ContentCrawlerService`: chỉ dùng Firecrawl để lấy nội dung
- `GroqClient`: gọi Groq chat completions để tóm tắt
- `SummarizationOrchestrator`: chia batch, retry và ghép kết quả

## Cấu hình

Backend dùng các biến chính:

- `GROQ_API_KEY`
- `FIRECRAWL_API_KEY`

Các giá trị mẫu nằm trong `backend/src/main/resources/application.properties`.

## Chạy dự án

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

## Ghi chú

- Crawl nội dung hiện chỉ dựa vào Firecrawl.
- Nếu Firecrawl hết quota hoặc lỗi, hệ thống sẽ log rõ và dùng fallback nội bộ từ dữ liệu RSS/description hiện có.
