# 🎓 Hướng Dẫn Onboarding - Dự Án Summarize-with-AI

> **Mục đích:** Giúp bạn hiểu sâu về dự án từ vấn đề → giải pháp → code, để tự tin fix bug và thêm tính năng mới.

**Thời gian đọc:** ~45 phút | **Cấp độ:** Junior Developer (biết Python cơ bản)

---

## 📚 Mục Lục

1. [Vấn Đề Thực Tế](#1-vấn-đề-thực-tế)
2. [Giải Pháp](#2-giải-pháp)
3. [Kiến Trúc Tổng Quan](#3-kiến-trúc-tổng-quan)
4. [Code Walkthrough](#4-code-walkthrough)
5. [Data Flow](#5-data-flow)
6. [Cách Chạy & Demo](#6-cách-chạy--demo)
7. [Next Steps](#7-next-steps)

---

## 1. Vấn Đề Thực Tế

### 🤔 Bối cảnh

Bạn là một **developer/tech enthusiast** muốn cập nhật tin công nghệ mỗi ngày.

**Vấn đề gặp phải:**
- 📰 Có hàng trăm bài viết tech mỗi ngày (TechCrunch, Verge, Hacker News, Techmeme...)
- ⏰ **Không có thời gian** đọc hết tất cả
- 🔍 Khó biết bài nào **quan trọng** để đọc trước
- 🌐 Phải mở nhiều tab trình duyệt, rất lộn xộn

### 💡 Insight

> "Nếu có một công cụ tự động **lấy tin + tóm tắt bằng AI**, tôi chỉ cần đọc bản tóm tắt ngắn gọn, rồi quyết định có đọc full không."

### 🎯 Mục Tiêu Dự Án

Xây dựng một **web app** cho phép:
1. ✅ Tự động lấy tin công nghệ từ nguồn tin uy tín (Techmeme)
2. ✅ Dùng AI (Google Gemini) tóm tắt mỗi bài viết thành 2-3 câu
3. ✅ Hiển thị giao diện web đẹp, dễ đọc
4. ✅ Cập nhật nhanh (< 1 giây load data)

---

## 2. Giải Pháp

### 🏗️ Kiến Trúc Tổng Thể (High-Level)

```
┌─────────────────┐
│  Admin/Dev      │
│  (You)          │
└────────┬────────┘
         │ 1. Chạy update_news.py (CLI)
         ▼
┌─────────────────────────────────────────┐
│  Pipeline (Python Script)               │
│  ┌───────────────────────────────────┐  │
│  │ 1. Fetch từ Techmeme RSS          │  │
│  │ 2. AI Summarize (Gemini)          │  │
│  │ 3. Save to summaries.json         │  │
│  └───────────────────────────────────┘  │
└─────────────────┬───────────────────────┘
                  │ summaries.json
                  ▼
         ┌────────────────┐
         │  FastAPI Server │
         │  (localhost:8000)│
         └────────┬────────┘
                  │ HTTP GET /api/summaries
                  ▼
         ┌────────────────┐
         │  Web Browser   │
         │  (news.html)   │
         └────────────────┘
```

### 🔑 Các Thành Phần Chính

| Thành Phần | Vai Trò | Công Nghệ |
|------------|---------|-----------|
| **update_news.py** | Script cập nhật tin (chạy bằng CLI) | Python |
| **Pipeline** | Fetch → Summarize → Save | Python + Gemini API |
| **FastAPI Server** | Phục vụ API và web UI | FastAPI + Uvicorn |
| **Web Frontend** | Giao diện người dùng | HTML/CSS/JavaScript |
| **summaries.json** | Lưu trữ dữ liệu (database đơn giản) | JSON File |

### ⚡ Workflow (Luồng Hoạt Động)

**Bước 1: Cập nhật tin (Manual - do Admin làm)**
```bash
python update_news.py --top 30
```
→ Kết quả: File `data/outputs/summaries.json` được tạo/cập nhật

**Bước 2: Server phục vụ data**
```bash
start_fastapi.bat
```
→ Server chạy tại `http://localhost:8000`

**Bước 3: User truy cập**
- Mở browser → `http://localhost:8000`
- JavaScript gọi API → `GET /api/summaries`
- Hiển thị các bài tin đã tóm tắt

---

## 3. Kiến Trúc Tổng Quan

### 📁 Cấu Trúc Thư Mục (Folder Structure)

Xem chi tiết tại: [`diagrams/folder-structure.txt`](diagrams/folder-structure.txt)

```
Demo_Skola/
├── src/                    # ⭐ Backend Python code (API + Pipeline)
│   ├── api/               # FastAPI web server
│   ├── clients/           # Tích hợp với dịch vụ bên ngoài (Gemini AI)
│   ├── feeds/             # Lấy tin từ nguồn (Techmeme RSS)
│   ├── pipelines/         # Orchestration logic (điều phối workflow)
│   ├── services/          # Business logic (feed, summarize, storage)
│   └── utils/             # Helper functions (batching, JSON tools)
│
├── js/                     # ⭐ Frontend JavaScript code
│   ├── main.js            # Entry point
│   ├── services/          # API calls
│   └── ui/                # UI rendering, feedback, keyboard shortcuts
│
├── styles/                 # CSS styling
├── public/                 # Static files (index.html)
├── data/                   # ⭐ Data storage
│   └── outputs/
│       └── summaries.json # Database (file JSON)
│
├── update_news.py          # ⭐ Script cập nhật tin (CLI)
├── start_fastapi.bat       # ⭐ Script khởi động server
├── requirements.txt        # Python dependencies
└── .env                    # Environment variables (API keys)
```

### 🎯 Kiến Trúc Hiện Tại (Current)

**Pattern:** Monolithic with Services Pattern

```
┌─────────────────────────────────────────┐
│           Application Layer             │
│  ┌─────────────────────────────────┐    │
│  │  update_news.py (CLI)           │    │
│  │  src/api/app.py (FastAPI)       │    │
│  └─────────────────────────────────┘    │
└────────────────┬────────────────────────┘
                 │
┌────────────────┼────────────────────────┐
│         Service Layer                   │
│  ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │ Feed     │ │Summarize │ │ Storage │ │
│  │ Service  │ │ Service  │ │ Service │ │
│  └──────────┘ └──────────┘ └─────────┘ │
└────────────────┬────────────────────────┘
                 │
┌────────────────┼────────────────────────┐
│         Client/Adapter Layer            │
│  ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │ Techmeme │ │  Gemini  │ │   JSON  │ │
│  │ Client   │ │  Client  │ │  File   │ │
│  └──────────┘ └──────────┘ └─────────┘ │
└─────────────────────────────────────────┘
```

**Ưu điểm:**
- ✅ Đơn giản, dễ hiểu
- ✅ Phù hợp với dự án nhỏ

**Nhược điểm:**
- ❌ Services phụ thuộc trực tiếp vào clients → khó test
- ❌ Khó mở rộng (thêm nguồn tin mới phải sửa nhiều chỗ)

### 🔮 Kiến Trúc Mục Tiêu (Target - Trong Todo List)

**Pattern:** Hexagonal Architecture (Ports & Adapters)

Xem diagram chi tiết tại: [`ARCHITECTURE_EXPLAINED.md`](ARCHITECTURE_EXPLAINED.md)

---

## 4. Code Walkthrough

Giờ chúng ta đi vào từng phần code chi tiết! 🔍

### 📂 Phần 1: Backend (Python)

#### 4.1. Entry Point - `update_news.py`

**Mục đích:** Script chạy từ command line để cập nhật tin

**Code chính:**
```python
# Đọc từ Techmeme → AI tóm tắt → Lưu JSON
if __name__ == "__main__":
    # Khởi tạo pipeline (orchestrator)
    pipeline = NewsPipeline()
    
    # Chạy workflow
    summaries = pipeline.run(top_n=args.top)
    
    # Kết quả → summaries.json
```

**Giải thích từng bước:**
1. Parse arguments (`--top 30`)
2. Tạo `NewsPipeline` object (điều phối workflow)
3. Gọi `pipeline.run()` → thực hiện fetch + summarize + save
4. Kết quả lưu vào `data/outputs/summaries.json`

**Khi nào chạy script này?**
- ⏰ Buổi sáng (cập nhật tin mới)
- 🐛 Sau khi sửa bug trong pipeline
- 🔄 Khi muốn refresh data thủ công

---

#### 4.2. Pipeline - `src/pipelines/news_pipeline.py`

**Mục đích:** Orchestrator (điều phối workflow) - điều khiển các services

```python
class NewsPipeline:
    def __init__(self):
        self.feed_service = FeedService()          # Lấy tin
        self.summarizer = SummarizationService()   # Tóm tắt
        self.storage = StorageService()            # Lưu trữ
    
    def run(self, top_n: int):
        # Bước 1: Fetch tin từ Techmeme
        articles = self.feed_service.fetch_latest_articles(top_n)
        
        # Bước 2: AI tóm tắt (gọi Gemini API)
        summaries = self.summarizer.summarize_batch(articles)
        
        # Bước 3: Lưu vào file JSON
        self.storage.save_summaries(summaries)
        
        return summaries
```

**Tại sao cần Pipeline class?**
- ✅ **Tách rời logic:** Mỗi service làm 1 việc (Single Responsibility)
- ✅ **Dễ test:** Mock từng service riêng
- ✅ **Dễ mở rộng:** Thêm bước mới (ví dụ: filter spam) chỉ cần thêm 1 dòng

---

#### 4.3. Services Layer

##### a) `FeedService` - Lấy tin từ nguồn

File: `src/services/feed_service.py`

```python
class FeedService:
    def fetch_latest_articles(self, top_n: int):
        # Gọi TechmemeClient để lấy RSS feed
        client = TechmemeClient()
        articles = client.fetch_feed()  # Trả về list các bài viết
        
        # Giới hạn số lượng
        return articles[:top_n]
```

**Giải thích:**
- `TechmemeClient`: Class chuyên lấy data từ Techmeme RSS
- Trả về list các `Article` objects (title, url, source, published_date)

##### b) `SummarizationService` - Tóm tắt bằng AI

File: `src/services/summarization_service.py`

```python
class SummarizationService:
    def __init__(self):
        self.gemini_client = GeminiClient()  # Tích hợp Google Gemini
    
    def summarize_batch(self, articles: List[Article]):
        summaries = []
        for article in articles:
            # Gọi Gemini API để tóm tắt từng bài
            summary = self.gemini_client.summarize(article.content)
            summaries.append({
                "title": article.title,
                "url": article.url,
                "summary": summary,  # ← Đây là phần AI sinh ra
                "source": article.source
            })
        return summaries
```

**Chi tiết API Call:**
- Gửi prompt tới Gemini: "Summarize this article in 2-3 sentences"
- Gemini trả về text tóm tắt
- Giới hạn độ dài: max 300 ký tự

##### c) `StorageService` - Lưu trữ data

File: `src/services/storage_service.py`

```python
class StorageService:
    OUTPUT_FILE = "data/outputs/summaries.json"
    
    def save_summaries(self, summaries: List[dict]):
        # Tạo metadata
        data = {
            "last_updated": datetime.now().isoformat(),
            "total_items": len(summaries),
            "summaries": summaries
        }
        
        # Ghi vào file JSON
        with open(self.OUTPUT_FILE, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
```

**Tại sao dùng JSON file thay vì database?**
- ✅ Đơn giản, không cần setup MySQL/PostgreSQL
- ✅ Đủ nhanh cho ~15-30 bài viết
- ⚠️ **Giới hạn:** Nếu > 1000 bài → nên chuyển sang SQLite/Redis

---

#### 4.4. FastAPI Server - `src/api/app.py`

**Mục đích:** Web server phục vụ API và frontend

```python
from fastapi import FastAPI
from fastapi.responses import FileResponse, JSONResponse

app = FastAPI(title="Tech News Summarizer")

# Endpoint 1: Trang chủ
@app.get("/")
async def root():
    return FileResponse("news.html")

# Endpoint 2: API trả data
@app.get("/api/summaries")
async def get_summaries():
    # Đọc file JSON (nhanh, < 1s)
    data = load_summaries_from_file()
    
    # Thêm metadata
    return JSONResponse({
        "summaries": data["summaries"],
        "freshness": calculate_age(data["last_updated"]),
        "total": len(data["summaries"])
    })

# Endpoint 3: Health check
@app.get("/healthz")
async def health():
    return {"status": "ok"}
```

**Giải thích các endpoint:**

| Endpoint | Method | Mục Đích | Response |
|----------|--------|----------|----------|
| `/` | GET | Trả về trang HTML chính | `news.html` file |
| `/api/summaries` | GET | Trả về data JSON | `{"summaries": [...], "freshness": "2 hours ago"}` |
| `/healthz` | GET | Kiểm tra server còn sống không | `{"status": "ok"}` |

**Tại sao cần endpoint `/healthz`?**
- ✅ Monitoring tools (ví dụ: Docker health check) dùng để biết server còn chạy không
- ✅ Load balancer có thể check trước khi route traffic

---

### 📂 Phần 2: Frontend (JavaScript)

#### 4.5. Main App - `js/main.js`

**Mục đích:** Entry point của frontend, điều khiển toàn bộ UI

```javascript
// Entry point khi page load xong
document.addEventListener('DOMContentLoaded', async () => {
    // Hiển thị loading spinner
    showLoading();
    
    // Gọi API lấy data
    const data = await newsService.fetchSummaries();
    
    // Render data ra UI
    renderNews(data.summaries);
    
    // Ẩn loading
    hideLoading();
    
    // Setup các tính năng khác
    setupFilters();      // Filter theo nguồn
    setupKeyboard();     // Keyboard shortcuts
    setupRefresh();      // Nút refresh
});
```

**Giải thích flow:**
1. Browser load `news.html`
2. HTML gọi `main.js`
3. `main.js` gọi API `/api/summaries`
4. Nhận JSON response
5. Parse data và render HTML
6. User thấy các bài tin trên màn hình

---

#### 4.6. News Service - `js/services/newsService.js`

**Mục đích:** Gọi API để lấy data

```javascript
class NewsService {
    async fetchSummaries() {
        try {
            const response = await fetch('/api/summaries');
            if (!response.ok) throw new Error('Network error');
            
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Failed to fetch:', error);
            showErrorMessage('Unable to load news data');
            return null;
        }
    }
}
```

**Giải thích:**
- `fetch()`: JavaScript API để gọi HTTP request
- `await`: Chờ response trả về (async/await pattern)
- `response.json()`: Parse JSON string thành JavaScript object
- Error handling: Nếu lỗi → hiển thị thông báo lỗi cho user

---

#### 4.7. UI Rendering - `js/ui/render.js`

**Mục đích:** Chuyển data JSON thành HTML để hiển thị

```javascript
function renderNews(summaries) {
    const container = document.getElementById('news-container');
    
    // Xóa nội dung cũ
    container.innerHTML = '';
    
    // Tạo HTML cho từng bài viết
    summaries.forEach(article => {
        const card = createNewsCard(article);
        container.appendChild(card);
    });
}

function createNewsCard(article) {
    // Tạo HTML element
    const card = document.createElement('div');
    card.className = 'news-card';
    
    // Nội dung HTML
    card.innerHTML = `
        <h3><a href="${article.url}" target="_blank">${article.title}</a></h3>
        <p class="summary">${article.summary}</p>
        <div class="meta">
            <span class="source">${article.source}</span>
            <span class="date">${article.published_date}</span>
        </div>
    `;
    
    return card;
}
```

**Tại sao tách hàm `createNewsCard()`?**
- ✅ **Reusable:** Dùng lại khi thêm bài mới
- ✅ **Testable:** Dễ test riêng function này
- ✅ **Readable:** Code dễ đọc hơn

---

## 5. Data Flow

### 🔄 Luồng Dữ Liệu Chi Tiết

Xem diagram tại: [`diagrams/data-flow.mmd`](diagrams/data-flow.mmd)

**Scenario: User mở trang web để xem tin**

```
1. Browser Request
   User → localhost:8000
   
2. FastAPI Server
   app.get("/") → Trả về news.html
   
3. HTML Load
   Browser parse HTML → Load CSS & JS
   
4. JavaScript Execution
   main.js → document.addEventListener('DOMContentLoaded')
   
5. API Call
   newsService.fetchSummaries()
   → fetch('/api/summaries')
   
6. Server Response
   FastAPI đọc summaries.json
   → Trả về JSON:
   {
     "summaries": [
       {"title": "...", "summary": "...", "url": "..."},
       ...
     ],
     "freshness": "2 hours ago",
     "total": 15
   }
   
7. Client Processing
   Parse JSON → JavaScript objects
   
8. UI Rendering
   renderNews() → Tạo HTML elements
   → Append vào DOM
   
9. User Interaction
   User thấy các bài tin trên màn hình ✅
```

---

### 📊 Data Structure (Cấu Trúc Dữ Liệu)

**File: `data/outputs/summaries.json`**

```json
{
  "last_updated": "2025-10-29T10:30:00+07:00",
  "total_items": 15,
  "summaries": [
    {
      "title": "OpenAI Launches GPT-5",
      "url": "https://example.com/article",
      "summary": "OpenAI today announced GPT-5 with multimodal capabilities. The model shows significant improvements in reasoning and coding tasks.",
      "source": "TechCrunch",
      "published_date": "2025-10-29T09:00:00Z",
      "correlation_id": "abc123"
    }
  ]
}
```

**Field Explanation (Giải thích từng field):**

| Field | Type | Mục Đích | Ví Dụ |
|-------|------|----------|-------|
| `last_updated` | ISO datetime | Thời gian cập nhật gần nhất | `"2025-10-29T10:30:00+07:00"` |
| `total_items` | Integer | Tổng số bài viết | `15` |
| `title` | String | Tiêu đề bài viết | `"OpenAI Launches GPT-5"` |
| `url` | String (URL) | Link gốc | `"https://..."` |
| `summary` | String | Tóm tắt do AI sinh ra (2-3 câu) | `"OpenAI today..."` |
| `source` | String | Nguồn tin | `"TechCrunch"` |
| `published_date` | ISO datetime | Thời gian xuất bản | `"2025-10-29T09:00:00Z"` |
| `correlation_id` | String (UUID) | ID để trace log (debugging) | `"abc123"` |

---

## 6. Cách Chạy & Demo

### ▶️ Khởi Động Dự Án

Xem chi tiết tại: [`QUICK_START.md`](QUICK_START.md)

**Bước 1: Chuẩn bị môi trường**
```bash
# Kích hoạt virtual environment
.venv\Scripts\activate

# Kiểm tra packages đã cài chưa
pip list
```

**Bước 2: Cập nhật tin (nếu chưa có data)**
```bash
python update_news.py --top 30
```
→ Chờ ~30 giây (AI tóm tắt mất thời gian)

**Bước 3: Khởi động server**
```bash
start_fastapi.bat
```
→ Server chạy tại `http://localhost:8000`

**Bước 4: Mở browser**
```
http://localhost:8000
```
→ Bạn sẽ thấy giao diện web với các bài tin đã tóm tắt ✅

---

### 🎬 Demo Các Tính Năng

Xem script chi tiết tại: [`DEMO_SCRIPT.md`](DEMO_SCRIPT.md)

#### Tính năng 1: Xem tin tóm tắt
1. Mở `http://localhost:8000`
2. Các bài viết hiển thị dạng cards
3. Mỗi card có: Title, Summary (2-3 câu), Source, Date
4. Click vào title → Mở bài gốc ở tab mới

#### Tính năng 2: Filter theo nguồn
1. Nhìn thanh filter phía trên
2. Click vào source (ví dụ: "TechCrunch")
3. Chỉ hiển thị bài từ TechCrunch
4. Click "All" → Hiển thị lại tất cả

#### Tính năng 3: Refresh data
1. Click nút "Refresh" (biểu tượng ↻)
2. Loading spinner hiện ra
3. API gọi lại → Data cập nhật
4. Nếu có bài mới → Hiển thị thông báo

#### Tính năng 4: Keyboard shortcuts
- `R` → Refresh
- `1-9` → Mở bài thứ 1-9
- `F` → Focus vào search (nếu có)

---

## 7. Next Steps

### 🎯 Nhiệm Vụ Đầu Tiên (First Tasks)

Xem ví dụ chi tiết tại: [`TASK_EXAMPLES.md`](TASK_EXAMPLES.md)

#### Task 1: Fix Bug - "API trả về data cũ"
**Scenario:** User phàn nàn data không cập nhật dù đã chạy `update_news.py`

**Debug steps:**
1. Kiểm tra file `data/outputs/summaries.json` → Xem `last_updated`
2. Kiểm tra timestamp có đúng không?
3. Nếu sai → Check `StorageService.save_summaries()`

**Giải pháp thường gặp:**
- Quên chạy `update_news.py`
- Server đọc file cũ (cache) → Restart server
- Timezone sai → Fix trong code

#### Task 2: Thêm Tính Năng - Filter theo keyword
**Yêu cầu:** User muốn search theo từ khóa (ví dụ: "AI", "blockchain")

**Files cần sửa:**
1. `js/main.js` → Thêm search box
2. `js/ui/render.js` → Filter logic
3. `styles/filters.css` → Styling

**Code mẫu:**
```javascript
function filterByKeyword(keyword) {
    const filtered = allSummaries.filter(article => 
        article.title.toLowerCase().includes(keyword.toLowerCase()) ||
        article.summary.toLowerCase().includes(keyword.toLowerCase())
    );
    renderNews(filtered);
}
```

#### Task 3: Thêm Nguồn Tin Mới - Hacker News
**Yêu cầu:** Thêm Hacker News ngoài Techmeme

**Files cần tạo/sửa:**
1. `src/feeds/hackernews/client.py` → Fetch từ HN API
2. `src/services/feed_service.py` → Thêm HN vào pipeline
3. `update_news.py` → Argument `--source` (techmeme/hackernews/all)

**Độ khó:** Medium (cần hiểu API của Hacker News)

---

### 📚 Tài Liệu Tham Khảo Thêm

1. **Code Concepts (Khái niệm):** [`CODE_CONCEPTS.md`](CODE_CONCEPTS.md)
   - FastAPI là gì?
   - Async/Await hoạt động thế nào?
   - Environment variables
   - Virtual environment

2. **Architecture Explained (Kiến trúc):** [`ARCHITECTURE_EXPLAINED.md`](ARCHITECTURE_EXPLAINED.md)
   - Kiến trúc hiện tại vs. mục tiêu
   - Hexagonal Architecture pattern
   - Dependency Injection

3. **Contribution Guide (Quy trình):** [`CONTRIBUTION_GUIDE.md`](CONTRIBUTION_GUIDE.md)
   - Git workflow
   - Code review process
   - Testing guidelines

4. **Common Pitfalls (Lỗi thường gặp):** [`COMMON_PITFALLS.md`](COMMON_PITFALLS.md)
   - Module not found
   - Port already in use
   - API returns empty

---

## 🎓 Kết Luận

Sau khi đọc xong tài liệu này, bạn đã hiểu:

✅ **Vấn đề:** Tại sao cần dự án này (quá nhiều tin tech, không đọc hết)  
✅ **Giải pháp:** Tự động lấy tin + AI tóm tắt + Web UI đẹp  
✅ **Kiến trúc:** Services pattern với 3 layers (Service → Client → Storage)  
✅ **Code:** Từng file làm gì, tại sao cần file đó  
✅ **Data Flow:** Request đi từ browser → server → JSON → render HTML  
✅ **Demo:** Cách chạy và test các tính năng  
✅ **Next Steps:** Task đầu tiên bạn có thể làm  

---

## ❓ Hỏi & Đáp

**Q: Tôi nên bắt đầu từ đâu?**  
A: Đọc [`QUICK_START.md`](QUICK_START.md) → Chạy thử project → Đọc [`CODE_CONCEPTS.md`](CODE_CONCEPTS.md) → Làm task đầu tiên trong [`TASK_EXAMPLES.md`](TASK_EXAMPLES.md)

**Q: Tôi bị lỗi khi chạy, phải làm sao?**  
A: Xem [`COMMON_PITFALLS.md`](COMMON_PITFALLS.md) → Nếu không có → Hỏi team lead

**Q: Tôi muốn đóng góp code, quy trình là gì?**  
A: Đọc [`CONTRIBUTION_GUIDE.md`](CONTRIBUTION_GUIDE.md) → Tạo branch → Code → Commit → Pull Request

**Q: Tôi có ý tưởng tính năng mới, có nên làm không?**  
A: Hỏi team lead trước → Nếu OK → Tạo task trong GitHub Issues → Code

---

**Chúc bạn onboarding thành công! 🎉**

Nếu có thắc mắc, hỏi team lead hoặc tham khảo các file docs khác trong folder `presentation/`.
