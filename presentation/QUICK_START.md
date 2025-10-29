# ⚡ Quick Start - Chạy Dự Án Trong 5 Phút

> **Mục đích:** Hướng dẫn setup và chạy dự án nhanh nhất có thể, kèm troubleshooting.

**Thời gian:** 5-10 phút | **Cấp độ:** Beginner

---

## ✅ Checklist Trước Khi Bắt Đầu

Đảm bảo bạn đã cài:
- [ ] Python 3.10+ (`python --version`)
- [ ] Git (`git --version`)
- [ ] VS Code (hoặc editor khác)
- [ ] Internet connection (để gọi API)

---

## 🚀 Bước 1: Clone & Setup (2 phút)

### 1.1. Clone Repository

```bash
git clone https://github.com/toilathuc/Summarize-with-AI.git
cd Summarize-with-AI
```

### 1.2. Tạo Virtual Environment

```bash
# Tạo venv
python -m venv .venv

# Kích hoạt (Windows)
.venv\Scripts\activate

# Kiểm tra đã activate chưa → Prompt có (.venv)
# (.venv) PS C:\...\Summarize-with-AI>
```

### 1.3. Cài Dependencies

```bash
pip install -r requirements.txt
```

**Expected output:**
```
Successfully installed fastapi-0.109.0 uvicorn-0.27.0 ...
```

---

## 🔑 Bước 2: Setup API Key (1 phút)

### 2.1. Tạo file `.env`

**Copy từ template:**
```bash
copy .env.example .env
```

**Hoặc tạo thủ công:**
```bash
# File: .env
GEMINI_API_KEY=your_api_key_here
TECHMEME_RSS_URL=https://www.techmeme.com/feed.xml
PORT=8000
```

### 2.2. Lấy Gemini API Key

1. Truy cập: https://aistudio.google.com/apikey
2. Đăng nhập Google account
3. Click "Create API Key"
4. Copy key → Paste vào file `.env`

**File `.env` sau khi sửa:**
```env
GEMINI_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXX
TECHMEME_RSS_URL=https://www.techmeme.com/feed.xml
PORT=8000
```

---

## 📰 Bước 3: Lấy Data (1 phút)

### 3.1. Chạy Script Cập Nhật

```bash
python update_news.py --top 30
```

**Expected output:**
```
Fetching articles from Techmeme...
✓ Found 15 articles
Summarizing with Gemini AI...
✓ Summarized 15/15 articles
Saving to data/outputs/summaries.json...
✓ Saved successfully
```

**⏱️ Thời gian:** ~30-60 giây (tùy tốc độ Gemini API)

---

## 🌐 Bước 4: Khởi Động Server (30 giây)

### 4.1. Chạy Server

**Option A: Dùng batch file (Windows)**
```bash
start_fastapi.bat
```

**Option B: Chạy trực tiếp**
```bash
uvicorn src.api.app:app --reload --port 8000
```

**Expected output:**
```
INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
INFO:     Started reloader process
INFO:     Started server process
INFO:     Application startup complete
```

### 4.2. Mở Browser

```
http://localhost:8000
```

**✅ Bạn sẽ thấy:**
- Giao diện web với các bài viết đã tóm tắt
- Mỗi card có: Title, Summary, Source
- Filter buttons phía trên

---

## 🎉 Xong! Dự Án Đã Chạy

**Kiểm tra các tính năng:**

| Tính Năng | Cách Test |
|-----------|-----------|
| **Xem tin** | Scroll trang → Thấy 15 bài |
| **Filter** | Click "TechCrunch" → Chỉ hiển thị bài từ TC |
| **Open article** | Click title → Mở tab mới với bài gốc |
| **Refresh** | Click nút ↻ → Data reload |
| **Keyboard** | Press `R` → Refresh |

---

## 🐛 Troubleshooting

### Lỗi 1: `ModuleNotFoundError: No module named 'uvicorn'`

**Nguyên nhân:** Chưa cài packages hoặc chưa activate venv

