# 🤝 Contribution Guide - Quy Trình Làm Việc

> **Mục đích:** Hướng dẫn quy trình coding, Git workflow, code review để làm việc nhóm hiệu quả.

**Thời gian đọc:** ~15 phút | **Cấp độ:** Junior to Mid-Level

---

## 📚 Mục Lục

1. [Git Workflow](#1-git-workflow)
2. [Code Style Guidelines](#2-code-style-guidelines)
3. [Testing Guidelines](#3-testing-guidelines)
4. [Code Review Process](#4-code-review-process)
5. [Documentation Standards](#5-documentation-standards)
6. [Common Mistakes](#6-common-mistakes)

---

## 1. Git Workflow

### 🌳 Branching Strategy

**Main branches:**
- `main` → Production code (luôn stable)
- `develop` → Development code (integration branch)

**Feature branches:**
- `feature/feature-name` → Tính năng mới
- `bugfix/bug-description` → Fix bug
- `refactor/component-name` → Refactoring

---

### 📋 Workflow Steps

#### Bước 1: Tạo Branch Mới

```bash
# Đảm bảo đang ở main/develop và đã update
git checkout main
git pull origin main

# Tạo branch mới
git checkout -b feature/add-search-functionality

# Hoặc bugfix
git checkout -b bugfix/fix-api-cache-issue
```

**Naming convention:**
```
feature/add-hacker-news-source
bugfix/fix-empty-summary
refactor/implement-hexagonal-arch
docs/update-readme
chore/upgrade-dependencies
```

---

#### Bước 2: Code & Commit

**Commit frequency:** Commit thường xuyên (mỗi khi hoàn thành 1 subtask nhỏ)

```bash
# Xem thay đổi
git status

# Add files
git add src/services/feed_service.py
git add test/test_feed_service.py

# Commit với message rõ ràng
git commit -m "feat: add search functionality for articles"
```

**Commit message format (Conventional Commits):**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: Tính năng mới
- `fix`: Fix bug
- `refactor`: Refactor code (không thay đổi functionality)
- `docs`: Update documentation
- `test`: Thêm/sửa tests
- `chore`: Maintenance (upgrade packages, config...)
- `perf`: Performance improvement
- `style`: Code formatting (không ảnh hưởng logic)

**Ví dụ:**
```bash
# Simple
git commit -m "feat: add search box in UI"

# With body
git commit -m "fix: API returning stale data

- Issue: Server was caching old summaries.json
- Root cause: Forgot to restart server
- Solution: Added cache invalidation on file change

Closes #42"
```

---

#### Bước 3: Push Branch

```bash
# Push lần đầu
git push -u origin feature/add-search-functionality

# Push lần sau
git push
```

---

#### Bước 4: Tạo Pull Request (PR)

**Trên GitHub:**
1. Vào repository → Tab "Pull Requests"
2. Click "New Pull Request"
3. Base: `main` (hoặc `develop`) ← Compare: `feature/add-search-functionality`
4. Fill PR template:

```markdown
## 📝 Description
Thêm tính năng search để user tìm bài viết theo keyword.

## 🎯 Changes
- Added search box in `news.html`
- Implemented `filterByKeyword()` in `js/main.js`
- Added CSS styling in `styles/search.css`

## ✅ Testing
- [x] Manual test: Search "AI" → Correct results
- [x] Edge case: Empty keyword → Show all articles
- [x] Performance: Debounce 300ms

## 📸 Screenshots
(Attach screenshots if UI change)

## 🔗 Related Issues
Closes #15
```

5. Request reviewers (team lead)
6. Submit PR

---

#### Bước 5: Code Review & Merge

**Review process:**
1. Reviewer sẽ comment trên PR
2. Nếu có yêu cầu sửa → Fix và push thêm commits
3. Sau khi approve → Merge vào `main`

**Merge options:**
- **Squash and merge** (recommended) → Gộp tất cả commits thành 1
- Merge commit → Giữ nguyên history
- Rebase and merge → Linear history

---

### 🔄 Sync Với Main Branch

**Trường hợp:** Bạn đang code feature, nhưng có người khác merge PR vào `main` trước.

**Solution: Rebase**
```bash
# Đang ở feature branch
git fetch origin

# Rebase lên main mới nhất
git rebase origin/main

# Nếu có conflict → Resolve conflicts
# Sau đó:
git add .
git rebase --continue

# Force push (vì history đã thay đổi)
git push --force-with-lease
```

---

## 2. Code Style Guidelines

### 🐍 Python Style (PEP 8)

**Formatting tool:** `black` (auto-format)

**Installation:**
```bash
pip install black flake8
```

**Usage:**
```bash
# Format code
black src/

# Check code style
flake8 src/
```

---

### 📏 Naming Conventions

| Element | Style | Example |
|---------|-------|---------|
| **File** | snake_case | `feed_service.py` |
| **Class** | PascalCase | `FeedService`, `Article` |
| **Function** | snake_case | `fetch_latest_articles()` |
| **Variable** | snake_case | `summaries`, `top_n` |
| **Constant** | UPPER_CASE | `API_KEY`, `MAX_RETRIES` |
| **Private** | _leading_underscore | `_internal_method()` |

---

### 💬 Comments & Docstrings

**Good docstring:**
```python
def fetch_latest_articles(self, source: str, top_n: int) -> List[Article]:
    """
    Lấy top N bài viết mới nhất từ nguồn chỉ định.
    
    Args:
        source: Nguồn tin ("techmeme", "hackernews", "all")
        top_n: Số lượng bài muốn lấy
        
    Returns:
        List[Article]: Danh sách articles (domain models)
        
    Raises:
        ValueError: Nếu source không hợp lệ
        RequestException: Nếu API call failed
        
    Example:
        >>> service = FeedService()
        >>> articles = service.fetch_latest_articles("techmeme", 10)
        >>> len(articles)
        10
    """
    pass
```

**When to comment (Vietnamese OK for complex logic):**
```python
# ✅ GOOD - Giải thích WHY
# Debounce để tránh gọi API quá nhiều lần khi user gõ nhanh
debounce_timer = 300

# ❌ BAD - Chỉ mô tả WHAT (code đã rõ rồi)
# Tạo biến i
i = 0
```

---

### 🎨 JavaScript Style

**Naming:**
```javascript
// Function: camelCase
function fetchSummaries() { }

// Variable: camelCase
const allSummaries = [];

// Constant: UPPER_CASE
const API_BASE_URL = 'http://localhost:8000';

// Class: PascalCase
class NewsService { }
```

**Modern syntax:**
```javascript
// ✅ GOOD - Use const/let, not var
const data = await fetch('/api/summaries');

// ✅ GOOD - Arrow functions
const filtered = articles.filter(a => a.source === 'TechCrunch');

// ✅ GOOD - Template literals
console.log(`Found ${articles.length} articles`);

// ❌ BAD - var, old syntax
var data = ...;
```

---

## 3. Testing Guidelines

### 🧪 Test Structure

**File naming:**
```
test/
├── test_feed_service.py
├── test_summarization_service.py
└── test_api_endpoints.py
```

**Test function naming:**
```python
def test_fetch_articles_success():
    """Test happy path"""
    pass

def test_fetch_articles_empty_response():
    """Test edge case: API trả về rỗng"""
    pass

def test_fetch_articles_network_error():
    """Test error case: Network fail"""
    pass
```

---

### ✅ Unit Test Example

```python
import pytest
from unittest.mock import Mock, patch
from src.services.feed_service import FeedService

class TestFeedService:
    def setup_method(self):
        """Setup trước mỗi test"""
        self.service = FeedService()
    
    def test_fetch_articles_success(self):
        """Test lấy articles thành công"""
        # Arrange
        mock_client = Mock()
        mock_client.fetch_feed.return_value = [
            {'title': 'Article 1', 'url': 'http://...'},
            {'title': 'Article 2', 'url': 'http://...'}
        ]
        
        # Act
        with patch.object(self.service, 'client', mock_client):
            articles = self.service.fetch_latest_articles(top_n=2)
        
        # Assert
        assert len(articles) == 2
        assert articles[0]['title'] == 'Article 1'
        mock_client.fetch_feed.assert_called_once()
    
    def test_fetch_articles_with_invalid_top_n(self):
        """Test với top_n không hợp lệ"""
        with pytest.raises(ValueError):
            self.service.fetch_latest_articles(top_n=-1)
```

**Chạy tests:**
```bash
# Chạy tất cả
pytest

# Chạy 1 file
pytest test/test_feed_service.py

# Chạy 1 test cụ thể
pytest test/test_feed_service.py::TestFeedService::test_fetch_articles_success

# Với coverage
pytest --cov=src --cov-report=html
```

---

### 🎯 Test Coverage Goal

**Target:** 80% coverage cho core logic

**Check coverage:**
```bash
pytest --cov=src --cov-report=term-missing
```

**Output:**
```
Name                              Stmts   Miss  Cover   Missing
---------------------------------------------------------------
src/services/feed_service.py         45      9    80%   12-15, 34-38
```

---

## 4. Code Review Process

### 👀 Reviewer Checklist

**Functionality:**
- [ ] Code chạy đúng, không có lỗi
- [ ] Test cases pass
- [ ] Edge cases được handle

**Code Quality:**
- [ ] Tên biến/function rõ ràng
- [ ] Không có code duplicate
- [ ] Function nhỏ gọn (< 50 lines)
- [ ] Có docstrings cho public functions

**Security:**
- [ ] Không hardcode secrets (API key, password)
- [ ] Input validation đầy đủ
- [ ] Không có SQL injection risk (nếu dùng DB)

**Performance:**
- [ ] Không có unnecessary loops
- [ ] API calls được optimize (batching, caching)

**Documentation:**
- [ ] README updated (nếu có breaking change)
- [ ] CHANGELOG updated

---

### 💬 Comment Examples

**Good comments:**
```markdown
# ✅ Constructive
Suggestion: Consider using a `try-except` here to handle network errors.

# ✅ Specific
Line 45: This variable name `x` is unclear. Maybe rename to `article_count`?

# ✅ Praise good code
Nice refactoring! This is much cleaner than before. 👍
```

**Bad comments:**
```markdown
# ❌ Vague
This looks wrong.

# ❌ Rude
This is bad code. Rewrite it.
```

---

### 🔧 Responding to Review

**Nếu agree with comment:**
```bash
# Fix code
git add src/services/feed_service.py
git commit -m "refactor: rename variable x to article_count"
git push
```

**Nếu cần discuss:**
```markdown
# Reply trên PR
Thanks for the feedback! I used `x` here because... 
Do you think we should also consider...?
```

---

## 5. Documentation Standards

### 📖 README.md Structure

```markdown
# Project Title

## Description
Ngắn gọn (2-3 câu)

## Features
- Feature 1
- Feature 2

## Installation
```bash
...
```

## Usage
```bash
...
```

## Configuration
Environment variables...

## Contributing
See CONTRIBUTION_GUIDE.md

## License
MIT
```

---

### 📝 Code Comments (Hybrid: Vietnamese + English)

**Nguyên tắc:**
- Code, function names, variables → **English**
- Comments giải thích logic phức tạp → **Vietnamese** (dễ hiểu hơn cho junior)
- Docstrings → **Vietnamese hoặc English** (tuỳ team)

**Ví dụ:**
```python
def fetch_latest_articles(self, top_n: int) -> List[Article]:
    """
    Lấy top N bài viết mới nhất từ Techmeme.
    (Get top N latest articles from Techmeme)
    """
    # Gọi API Techmeme để lấy RSS feed
    raw_feed = self.client.fetch_feed()
    
    # Filter: Chỉ lấy các bài có đầy đủ title và URL
    # (Một số bài trong RSS có thể thiếu field)
    valid_articles = [
        article for article in raw_feed
        if article.get('title') and article.get('url')
    ]
    
    return valid_articles[:top_n]
```

---

## 6. Common Mistakes

### ❌ Mistake 1: Commit trực tiếp vào `main`

**BAD:**
```bash
git checkout main
# ... code ...
git add .
git commit -m "fix bug"
git push origin main  # ← Push trực tiếp!
```

**GOOD:**
```bash
git checkout -b bugfix/fix-issue-42
# ... code ...
git commit -m "fix: ..."
git push origin bugfix/fix-issue-42
# → Tạo PR để review
```

---

### ❌ Mistake 2: Commit message không rõ ràng

**BAD:**
```bash
git commit -m "fix"
git commit -m "update"
git commit -m "asd"
```

**GOOD:**
```bash
git commit -m "fix: API returning empty summaries when file is missing"
git commit -m "feat: add Hacker News as news source"
```

---

### ❌ Mistake 3: Không test trước khi commit

**BAD:**
```bash
# Sửa code
git add .
git commit -m "feat: add feature"
git push
# → Sau đó mới test → Phát hiện lỗi → Phải commit thêm "fix typo"
```

**GOOD:**
```bash
# Sửa code
python update_news.py  # Test trước!
pytest  # Chạy tests
git add .
git commit -m "feat: ..."
git push
```

---

### ❌ Mistake 4: PR quá lớn

**BAD:**
- 1 PR có 50 files changed, 2000+ lines
- Reviewer mất nhiều giờ để review
- Khó tìm bug

**GOOD:**
- Chia thành nhiều PRs nhỏ
- Mỗi PR làm 1 việc cụ thể
- Ví dụ:
  - PR #1: Tạo Port interfaces
  - PR #2: Implement Techmeme adapter
  - PR #3: Update use-case

---

### ❌ Mistake 5: Không update từ main

**Scenario:**
- Bạn tạo branch từ main (commit A)
- 2 ngày sau, main đã có commit B, C (người khác merge)
- Bạn tạo PR → Conflict!

**Solution:**
```bash
# Thường xuyên sync với main
git checkout feature/my-feature
git fetch origin
git rebase origin/main
# Resolve conflicts nếu có
git push --force-with-lease
```

---

## 🎓 Tổng Kết

**Quy trình chuẩn:**
1. ✅ Tạo branch từ `main`
2. ✅ Code + Commit thường xuyên (clear messages)
3. ✅ Test local
4. ✅ Push branch
5. ✅ Tạo PR (fill template)
6. ✅ Address review comments
7. ✅ Merge sau khi approved

**Best Practices:**
- Commit nhỏ, thường xuyên
- Test trước khi commit
- PR nhỏ gọn (< 500 lines)
- Review code của người khác (học hỏi)

---

## 📖 Đọc Tiếp

- **[TASK_EXAMPLES.md](TASK_EXAMPLES.md)** → Ví dụ workflow thực tế
- **[COMMON_PITFALLS.md](COMMON_PITFALLS.md)** → Lỗi thường gặp
- **[ONBOARDING_GUIDE.md](ONBOARDING_GUIDE.md)** → Hiểu dự án

---

**Chúc bạn contribute thành công! 🚀**
