# 🏗️ Architecture Explained - Giải Thích Kiến Trúc Chi Tiết

> **Mục đích:** Giải thích kiến trúc hiện tại, kiến trúc mục tiêu (hexagonal), và tại sao cần refactor.

**Thời gian đọc:** ~25 phút | **Cấp độ:** Junior to Mid-Level

---

## 📚 Mục Lục

1. [Kiến Trúc Hiện Tại](#1-kiến-trúc-hiện-tại)
2. [Vấn Đề Của Kiến Trúc Hiện Tại](#2-vấn-đề-của-kiến-trúc-hiện-tại)
3. [Kiến Trúc Mục Tiêu - Hexagonal](#3-kiến-trúc-mục-tiêu---hexagonal)
4. [Roadmap Refactoring](#4-roadmap-refactoring)
5. [Folder Structure Chi Tiết](#5-folder-structure-chi-tiết)

---

## 1. Kiến Trúc Hiện Tại

### 🎯 Pattern: Monolithic with Services

**Diagram:**

```
┌─────────────────────────────────────────────────────┐
│              Application Layer                      │
│  ┌──────────────────┐    ┌──────────────────┐      │
│  │  update_news.py  │    │  src/api/app.py  │      │
│  │  (CLI Script)    │    │  (FastAPI)       │      │
│  └────────┬─────────┘    └────────┬─────────┘      │
│           │                       │                 │
│           └───────────┬───────────┘                 │
└─────────────────────────┼─────────────────────────────┘
                          │
┌─────────────────────────┼─────────────────────────────┐
│              Service Layer                           │
│  ┌──────────────────┐  ┌──────────────────┐         │
│  │  FeedService     │  │ SummarizationSvc │         │
│  │  - fetch_latest()│  │ - summarize()    │         │
│  └────────┬─────────┘  └────────┬─────────┘         │
│           │                     │                    │
│  ┌────────┴─────────────────────┴─────┐             │
│  │        StorageService               │             │
│  │        - save_summaries()           │             │
│  │        - load_summaries()           │             │
│  └────────┬────────────────────────────┘             │
└───────────┼──────────────────────────────────────────┘
            │
┌───────────┼──────────────────────────────────────────┐
│              Client/Adapter Layer                    │
│  ┌─────────┴────────┐  ┌──────────────┐             │
│  │  TechmemeClient  │  │ GeminiClient │             │
│  │  - fetch_feed()  │  │ - summarize()│             │
│  └──────────────────┘  └──────────────┘             │
│                                                      │
│  ┌──────────────────────────────────┐               │
│  │  JSON File Storage               │               │
│  │  - data/outputs/summaries.json   │               │
│  └──────────────────────────────────┘               │
└──────────────────────────────────────────────────────┘
```

---

### 📂 Folder Mapping

| Folder | Layer | Vai Trò |
|--------|-------|---------|
| `update_news.py` | Application | Entry point (CLI) |
| `src/api/app.py` | Application | Entry point (Web API) |
| `src/services/` | Service | Business logic |
| `src/clients/` | Client | Tích hợp với external API (Gemini) |
| `src/feeds/` | Client | Lấy data từ RSS feed |
| `data/outputs/` | Storage | File-based database (JSON) |

---

### 🔄 Data Flow

**Scenario: Chạy `python update_news.py --top 30`**

```
1. update_news.py
   │
   ├─→ Tạo NewsPipeline()
   │
   └─→ pipeline.run(top_n=30)
       │
       ├─→ FeedService.fetch_latest_articles(30)
       │   │
       │   └─→ TechmemeClient.fetch_feed()
       │       │
       │       └─→ HTTP GET: https://techmeme.com/feed.xml
       │           │
       │           └─→ Parse RSS → List[Article]
       │
       ├─→ SummarizationService.summarize_batch(articles)
       │   │
       │   └─→ GeminiClient.summarize(article.content)
       │       │
       │       └─→ HTTP POST: Google Gemini API
       │           │
       │           └─→ AI-generated summary (2-3 câu)
       │
       └─→ StorageService.save_summaries(summaries)
           │
           └─→ json.dump() → data/outputs/summaries.json
```

**Thời gian:** ~30-60 giây (phụ thuộc vào số lượng bài và tốc độ Gemini API)

---

## 2. Vấn Đề Của Kiến Trúc Hiện Tại

### ❌ Các Vấn Đề

#### Vấn đề 1: **Tight Coupling (Phụ thuộc chặt)**

**Code hiện tại:**
```python
# src/services/feed_service.py
class FeedService:
    def fetch_latest_articles(self, top_n: int):
        # ❌ Phụ thuộc trực tiếp vào TechmemeClient
        client = TechmemeClient()
        return client.fetch_feed()[:top_n]
```

**Hậu quả:**
- Nếu muốn thêm nguồn tin mới (Hacker News) → Phải sửa code `FeedService`
- Khó test: Không thể mock `TechmemeClient` dễ dàng
- Không tuân theo **Dependency Inversion Principle** (SOLID)

---

#### Vấn đề 2: **Khó Mở Rộng**

**Scenario:** Muốn thêm nguồn tin Hacker News

**Hiện tại (BAD):**
```python
class FeedService:
    def fetch_latest_articles(self, source: str, top_n: int):
        if source == "techmeme":
            client = TechmemeClient()
        elif source == "hackernews":
            client = HackerNewsClient()  # ← Phải sửa service
        elif source == "reddit":
            client = RedditClient()  # ← Lại phải sửa
        # ... thêm nhiều if/elif
        
        return client.fetch_feed()[:top_n]
```

**Vấn đề:**
- Mỗi lần thêm nguồn → Sửa code service
- Vi phạm **Open/Closed Principle** (mở cho mở rộng, đóng cho sửa đổi)

---

#### Vấn đề 3: **Khó Test**

**Test code hiện tại:**
```python
# test/test_feed_service.py
def test_fetch_articles():
    service = FeedService()
    
    # ❌ Test này gọi THẬT API Techmeme
    # → Chậm, phụ thuộc internet, tốn quota
    articles = service.fetch_latest_articles(10)
    
    assert len(articles) == 10
```

**Vấn đề:**
- Test chậm (phải gọi thật API)
- Không ổn định (nếu Techmeme down → Test fail)
- Tốn quota API (nếu có limit)

---

#### Vấn đề 4: **Logic Kinh Doanh Lẫn Lộn**

**File: `src/pipelines/news_pipeline.py`**
```python
class NewsPipeline:
    def run(self, top_n: int):
        # Business logic
        articles = self.feed_service.fetch_latest_articles(top_n)
        
        # ❌ Logic lẫn lộn: vừa orchestrate, vừa xử lý data
        # Khó tái sử dụng ở chỗ khác
        summaries = self.summarizer.summarize_batch(articles)
        
        self.storage.save_summaries(summaries)
        return summaries
```

---

## 3. Kiến Trúc Mục Tiêu - Hexagonal

### 🎯 Pattern: Hexagonal Architecture (Ports & Adapters)

**Còn gọi là:**
- Clean Architecture
- Onion Architecture
- Ports & Adapters

---

### 🏛️ Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                    │
│  ┌──────────────────┐    ┌──────────────────┐          │
│  │  CLI Handler     │    │  FastAPI Router  │          │
│  │  (update_news.py)│    │  (/api/summaries)│          │
│  └────────┬─────────┘    └────────┬─────────┘          │
└───────────┼──────────────────────┼────────────────────────┘
            │                      │
            │  Gọi Use-Case        │
            └──────────┬───────────┘
                       │
┌──────────────────────┼──────────────────────────────────┐
│              Application Layer (Use-Cases)              │
│  ┌────────────────────┴───────────────────────────┐    │
│  │     SummarizeUC (Use-Case)                     │    │
│  │     - execute(source, top_n)                   │    │
│  │                                                 │    │
│  │  Logic: Fetch → Filter → Summarize → Save     │    │
│  └─────────┬───────────────┬───────────┬─────────┘    │
└────────────┼───────────────┼───────────┼──────────────┘
             │               │           │
             │ Dùng Ports    │           │
             ▼               ▼           ▼
┌─────────────────────────────────────────────────────────┐
│                  Domain Layer (Core)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │  FeedPort    │  │SummarizerPort│  │ StoragePort  │ │
│  │  (interface) │  │  (interface) │  │  (interface) │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Domain Models: Article, Summary                 │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
             ▲               ▲           ▲
             │ Implement     │           │
             │               │           │
┌────────────┼───────────────┼───────────┼──────────────┐
│              Infrastructure Layer (Adapters)          │
│  ┌──────────┴──────┐  ┌───┴──────────┐  ┌───┴──────┐ │
│  │  TechmemeAdapter│  │ GeminiAdapter│  │  FSAdapter││
│  │  (implements    │  │ (implements  │  │ (File     ││
│  │   FeedPort)     │  │  Summarizer) │  │  Storage) ││
│  └─────────────────┘  └──────────────┘  └───────────┘ │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  External Services: Techmeme API, Gemini API     │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

### 🔑 Các Thành Phần

#### 1. **Domain Layer (Tầng Lõi)**

**Vai trò:** Business logic thuần túy, không phụ thuộc vào bất kỳ thư viện/framework nào

**Gồm:**
- **Domain Models:** `Article`, `Summary` (data classes)
- **Ports (Interfaces):** `FeedPort`, `SummarizerPort`, `StoragePort`

**Ví dụ Port:**
```python
# src/domain/ports.py

from abc import ABC, abstractmethod
from typing import List
from src.domain.models import Article, Summary

class FeedPort(ABC):
    """Interface để lấy tin từ nguồn (Techmeme, Hacker News, v.v.)"""
    
    @abstractmethod
    def fetch_latest_articles(self, top_n: int) -> List[Article]:
        """Lấy top N bài viết mới nhất"""
        pass

class SummarizerPort(ABC):
    """Interface để tóm tắt nội dung"""
    
    @abstractmethod
    def summarize(self, content: str) -> str:
        """Tóm tắt nội dung thành 2-3 câu"""
        pass

class StoragePort(ABC):
    """Interface để lưu trữ/đọc dữ liệu"""
    
    @abstractmethod
    def save_summaries(self, summaries: List[Summary]) -> None:
        pass
    
    @abstractmethod
    def load_summaries(self) -> List[Summary]:
        pass
```

**Lợi ích:**
- ✅ **Dependency Inversion:** Domain không phụ thuộc vào Infrastructure
- ✅ **Testable:** Dễ dàng mock các ports

---

#### 2. **Application Layer (Use-Cases)**

**Vai trò:** Orchestration logic (điều phối workflow), sử dụng Ports

**Ví dụ Use-Case:**
```python
# src/application/use_cases/summarize_uc.py

class SummarizeUC:
    """Use-case: Lấy tin → Tóm tắt → Lưu"""
    
    def __init__(
        self,
        feed_port: FeedPort,
        summarizer_port: SummarizerPort,
        storage_port: StoragePort
    ):
        # ✅ Dependency Injection - nhận interfaces, không phải concrete classes
        self.feed_port = feed_port
        self.summarizer_port = summarizer_port
        self.storage_port = storage_port
    
    def execute(self, source: str, top_n: int) -> List[Summary]:
        # Bước 1: Fetch articles
        articles = self.feed_port.fetch_latest_articles(top_n)
        
        # Bước 2: Summarize
        summaries = []
        for article in articles:
            summary_text = self.summarizer_port.summarize(article.content)
            summaries.append(Summary(
                title=article.title,
                url=article.url,
                summary=summary_text,
                source=source
            ))
        
        # Bước 3: Save
        self.storage_port.save_summaries(summaries)
        
        return summaries
```

**Lợi ích:**
- ✅ **Single Responsibility:** Chỉ làm 1 việc (orchestrate)
- ✅ **Testable:** Mock ports dễ dàng
- ✅ **Reusable:** Dùng lại ở CLI và API

---

#### 3. **Infrastructure Layer (Adapters)**

**Vai trò:** Implement các Ports, tích hợp với external services

**Ví dụ Adapter:**
```python
# src/infrastructure/feed_techmeme.py

class TechmemeAdapter(FeedPort):
    """Adapter cho Techmeme RSS feed"""
    
    def __init__(self):
        self.client = TechmemeClient()
    
    def fetch_latest_articles(self, top_n: int) -> List[Article]:
        # Gọi client cũ (wrap lại)
        raw_articles = self.client.fetch_feed()
        
        # Convert sang Domain Model
        articles = [
            Article(
                title=item['title'],
                url=item['url'],
                content=item.get('content', ''),
                published_date=item['published']
            )
            for item in raw_articles[:top_n]
        ]
        
        return articles
```

**Lợi ích:**
- ✅ **Pluggable:** Dễ dàng thay thế adapter (Techmeme → Hacker News)
- ✅ **Isolate:** Thay đổi external API không ảnh hưởng domain

---

#### 4. **Presentation Layer**

**Vai trò:** Điểm vào của user (CLI, API), gọi Use-Cases

**Ví dụ CLI:**
```python
# update_news.py (sau khi refactor)

from src.application.use_cases.summarize_uc import SummarizeUC
from src.infrastructure.feed_techmeme import TechmemeAdapter
from src.infrastructure.llm_gemini import GeminiAdapter
from src.infrastructure.storage_fs import FileSystemAdapter

def main():
    # Dependency Injection
    feed_adapter = TechmemeAdapter()
    summarizer_adapter = GeminiAdapter()
    storage_adapter = FileSystemAdapter()
    
    # Tạo use-case với adapters
    uc = SummarizeUC(
        feed_port=feed_adapter,
        summarizer_port=summarizer_adapter,
        storage_port=storage_adapter
    )
    
    # Chạy
    summaries = uc.execute(source="techmeme", top_n=30)
    print(f"Saved {len(summaries)} summaries")

if __name__ == "__main__":
    main()
```

**Ví dụ API:**
```python
# src/presentation/api/v1/summarize_router.py

from fastapi import APIRouter
from src.application.use_cases.summarize_uc import SummarizeUC

router = APIRouter()

@router.get("/api/summaries")
async def get_summaries():
    # Gọi use-case (không phải gọi trực tiếp services)
    uc = build_summarize_uc()  # Factory pattern
    summaries = uc.execute(source="techmeme", top_n=30)
    
    return {"summaries": [s.dict() for s in summaries]}
```

---

## 4. Roadmap Refactoring

### 🗺️ Các Bước Refactor (Theo Todo List)

#### Bước 1: **Tạo Domain Layer**
- [ ] Tạo `src/domain/models.py` → Data classes (`Article`, `Summary`)
- [ ] Tạo `src/domain/ports.py` → Interfaces (`FeedPort`, `SummarizerPort`, `StoragePort`)

**Thời gian:** ~1-2 giờ

---

#### Bước 2: **Tạo Application Layer**
- [ ] Tạo `src/application/use_cases/summarize_uc.py`
- [ ] Implement logic orchestration (fetch → summarize → save)

**Thời gian:** ~2-3 giờ

---

#### Bước 3: **Tạo Infrastructure Adapters**
- [ ] `src/infrastructure/feed_techmeme.py` → Implement `FeedPort`
- [ ] `src/infrastructure/llm_gemini.py` → Implement `SummarizerPort`
- [ ] `src/infrastructure/storage_fs.py` → Implement `StoragePort`

**Thời gian:** ~3-4 giờ

---

#### Bước 4: **Refactor Presentation**
- [ ] Sửa `update_news.py` → Gọi use-case thay vì pipeline
- [ ] Tạo `src/presentation/api/v1/summarize_router.py`
- [ ] Sửa `src/api/app.py` → Register router

**Thời gian:** ~2-3 giờ

---

#### Bước 5: **Testing**
- [ ] Unit tests cho use-case (mock ports)
- [ ] Integration tests cho adapters
- [ ] Golden test (so sánh `summaries.json` output)

**Thời gian:** ~4-5 giờ

---

#### Bước 6: **Cleanup**
- [ ] Xóa code cũ (`src/services/`, `src/pipelines/`)
- [ ] Update docs

**Thời gian:** ~1-2 giờ

---

**Tổng thời gian:** ~15-20 giờ (2-3 ngày làm việc)

---

## 5. Folder Structure Chi Tiết

### 📁 Cấu Trúc Sau Khi Refactor

```
src/
├── domain/                     # ⭐ Domain Layer (Core)
│   ├── __init__.py
│   ├── models.py              # Article, Summary (data classes)
│   └── ports.py               # FeedPort, SummarizerPort, StoragePort (interfaces)
│
├── application/                # ⭐ Application Layer (Use-Cases)
│   ├── __init__.py
│   └── use_cases/
│       ├── __init__.py
│       └── summarize_uc.py    # SummarizeUC (orchestration logic)
│
├── infrastructure/             # ⭐ Infrastructure Layer (Adapters)
│   ├── __init__.py
│   ├── feed_techmeme.py       # TechmemeAdapter (implements FeedPort)
│   ├── llm_gemini.py          # GeminiAdapter (implements SummarizerPort)
│   └── storage_fs.py          # FileSystemAdapter (implements StoragePort)
│
├── presentation/               # ⭐ Presentation Layer
│   ├── __init__.py
│   ├── api/
│   │   ├── __init__.py
│   │   └── v1/
│   │       ├── __init__.py
│   │       └── summarize_router.py  # FastAPI router
│   └── middleware/
│       ├── __init__.py
│       ├── correlation.py     # Correlation ID middleware
│       └── access_log.py      # Logging middleware
│
├── clients/                    # (Giữ lại - dùng trong adapters)
│   └── gemini.py
│
├── feeds/                      # (Giữ lại - dùng trong adapters)
│   └── techmeme/
│       └── client.py
│
├── config/
│   └── settings.py
│
└── utils/
    └── json_tools.py
```

---

### 📊 So Sánh Before/After

| Aspect | Before (Hiện tại) | After (Mục tiêu) |
|--------|-------------------|------------------|
| **Testability** | Khó (phụ thuộc external API) | Dễ (mock ports) |
| **Extensibility** | Khó (sửa nhiều chỗ) | Dễ (chỉ thêm adapter) |
| **Maintainability** | Trung bình | Cao (tách biệt rõ ràng) |
| **Dependencies** | Tight coupling | Loose coupling (DI) |
| **Lines of Code** | ~800 lines | ~1200 lines (trade-off) |

---

## 🎓 Tổng Kết

### ✅ Lợi Ích Của Hexagonal Architecture

1. **Testability** → Dễ test với mocked ports
2. **Extensibility** → Thêm nguồn tin mới không cần sửa core logic
3. **Maintainability** → Code tách biệt rõ ràng, dễ đọc
4. **Flexibility** → Dễ swap adapters (ví dụ: JSON → SQLite)
5. **SOLID Principles** → Tuân theo best practices

---

### ⚠️ Trade-offs

1. **Complexity** → Nhiều file/folder hơn
2. **Learning Curve** → Junior dev cần thời gian hiểu
3. **Boilerplate** → Nhiều code boilerplate (interfaces, adapters)

---

### 💡 Khi Nào Nên Dùng Hexagonal?

**✅ Nên dùng khi:**
- Dự án dự kiến mở rộng nhiều (thêm nguồn tin, thay AI model)
- Team > 3 người (cần cấu trúc rõ ràng)
- Cần test coverage cao

**❌ Không cần khi:**
- Dự án nhỏ, prototype
- Chỉ 1 người maintain
- Không có kế hoạch mở rộng

---

## 📖 Đọc Tiếp

- **[ONBOARDING_GUIDE.md](ONBOARDING_GUIDE.md)** → Hiểu dự án tổng quan
- **[TASK_EXAMPLES.md](TASK_EXAMPLES.md)** → Ví dụ task refactoring
- **[CONTRIBUTION_GUIDE.md](CONTRIBUTION_GUIDE.md)** → Quy trình coding

---

**Chúc bạn refactor thành công! 🚀**