**Fix:**
```bash
# Activate venv
.venv\Scripts\activate

# Cài lại packages
pip install -r requirements.txt
```

---

### Lỗi 2: `GEMINI_API_KEY not found`

**Nguyên nhân:** File `.env` không có hoặc key sai

**Fix:**
```bash
# Kiểm tra file .env tồn tại
ls .env

# Xem nội dung
cat .env

# Đảm bảo có dòng:
# GEMINI_API_KEY=AIzaSy...
```

---

### Lỗi 3: `[Errno 10048] Address already in use`

**Nguyên nhân:** Port 8000 đang được dùng bởi process khác

**Fix Option A: Kill process cũ**
```bash
# Windows
netstat -ano | findstr :8000
taskkill /PID <PID_number> /F
```

**Fix Option B: Dùng port khác**
```bash
uvicorn src.api.app:app --reload --port 8001
```

---

### Lỗi 4: `FileNotFoundError: data/outputs/summaries.json`

**Nguyên nhân:** Chưa chạy `update_news.py` để tạo data

**Fix:**
```bash
python update_news.py --top 30
```

---

### Lỗi 5: `API rate limit exceeded`

**Nguyên nhân:** Gemini API có giới hạn requests (free tier: 60 requests/minute)

**Fix:**
- Đợi 1 phút rồi chạy lại
- Hoặc giảm số bài: `python update_news.py --top 10`

---

### Lỗi 6: Browser hiển thị text lỗi tiếng Việt

**Nguyên nhân:** Browser cache file JavaScript cũ

**Fix:**
```bash
# Hard refresh
Ctrl + Shift + R (Windows)
Cmd + Shift + R (Mac)
```

---

### Lỗi 7: `ImportError: cannot import name 'app'`

**Nguyên nhân:** Đang ở sai thư mục hoặc thiếu `__init__.py`

**Fix:**
```bash
# Đảm bảo ở root folder
cd Summarize-with-AI

# Kiểm tra cấu trúc
ls src/api/app.py  # Phải tồn tại
```

---

## 📋 Commands Cheat Sheet

```bash
# ========================================
# SETUP (Chỉ làm 1 lần)
# ========================================
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt

# ========================================
# DAILY WORKFLOW
# ========================================

# 1. Activate venv (mỗi lần mở terminal mới)
.venv\Scripts\activate

# 2. Cập nhật tin (buổi sáng)
python update_news.py --top 30

# 3. Chạy server
start_fastapi.bat
# Hoặc:
uvicorn src.api.app:app --reload --port 8000

# 4. Mở browser
# http://localhost:8000

# ========================================
# TESTING
# ========================================

# Test API
curl http://localhost:8000/api/summaries

# Test health
curl http://localhost:8000/healthz

# ========================================
# CLEANUP
# ========================================

# Tắt server
Ctrl + C

# Deactivate venv
deactivate
```

---

## 🎯 Next Steps

Sau khi chạy thành công, đọc tiếp:

1. **[ONBOARDING_GUIDE.md](ONBOARDING_GUIDE.md)** → Hiểu dự án chi tiết
2. **[CODE_CONCEPTS.md](CODE_CONCEPTS.md)** → Học các khái niệm (FastAPI, async, JSON...)
3. **[TASK_EXAMPLES.md](TASK_EXAMPLES.md)** → Làm task đầu tiên (fix bug hoặc thêm feature)

---

## 💡 Tips

**Productivity Tips:**
- Dùng VS Code → Mở terminal ngay trong editor (`Ctrl + `` `)
- Cài extension "Python" cho VS Code → Intellisense
- Bookmark `http://localhost:8000` → Truy cập nhanh

**Development Tips:**
- Server chạy với `--reload` → Tự restart khi sửa code
- Xem logs trong terminal → Debug dễ hơn
- Dùng `Ctrl+C` để tắt server (không đóng terminal)

---

**Chúc bạn setup thành công! 🎉**

Nếu gặp lỗi không có trong troubleshooting, xem [`COMMON_PITFALLS.md`](COMMON_PITFALLS.md) hoặc hỏi team lead.
