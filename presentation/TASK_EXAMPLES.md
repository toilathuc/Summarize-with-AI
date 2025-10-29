# 📝 Task Examples - Ví Dụ Thực Tế

> **Mục đích:** Hướng dẫn từng bước cách fix bug và thêm tính năng, với code mẫu cụ thể.

**Thời gian đọc:** ~20 phút | **Cấp độ:** Junior Developer

---

## 📚 Mục Lục

1. [Task 1: Fix Bug - API Trả Về Data Cũ](#task-1-fix-bug---api-trả-về-data-cũ)
2. [Task 2: Thêm Tính Năng - Search/Filter](#task-2-thêm-tính-năng---searchfilter)
3. [Task 3: Thêm Nguồn Tin Mới - Hacker News](#task-3-thêm-nguồn-tin-mới---hacker-news)
4. [Task 4: Refactor - Implement Port](#task-4-refactor---implement-port)
5. [Task 5: Performance - Cache API Response](#task-5-performance---cache-api-response)

---

## Task 1: Fix Bug - API Trả Về Data Cũ

### 🐛 Mô Tả Bug

**User Report:**
> "Tôi đã chạy `python update_news.py` 10 phút trước, nhưng website vẫn hiển thị tin cũ từ sáng nay."

**Expected:** Data cập nhật sau khi chạy script  
**Actual:** Data vẫn cũ

---

### 🔍 Debug Steps

#### Bước 1: Kiểm tra file JSON

```bash
# Xem nội dung file summaries.json
cat data/outputs/summaries.json | head -20
```

**Tìm field `last_updated`:**
```json
{
  "last_updated": "2025-10-29T08:00:00+07:00",
  ...
}
```

**Kiểm tra:**
- Nếu timestamp cũ (8:00 sáng) → Script không chạy thành công
- Nếu timestamp mới (10 phút trước) → Bug ở API/Frontend

---

#### Bước 2: Kiểm tra script có chạy thành công không

```bash
# Chạy lại script với verbose output
python update_news.py --top 30
```

**Kết quả mong đợi:**
```
Fetching articles from Techmeme...
✓ Found 15 articles
Summarizing with Gemini AI...
✓ Summarized 15 articles
Saving to data/outputs/summaries.json...
✓ Saved successfully
```

**Nếu lỗi:**
- `ModuleNotFoundError` → Thiếu package (chạy `pip install -r requirements.txt`)
- `API Key error` → Kiểm tra file `.env`
- `Network error` → Kiểm tra internet

---

#### Bước 3: Kiểm tra API response

**Mở browser Developer Tools:**
1. Press `F12`
2. Tab "Network"
3. Reload trang (Ctrl+R)
4. Click vào request `/api/summaries`
5. Xem Response:

```json
{
  "summaries": [...],
  "freshness": "8 hours ago",  // ← Nếu > 1 giờ → data cũ
  "last_updated": "2025-10-29T08:00:00+07:00"
}
```

---

### 🛠️ Giải Pháp

#### Giải pháp 1: Server đang cache

**Nguyên nhân:** FastAPI cache response hoặc đọc file cũ

**Fix:**
```bash
# Restart server
# Ctrl+C trong terminal đang chạy uvicorn
# Sau đó chạy lại:
start_fastapi.bat
```

---

#### Giải pháp 2: Browser cache

**Nguyên nhân:** Browser cache file JavaScript cũ

**Fix:**
- Hard refresh: `Ctrl + Shift + R` (Windows)
- Hoặc xóa cache: `Ctrl + Shift + Delete`

---

#### Giải pháp 3: Script chạy sai file

**Nguyên nhân:** Có nhiều file `summaries.json` (test, backup)

**Kiểm tra:**
```python
# File: src/services/storage_service.py
class StorageService:
    OUTPUT_FILE = "data/outputs/summaries.json"  # ← Đúng path?
```

**Fix:** Đảm bảo path đúng, không có typo

---

#### Giải pháp 4: File permissions

**Nguyên nhân:** Script không có quyền ghi file

**Kiểm tra (Windows):**
```powershell
# Kiểm tra quyền
Get-Acl data\outputs\summaries.json
```

**Fix:**
- Right-click file → Properties → Security → Edit permissions

---

### ✅ Testing

**Sau khi fix, test lại:**

1. Chạy script:
   ```bash
   python update_news.py --top 5
   ```

2. Kiểm tra file:
   ```bash
   cat data/outputs/summaries.json | findstr "last_updated"
   ```
   → Timestamp phải là hiện tại (vài giây trước)

3. Reload browser (hard refresh)
4. Kiểm tra UI → Data mới xuất hiện ✅

---

### 📝 Commit Message

```
fix: API returning stale data

- Issue: Server was caching old summaries.json
- Root cause: Forgot to restart server after running update_news.py
- Solution: Added note in README to restart server after updates
- Tested: Data now updates correctly after script run

Closes #42
```

---

## Task 2: Thêm Tính Năng - Search/Filter

### 🎯 Yêu Cầu

**User Story:**
> "Là một user, tôi muốn search theo từ khóa (ví dụ: 'AI', 'blockchain') để tìm các bài viết liên quan nhanh chóng."

**Acceptance Criteria:**
- [ ] Có search box phía trên danh sách tin
- [ ] Gõ từ khóa → Filter real-time (không cần reload page)
- [ ] Search trong cả title và summary
- [ ] Case-insensitive (không phân biệt hoa/thường)
- [ ] Hiển thị số kết quả tìm được

---

### 🛠️ Implementation

#### Bước 1: Thêm Search Box (HTML)

**File: `news.html` (hoặc `public/index.html`)**

Tìm vị trí thích hợp (phía trên news container), thêm:

```html
<!-- Search Box -->
<div class="search-container">
    <input 
        type="text" 
        id="searchInput" 
        placeholder="Search articles (e.g., AI, blockchain)..."
        class="search-box"
    />
    <span id="searchResults" class="search-results-count"></span>
</div>

<!-- News Container (đã có) -->
<div id="news-container"></div>
```

---

#### Bước 2: Styling (CSS)

**File: `styles/filters.css` (hoặc tạo `styles/search.css` mới)**

```css
/* Search Container */
.search-container {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    padding: 0 16px;
}

/* Search Box */
.search-box {
    flex: 1;
    padding: 12px 16px;
    font-size: 16px;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    transition: border-color 0.3s;
}

.search-box:focus {
    outline: none;
    border-color: #4285f4;
}

/* Results Count */
.search-results-count {
    color: #666;
    font-size: 14px;
    white-space: nowrap;
}
```

---

#### Bước 3: Logic (JavaScript)

**File: `js/main.js`**

```javascript
// Lưu toàn bộ summaries (không filter)
let allSummaries = [];

// Hàm khởi tạo (đã có - sửa lại)
document.addEventListener('DOMContentLoaded', async () => {
    showLoading();
    
    const data = await newsService.fetchSummaries();
    allSummaries = data.summaries;  // ← Lưu vào biến global
    
    renderNews(allSummaries);
    hideLoading();
    
    // ⭐ Setup search
    setupSearch();
});

// ⭐ Hàm mới: Setup search functionality
function setupSearch() {
    const searchInput = document.getElementById('searchInput');
    const resultsCount = document.getElementById('searchResults');
    
    // Debounce: Chờ 300ms sau khi user ngừng gõ mới search
    let debounceTimer;
    
    searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        
        debounceTimer = setTimeout(() => {
            const keyword = e.target.value.trim().toLowerCase();
            
            if (keyword === '') {
                // Không có keyword → Hiển thị tất cả
                renderNews(allSummaries);
                resultsCount.textContent = '';
            } else {
                // Filter theo keyword
                const filtered = filterByKeyword(keyword);
                renderNews(filtered);
                
                // Hiển thị số kết quả
                resultsCount.textContent = `${filtered.length} results`;
            }
        }, 300);
    });
}

// ⭐ Hàm filter
function filterByKeyword(keyword) {
    return allSummaries.filter(article => {
        const title = article.title.toLowerCase();
        const summary = article.summary.toLowerCase();
        
        // Tìm trong cả title và summary
        return title.includes(keyword) || summary.includes(keyword);
    });
}
```

---

#### Bước 4: Testing

**Manual Test Cases:**

| Test Case | Input | Expected |
|-----------|-------|----------|
| TC1 | Gõ "AI" | Hiển thị các bài có "AI" trong title/summary |
| TC2 | Gõ "blockchain" | Filter đúng bài |
| TC3 | Gõ "xyz123" | Hiển thị "0 results" |
| TC4 | Xóa hết text | Hiển thị lại tất cả bài |
| TC5 | Gõ "ai" (lowercase) | Vẫn match "AI" (case-insensitive) ✅ |

---

#### Bước 5: Enhancement - Highlight Search Term

**Bonus feature:** Làm nổi bật từ khóa trong kết quả

```javascript
// File: js/ui/render.js

function createNewsCard(article, searchKeyword = '') {
    const card = document.createElement('div');
    card.className = 'news-card';
    
    // Highlight keyword nếu có
    let title = article.title;
    let summary = article.summary;
    
    if (searchKeyword) {
        const regex = new RegExp(`(${searchKeyword})`, 'gi');
        title = title.replace(regex, '<mark>$1</mark>');
        summary = summary.replace(regex, '<mark>$1</mark>');
    }
    
    card.innerHTML = `
        <h3><a href="${article.url}" target="_blank">${title}</a></h3>
        <p class="summary">${summary}</p>
        <div class="meta">
            <span class="source">${article.source}</span>
        </div>
    `;
    
    return card;
}
```

**CSS cho `<mark>`:**
```css
mark {
    background-color: #ffeb3b;
    padding: 2px 4px;
    border-radius: 2px;
}
```

---

### 📝 Commit Message

```
feat: add search functionality for articles

- Added search box in UI with real-time filtering
- Search in both title and summary (case-insensitive)
- Display results count
- Debounce input (300ms) for performance
- Bonus: Highlight search terms in results

Closes #15
```

---

## Task 3: Thêm Nguồn Tin Mới - Hacker News

### 🎯 Yêu Cầu

**User Story:**
> "Là một developer, tôi muốn lấy tin từ Hacker News ngoài Techmeme để có nhiều nguồn tin hơn."

**Acceptance Criteria:**
- [ ] Script hỗ trợ argument `--source` (techmeme/hackernews/all)
- [ ] Hacker News API hoạt động tương tự Techmeme
- [ ] Data có field `source` để phân biệt
- [ ] UI filter theo source

---

### 🛠️ Implementation

#### Bước 1: Tạo Hacker News Client

**File: `src/feeds/hackernews/client.py` (tạo mới)**

```python
"""
Hacker News Client - Lấy top stories từ HN API

API Documentation: https://github.com/HackerNews/API
"""

import requests
from typing import List, Dict
from datetime import datetime

class HackerNewsClient:
    BASE_URL = "https://hacker-news.firebaseio.com/v0"
    
    def fetch_feed(self, top_n: int = 30) -> List[Dict]:
        """
        Lấy top N stories từ Hacker News
        
        Returns:
            List[Dict]: Danh sách articles
                {
                    "title": str,
                    "url": str,
                    "content": str (empty - HN không có full content),
                    "source": "Hacker News",
                    "published_date": str (ISO format)
                }
        """
        # Bước 1: Lấy danh sách top story IDs
        top_stories_url = f"{self.BASE_URL}/topstories.json"
        response = requests.get(top_stories_url, timeout=10)
        response.raise_for_status()
        
        story_ids = response.json()[:top_n]  # Chỉ lấy top N
        
        # Bước 2: Fetch chi tiết từng story
        articles = []
        for story_id in story_ids:
            story = self._fetch_story(story_id)
            if story:
                articles.append(story)
        
        return articles
    
    def _fetch_story(self, story_id: int) -> Dict:
        """Lấy chi tiết 1 story"""
        url = f"{self.BASE_URL}/item/{story_id}.json"
        
        try:
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            data = response.json()
            
            # Convert sang format chuẩn
            return {
                "title": data.get("title", ""),
                "url": data.get("url", f"https://news.ycombinator.com/item?id={story_id}"),
                "content": "",  # HN không có full content
                "source": "Hacker News",
                "published_date": self._timestamp_to_iso(data.get("time"))
            }
        except Exception as e:
            print(f"Error fetching story {story_id}: {e}")
            return None
    
    def _timestamp_to_iso(self, timestamp: int) -> str:
        """Convert Unix timestamp sang ISO format"""
        if not timestamp:
            return datetime.now().isoformat()
        
        dt = datetime.fromtimestamp(timestamp)
        return dt.isoformat()
```

---

#### Bước 2: Sửa FeedService

**File: `src/services/feed_service.py`**

```python
from src.feeds.techmeme.client import TechmemeClient
from src.feeds.hackernews.client import HackerNewsClient  # ← Import mới

class FeedService:
    def fetch_latest_articles(self, source: str = "techmeme", top_n: int = 30):
        """
        Lấy articles từ nguồn chỉ định
        
        Args:
            source: "techmeme", "hackernews", hoặc "all"
            top_n: Số lượng bài
        """
        articles = []
        
        if source in ["techmeme", "all"]:
            # Lấy từ Techmeme
            techmeme = TechmemeClient()
            techmeme_articles = techmeme.fetch_feed()[:top_n]
            articles.extend(techmeme_articles)
        
        if source in ["hackernews", "all"]:
            # Lấy từ Hacker News
            hn = HackerNewsClient()
            hn_articles = hn.fetch_feed(top_n)
            articles.extend(hn_articles)
        
        # Nếu "all" → Giới hạn tổng số
        if source == "all":
            articles = articles[:top_n]
        
        return articles
```

---

#### Bước 3: Sửa update_news.py

**File: `update_news.py`**

```python
import argparse

def main():
    parser = argparse.ArgumentParser(description='Update tech news')
    parser.add_argument('--top', type=int, default=30, help='Number of articles')
    parser.add_argument('--source', type=str, default='techmeme',
                        choices=['techmeme', 'hackernews', 'all'],
                        help='News source')  # ← Argument mới
    
    args = parser.parse_args()
    
    # Chạy pipeline với source
    pipeline = NewsPipeline()
    summaries = pipeline.run(source=args.source, top_n=args.top)
    
    print(f"✓ Saved {len(summaries)} summaries from {args.source}")

if __name__ == "__main__":
    main()
```

**Sửa `NewsPipeline.run()`:**
```python
class NewsPipeline:
    def run(self, source: str = "techmeme", top_n: int = 30):
        # Fetch với source parameter
        articles = self.feed_service.fetch_latest_articles(
            source=source,
            top_n=top_n
        )
        
        # ... rest of code
```

---

#### Bước 4: Testing

**Test Commands:**

```bash
# Test Hacker News only
python update_news.py --source hackernews --top 10

# Test Techmeme only (default)
python update_news.py --source techmeme --top 10

# Test cả hai
python update_news.py --source all --top 20
```

**Kiểm tra output:**
```bash
cat data/outputs/summaries.json | findstr "source"
```

**Expected:**
```json
{
  "summaries": [
    {"title": "...", "source": "Hacker News"},
    {"title": "...", "source": "TechCrunch"},
    ...
  ]
}
```

---

### 📝 Commit Message

```
feat: add Hacker News as news source

- Created HackerNewsClient to fetch from HN API
- Added --source argument to update_news.py
- Supports: techmeme, hackernews, or all
- Updated FeedService to handle multiple sources

Closes #23
```

---

## Task 4: Refactor - Implement Port

### 🎯 Mục Đích

Refactor `FeedService` theo **Hexagonal Architecture** (Port & Adapter pattern)

**Benefit:**
- ✅ Dễ test (mock port)
- ✅ Dễ mở rộng (thêm nguồn mới không sửa core logic)

---

### 🛠️ Implementation

#### Bước 1: Tạo Port (Interface)

**File: `src/domain/ports.py` (tạo mới)**

```python
"""
Domain Ports - Interfaces cho external dependencies
"""

from abc import ABC, abstractmethod
from typing import List
from src.domain.models import Article

class FeedPort(ABC):
    """
    Interface để lấy tin từ nguồn bên ngoài
    
    Implementers: TechmemeAdapter, HackerNewsAdapter
    """
    
    @abstractmethod
    def fetch_latest_articles(self, top_n: int) -> List[Article]:
        """
        Lấy top N bài viết mới nhất
        
        Args:
            top_n: Số lượng bài muốn lấy
            
        Returns:
            List[Article]: Danh sách articles (domain model)
        """
        pass
```

---

#### Bước 2: Tạo Domain Model

**File: `src/domain/models.py` (tạo mới)**

```python
"""
Domain Models - Data classes thuần túy (không phụ thuộc framework)
"""

from dataclasses import dataclass
from datetime import datetime

@dataclass
class Article:
    """Đại diện cho 1 bài viết tin tức"""
    title: str
    url: str
    content: str
    source: str
    published_date: str
    
    def __post_init__(self):
        """Validation"""
        if not self.title:
            raise ValueError("Title cannot be empty")
        if not self.url:
            raise ValueError("URL cannot be empty")
```

---

#### Bước 3: Tạo Adapter

**File: `src/infrastructure/feed_techmeme.py` (tạo mới)**

```python
"""
Techmeme Adapter - Implement FeedPort
"""

from typing import List
from src.domain.ports import FeedPort
from src.domain.models import Article
from src.feeds.techmeme.client import TechmemeClient

class TechmemeAdapter(FeedPort):
    """Adapter cho Techmeme RSS feed"""
    
    def __init__(self):
        self.client = TechmemeClient()
    
    def fetch_latest_articles(self, top_n: int) -> List[Article]:
        # Gọi client cũ
        raw_data = self.client.fetch_feed()
        
        # Convert sang Domain Model
        articles = []
        for item in raw_data[:top_n]:
            article = Article(
                title=item.get('title', ''),
                url=item.get('url', ''),
                content=item.get('content', ''),
                source='Techmeme',
                published_date=item.get('published', '')
            )
            articles.append(article)
        
        return articles
```

---

#### Bước 4: Sử Dụng Trong Use-Case

**File: `src/application/use_cases/summarize_uc.py` (tạo mới)**

```python
"""
Summarize Use-Case - Orchestration logic
"""

from typing import List
from src.domain.ports import FeedPort, SummarizerPort, StoragePort
from src.domain.models import Summary

class SummarizeUC:
    """
    Use-case: Lấy tin → Tóm tắt → Lưu
    
    Dependency Injection: Nhận ports (interfaces), không phải concrete classes
    """
    
    def __init__(
        self,
        feed_port: FeedPort,
        summarizer_port: SummarizerPort,
        storage_port: StoragePort
    ):
        self.feed_port = feed_port
        self.summarizer_port = summarizer_port
        self.storage_port = storage_port
    
    def execute(self, source: str, top_n: int) -> List[Summary]:
        """
        Chạy workflow
        
        Args:
            source: Nguồn tin (để logging)
            top_n: Số bài
            
        Returns:
            List[Summary]: Kết quả đã tóm tắt
        """
        # Bước 1: Fetch
        articles = self.feed_port.fetch_latest_articles(top_n)
        
        # Bước 2: Summarize
        summaries = []
        for article in articles:
            summary_text = self.summarizer_port.summarize(article.content)
            
            summary = Summary(
                title=article.title,
                url=article.url,
                summary=summary_text,
                source=article.source,
                published_date=article.published_date
            )
            summaries.append(summary)
        
        # Bước 3: Save
        self.storage_port.save_summaries(summaries)
        
        return summaries
```

---

#### Bước 5: Wire Up (Dependency Injection)

**File: `update_news.py` (refactored)**

```python
from src.application.use_cases.summarize_uc import SummarizeUC
from src.infrastructure.feed_techmeme import TechmemeAdapter
from src.infrastructure.llm_gemini import GeminiAdapter
from src.infrastructure.storage_fs import FileSystemAdapter

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--top', type=int, default=30)
    args = parser.parse_args()
    
    # Dependency Injection - tạo adapters
    feed_adapter = TechmemeAdapter()
    summarizer_adapter = GeminiAdapter()
    storage_adapter = FileSystemAdapter()
    
    # Tạo use-case với dependencies
    uc = SummarizeUC(
        feed_port=feed_adapter,
        summarizer_port=summarizer_adapter,
        storage_port=storage_adapter
    )
    
    # Chạy
    summaries = uc.execute(source="techmeme", top_n=args.top)
    print(f"✓ Saved {len(summaries)} summaries")
```

---

#### Bước 6: Unit Test

**File: `test/test_summarize_uc.py` (tạo mới)**

```python
"""
Unit test cho SummarizeUC với mocked ports
"""

import pytest
from unittest.mock import Mock
from src.application.use_cases.summarize_uc import SummarizeUC
from src.domain.models import Article

def test_summarize_uc_success():
    # Arrange: Tạo mock ports
    feed_port = Mock()
    summarizer_port = Mock()
    storage_port = Mock()
    
    # Mock data
    mock_articles = [
        Article(
            title="AI Breakthrough",
            url="https://example.com",
            content="Full article content...",
            source="Techmeme",
            published_date="2025-10-29T10:00:00Z"
        )
    ]
    feed_port.fetch_latest_articles.return_value = mock_articles
    summarizer_port.summarize.return_value = "AI has made significant progress."
    
    # Act: Chạy use-case
    uc = SummarizeUC(feed_port, summarizer_port, storage_port)
    summaries = uc.execute(source="techmeme", top_n=10)
    
    # Assert
    assert len(summaries) == 1
    assert summaries[0].title == "AI Breakthrough"
    assert summaries[0].summary == "AI has made significant progress."
    
    # Verify mocks được gọi
    feed_port.fetch_latest_articles.assert_called_once_with(10)
    summarizer_port.summarize.assert_called_once()
    storage_port.save_summaries.assert_called_once()
```

**Chạy test:**
```bash
pytest test/test_summarize_uc.py -v
```

---

### 📝 Commit Message

```
refactor: implement hexagonal architecture for feed service

- Created FeedPort interface in domain layer
- Implemented TechmemeAdapter in infrastructure
- Created SummarizeUC use-case with dependency injection
- Added unit tests with mocked ports
- Updated update_news.py to use new architecture

Breaking change: Old FeedService deprecated
Migration guide in docs/ARCHITECTURE_EXPLAINED.md

Closes #45
```

---

## Task 5: Performance - Cache API Response

### 🎯 Vấn Đề

**Observation:** Mỗi lần user refresh page, server đọc lại file `summaries.json` (I/O operation).

**Nếu có 100 users cùng lúc → 100 lần đọc file → Chậm!**

---

### 💡 Giải Pháp: In-Memory Cache

Cache data trong RAM (memory), chỉ reload khi file thay đổi.

---

### 🛠️ Implementation

**File: `src/api/app.py`**

```python
from fastapi import FastAPI
from fastapi.responses import JSONResponse
import json
from datetime import datetime
from pathlib import Path

app = FastAPI()

# ⭐ Cache global
_cache = {
    "data": None,
    "last_modified": None
}

SUMMARIES_FILE = Path("data/outputs/summaries.json")

def get_summaries_cached():
    """
    Đọc summaries với caching
    
    Logic:
    - Nếu file không thay đổi → Trả cache
    - Nếu file mới → Đọc lại và update cache
    """
    global _cache
    
    # Lấy timestamp file
    file_mtime = SUMMARIES_FILE.stat().st_mtime
    
    # Kiểm tra cache
    if _cache["data"] and _cache["last_modified"] == file_mtime:
        # Cache hit!
        print("✓ Cache hit")
        return _cache["data"]
    
    # Cache miss → Đọc file
    print("✗ Cache miss - Reading file")
    with open(SUMMARIES_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Update cache
    _cache["data"] = data
    _cache["last_modified"] = file_mtime
    
    return data

@app.get("/api/summaries")
async def get_summaries():
    """API endpoint với cache"""
    data = get_summaries_cached()  # ← Dùng cached version
    
    return JSONResponse({
        "summaries": data["summaries"],
        "freshness": calculate_age(data["last_updated"]),
        "total": len(data["summaries"]),
        "cached": _cache["last_modified"] is not None  # Debug info
    })
```

---

### 📊 Performance Test

**Before (No Cache):**
```bash
# Gọi API 100 lần
for i in {1..100}; do curl http://localhost:8000/api/summaries > /dev/null; done

# Thời gian: ~5 giây (50ms/request)
```

**After (With Cache):**
```bash
# Gọi API 100 lần
for i in {1..100}; do curl http://localhost:8000/api/summaries > /dev/null; done

# Thời gian: ~0.5 giây (5ms/request)
# → Nhanh hơn 10x! 🚀
```

---

### 📝 Commit Message

```
perf: add in-memory cache for summaries API

- Cache summaries.json in memory to avoid repeated file reads
- Invalidate cache when file modified (check mtime)
- Reduces response time from ~50ms to ~5ms (10x faster)
- Added cache hit/miss logging for debugging

Closes #67
```

---

## 🎓 Tổng Kết

Sau khi làm 5 tasks này, bạn đã học được:

✅ **Debug skills:** Tìm root cause của bug  
✅ **Feature development:** Thêm search/filter  
✅ **Integration:** Thêm nguồn data mới (Hacker News)  
✅ **Refactoring:** Implement design pattern (Hexagonal)  
✅ **Performance:** Optimize với caching  

---

## 📖 Đọc Tiếp

- **[ONBOARDING_GUIDE.md](ONBOARDING_GUIDE.md)** → Hiểu dự án tổng quan
- **[COMMON_PITFALLS.md](COMMON_PITFALLS.md)** → Lỗi thường gặp
- **[CONTRIBUTION_GUIDE.md](CONTRIBUTION_GUIDE.md)** → Quy trình coding

---

**Chúc bạn coding vui vẻ! 🚀**
