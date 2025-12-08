# 🧠 Summarize with AI

> **Hệ thống tổng hợp tin tức công nghệ tự động bằng AI (Gemini). Kiến trúc Hexagonal tối ưu hiệu năng vượt trội.**

![Java](https://img.shields.io/badge/Java-21-orange) 
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green) 
![React](https://img.shields.io/badge/React-18-blue) 
![Redis](https://img.shields.io/badge/Redis-Cache_%26_Lock-red) 
![Architecture](https://img.shields.io/badge/Architecture-Hexagonal-purple)

---

## 📖 Giới thiệu

**Summarize with AI** là ứng dụng full-stack tự động thu thập, phân tích, và tóm tắt tin tức công nghệ từ các nguồn uy tín (ví dụ: Techmeme). Sử dụng **Google Gemini AI** để tạo bản tóm tắt ngắn gọn bằng tiếng Việt.

✨ **Điểm nổi bật**: Kiến trúc Hexagonal (Ports & Adapters) kết hợp kỹ thuật tối ưu hóa như **Asynchronous Processing**, **Redis Cache**, **Distributed Locking**, **Rate Limiting**; xử lý hàng nghìn request với độ trễ cực thấp (~2ms).

---

## 🏗️ Kiến trúc hệ thống

Kiến trúc **Hexagonal** giúp tối ưu hóa cho: Tách biệt business logic và các thành phần external. Linh hoạt mở rộng, dễ kiểm thử.

### **1. Core Domain**
- Chứa các business rules cốt lõi, không phụ thuộc framework hay database.
- Giao tiếp với các thành phần ngoài qua các **Port** (interface).

### **2. Ports**
- **Input Ports (Use Cases):** Định nghĩa hành động (VD: `GetNewsUseCase`, `RefreshNewsUseCase`).
- **Output Ports:** Interface để domain tương tác với external (VD: `NewsRepository`, `AIClient`, `CachePort`).

### **3. Adapters**
- **Primary (Driving):** REST Controllers, Schedulers (kích hoạt UseCase).
- **Secondary (Driven):** 
    - **Persistence:** SQLite (storage), Redis (cache).
    - **External Services:** Firecrawl (crawl), Gemini (summarize).

### ⚡ Tối ưu hiệu năng

- **Asynchronous Processing:** Làm mới tin tức xử lý bất đồng bộ → trả về `202 Accepted` ngay, không làm người dùng chờ.
- **Redis Cache-Aside:** Đọc dữ liệu nhanh, giảm tải DB.
- **Distributed Lock (Redis):** Đảm bảo chỉ 1 tiến trình refresh chạy, chống race condition.
- **Rate Limiting:** Giảm nguy cơ quá tải.

---

## 🔄 Luồng hoạt động (Workflow)

1. User bấm "Refresh" hoặc scheduler kích hoạt.
2. Fetch RSS: lấy danh sách bài viết mới từ Techmeme.
3. Filter: lọc bỏ bài đã có trong DB.
4. Crawl (Firecrawl): lấy nội dung chi tiết cho bài mới.
5. Summarize (Gemini): tóm tắt Markdown sang gạch đầu dòng bằng tiếng Việt.
6. Save: lưu vào SQLite, update cache Redis.
7. Serve: API trả dữ liệu siêu nhanh từ cache (~2ms).

---

## 🛠️ Tech Stack

### **Backend**
- **Java 21**
- **Spring Boot 3**
- **SQLite:** lưu trữ chính
- **Redis:** cache, lock, rate limit
- **AI:** Google Gemini Flash
- **Crawler:** Firecrawl API
- **Testing:** JUnit 5, Mockito, k6 (performance)

### **Frontend**
- **React 18 (Vite)**
- **CSS:** custom, concepts Tailwind
- **State:** React Hooks

---

## 🚀 Cài đặt & chạy dự án

### **1. Yêu cầu tiên quyết**
- **Java:** JDK 21+
- **Node.js:** >= 18
- **Redis:** cài và chạy (mặc định port 6379)
- **API Keys:**
    - `GEMINI_API_KEY` lấy từ Google AI Studio
    - `FIRECRAWL_API_KEY` lấy từ Firecrawl

### **2. Cấu hình Backend**
Chỉnh file `backend/src/main/resources/application.properties` với các API key:

```properties
# Gemini AI
gemini.apiKey=YOUR_GEMINI_API_KEY

# Firecrawl
firecrawl.apiKey=YOUR_FIRECRAWL_API_KEY

# Redis (mặc định localhost:6379)
spring.redis.host=localhost
spring.redis.port=6379
```

### **3. Chạy Backend**
```bash
cd backend
./mvnw spring-boot:run
```
_Server khởi động tại [`http://localhost:8080`](http://localhost:8080)_

### **4. Chạy Frontend**
```bash
cd frontend
npm install
npm run dev
```
_Truy cập ứng dụng tại [`http://localhost:5173`](http://localhost:5173)_

---

## 🔌 API Documentation

| Method | Endpoint            | Mô tả                                        |
|--------|---------------------|----------------------------------------------|
| GET    | `/api/news`         | Lấy danh sách tin đã tóm tắt (cache)         |
| POST   | `/api/news/refresh` | Kích hoạt làm mới tin tức (xử lý async)      |
| GET    | `/api/news/status`  | Kiểm tra trạng thái tiến trình refresh       |

---

## 📊 Hiệu năng (Performance)

Kiểm thử tải bằng **k6** (100 Virtual Users):

| Kịch bản   | Latency (Avg) | RPS | Kết quả                                          |
|------------|---------------|-----|--------------------------------------------------|
| Đọc        | ~2.78 ms      |  55 | Phản hồi tức thì nhờ cache Redis                  |
| Ghi/Refresh| ~2.21 ms      |  55 | Async API, không block request                    |
| Hỗn hợp    | ~2.47 ms      | 110 | Hệ thống hoàn toàn ổn định                       |

_💡 Phiên bản async mới nhanh hơn **400 lần** so với bản sync cũ khi xử lý nặng._

---

## 📂 Cấu trúc thư mục

```bash
Summarize-with-AI/
├── backend/                # Spring Boot app
│   ├── src/main/java/      # Source code (hexagonal)
│   ├── report/             # Báo cáo hiệu năng (k6)
│   └── ...
├── frontend/               # Ứng dụng React
│   ├── src/                # React components & hooks
│   └── ...
├── java-spring/            # Bản cũ (legacy, tham khảo)
└── README.md               # Tài liệu dự án
```

---

## 📝 License

Dự án tạo ra phục vụ mục đích học tập và nghiên cứu, không dùng thương mại.
