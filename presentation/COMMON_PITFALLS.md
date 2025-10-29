# ⚠️ Common Pitfalls - Lỗi Thường Gặp & Cách Fix

> **Mục đích:** Tổng hợp các lỗi phổ biến khi phát triển dự án, kèm giải pháp chi tiết.

**Cấp độ:** Junior to Mid-Level

---

## 📚 Mục Lục

1. [Setup & Environment](#1-setup--environment)
2. [Python & Dependencies](#2-python--dependencies)
3. [API & External Services](#3-api--external-services)
4. [Data & File Operations](#4-data--file-operations)
5. [Frontend & Browser](#5-frontend--browser)
6. [Server & Networking](#6-server--networking)
7. [Git & Version Control](#7-git--version-control)

---

## 1. Setup & Environment

### ❌ Lỗi: `python: command not found`

**Nguyên nhân:** Python chưa được cài hoặc chưa thêm vào PATH

**Kiểm tra:**
```bash
python --version
# Hoặc
python3 --version
```

**Fix (Windows):**
1. Download Python từ: https://www.python.org/downloads/
2. Khi cài, **tích** ô "Add Python to PATH"
3. Restart terminal

**Fix (Ubuntu/Linux):**
```bash
sudo apt update
sudo apt install python3 python3-pip python3-venv
```

---

### ❌ Lỗi: Virtual environment không activate được

**Triệu chứng:**
```bash
.venv\Scripts\activate
# → Không có (venv) trong prompt
```

**Nguyên nhân (Windows):** PowerShell execution policy

**Fix:**
```powershell
# Chạy PowerShell as Administrator
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Sau đó activate lại
.venv\Scripts\activate
```

**Alternative: Dùng Command Prompt thay vì PowerShell**
```bash
# CMD (không phải PowerShell)
.venv\Scripts\activate.bat
```

---

### ❌ Lỗi: `pip: command not found`

**Nguyên nhân:** pip chưa cài hoặc không trong PATH

**Fix:**
```bash
# Kiểm tra pip có sẵn trong Python không
python -m pip --version

# Nếu có → Dùng `python -m pip` thay vì `pip`
python -m pip install -r requirements.txt
```

---

## 2. Python & Dependencies

### ❌ Lỗi: `ModuleNotFoundError: No module named 'X'`

**Ví dụ:**
```
ModuleNotFoundError: No module named 'fastapi'
ModuleNotFoundError: No module named 'uvicorn'
```

**Nguyên nhân:** Package chưa cài hoặc cài sai venv

**Fix:**
```bash
# Bước 1: Đảm bảo đã activate venv
.venv\Scripts\activate

# Bước 2: Kiểm tra venv có đúng không
where python
# Expected: C:\...\Summarize-with-AI\.venv\Scripts\python.exe

# Bước 3: Cài lại packages
pip install -r requirements.txt

# Bước 4: Verify
pip list | findstr fastapi
```

---

### ❌ Lỗi: `ImportError: cannot import name 'app' from 'src.api'`

**Nguyên nhân:** Python không tìm thấy module do thiếu `__init__.py` hoặc sai cấu trúc

**Kiểm tra cấu trúc:**
```
src/
├── __init__.py          ← Phải có!
├── api/
│   ├── __init__.py      ← Phải có!
│   └── app.py
```

**Fix:**
```bash
# Tạo __init__.py nếu thiếu
touch src/__init__.py
touch src/api/__init__.py

# Hoặc Windows:
echo. > src\__init__.py
echo. > src\api\__init__.py
```

---

### ❌ Lỗi: `TypeError: 'NoneType' object is not subscriptable`

**Ví dụ code lỗi:**
```python
data = json.load(f)
print(data['summaries'])  # ← Lỗi nếu data = None
```

**Nguyên nhân:** File JSON rỗng hoặc format sai

**Fix: Defensive coding**
```python
data = json.load(f)

# Kiểm tra trước khi dùng
if data is None or 'summaries' not in data:
    print("Error: Invalid JSON format")
    return []

summaries = data['summaries']
```

---

## 3. API & External Services

### ❌ Lỗi: `google.api_core.exceptions.PermissionDenied: 403 API key not valid`

**Nguyên nhân:** Gemini API key sai hoặc hết hạn

**Fix:**
```bash
# Bước 1: Kiểm tra file .env
cat .env
# → Xem GEMINI_API_KEY có đúng không

# Bước 2: Tạo key mới
# https://aistudio.google.com/apikey

# Bước 3: Update .env
GEMINI_API_KEY=AIzaSy_NEW_KEY_HERE

# Bước 4: Restart server
```

---

### ❌ Lỗi: `requests.exceptions.ConnectionError: Failed to establish connection`

**Nguyên nhân:** 
- Không có internet
- URL sai
- Service bên ngoài down (Techmeme, Gemini)

**Debug:**
```bash
# Test internet
ping google.com

# Test URL trực tiếp
curl https://www.techmeme.com/feed.xml
```

**Fix:**
```python
# Thêm retry logic
import time
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

def create_session_with_retry():
    session = requests.Session()
    retry = Retry(
        total=3,
        backoff_factor=1,  # Chờ 1s, 2s, 4s
        status_forcelist=[500, 502, 503, 504]
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount('http://', adapter)
    session.mount('https://', adapter)
    return session

# Dùng session này thay vì requests.get()
session = create_session_with_retry()
response = session.get(url)
```

---

### ❌ Lỗi: `google.api_core.exceptions.ResourceExhausted: 429 Quota exceeded`

**Nguyên nhân:** Gemini API free tier có giới hạn:
- 60 requests/minute
- 1500 requests/day

**Fix Option A: Đợi**
```bash
# Đợi 1 phút, chạy lại
sleep 60
python update_news.py --top 10
```

**Fix Option B: Batch processing**
```python
# File: src/services/summarization_service.py

def summarize_batch(self, articles: List[Article]):
    summaries = []
    for i, article in enumerate(articles):
        # Summarize
        summary = self.gemini_client.summarize(article.content)
        summaries.append(summary)
        
        # ⭐ Rate limiting: Đợi 1 giây giữa các request
        if (i + 1) % 10 == 0:  # Mỗi 10 bài
            time.sleep(1)
    
    return summaries
```

**Fix Option C: Upgrade API tier** (nếu production)

---

## 4. Data & File Operations

### ❌ Lỗi: `FileNotFoundError: [Errno 2] No such file or directory: 'data/outputs/summaries.json'`

**Nguyên nhân:** Chưa chạy `update_news.py` để tạo file

**Fix:**
```bash
# Tạo folder nếu chưa có
mkdir -p data/outputs

# Chạy script
python update_news.py --top 30
```

---

### ❌ Lỗi: `json.decoder.JSONDecodeError: Expecting value`

**Nguyên nhân:** File JSON bị corrupt hoặc rỗng

**Debug:**
```bash
# Xem nội dung file
cat data/outputs/summaries.json

# Kiểm tra syntax
python -m json.tool data/outputs/summaries.json
```

**Fix:**
```bash
# Backup file cũ
copy data\outputs\summaries.json data\outputs\summaries.json.backup

# Chạy lại script để tạo file mới
python update_news.py --top 30
```

---

### ❌ Lỗi: `UnicodeDecodeError: 'charmap' codec can't decode byte`

**Nguyên nhân:** File có ký tự Unicode (tiếng Việt, emoji) nhưng mở sai encoding

**Fix:**
```python
# ❌ BAD - Không chỉ định encoding
with open('data.json', 'r') as f:
    data = json.load(f)

# ✅ GOOD - Luôn dùng utf-8
with open('data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)
```

---

### ❌ Lỗi: `PermissionError: [Errno 13] Permission denied`

**Nguyên nhân:** 
- File đang mở bởi chương trình khác (Excel, text editor)
- Không có quyền ghi

**Fix:**
```bash
# Đóng tất cả chương trình đang mở file
# Sau đó chạy lại

# Hoặc check quyền (Windows)
icacls data\outputs\summaries.json
```

---

## 5. Frontend & Browser

### ❌ Lỗi: Browser hiển thị "Unable to load news data"

**Nguyên nhân:** API call failed

**Debug:**
1. Mở Developer Tools: `F12`
2. Tab "Console" → Xem error message
3. Tab "Network" → Xem request `/api/summaries`

**Possible errors:**

| Error | Nguyên nhân | Fix |
|-------|-------------|-----|
| `404 Not Found` | Endpoint không tồn tại | Kiểm tra server chạy chưa |
| `500 Internal Error` | Server bị lỗi | Xem logs trong terminal |
| `CORS error` | (Không có trong dự án này) | - |

---

### ❌ Lỗi: UI vẫn hiển thị text tiếng Việt cũ

**Nguyên nhân:** Browser cache

**Fix:**
```bash
# Hard refresh (xóa cache)
Ctrl + Shift + R (Windows)
Cmd + Shift + R (Mac)

# Hoặc xóa cache thủ công
Ctrl + Shift + Delete → Clear browsing data
```

**Prevention:** Update cache-bust version
```html
<!-- news.html -->
<script src="js/main.js?v=5.0"></script>
<!-- Tăng version number mỗi lần deploy -->
```

---

### ❌ Lỗi: Keyboard shortcuts không hoạt động

**Triệu chứng:** Press `R` không refresh

**Nguyên nhân:** Focus đang ở input field hoặc JS chưa load

**Debug:**
```javascript
// Console (F12)
console.log(document.getElementById('searchInput'));
// → Phải có element, không null
```

**Fix:**
```javascript
// File: js/ui/keyboard.js

document.addEventListener('keydown', (e) => {
    // Bỏ qua nếu đang gõ trong input
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
        return;
    }
    
    if (e.key === 'r' || e.key === 'R') {
        e.preventDefault();
        refreshData();
    }
});
```

---

## 6. Server & Networking

### ❌ Lỗi: `OSError: [Errno 10048] Address already in use`

**Nguyên nhân:** Port 8000 đã được dùng bởi process khác

**Fix Option A: Kill process**
```bash
# Windows
netstat -ano | findstr :8000
# → Xem PID (Process ID)

taskkill /PID <PID_number> /F
```

**Fix Option B: Dùng port khác**
```bash
uvicorn src.api.app:app --reload --port 8001
```

**Prevention:**
```bash
# Trước khi chạy server, check port
netstat -ano | findstr :8000
# Nếu rỗng → Port available
```

---

### ❌ Lỗi: Server chạy nhưng browser không access được

**Triệu chứng:**
- Server logs: `INFO: Uvicorn running on http://127.0.0.1:8000`
- Browser: "This site can't be reached"

**Nguyên nhân:** 
- Firewall block
- Sai URL

**Debug:**
```bash
# Test server từ terminal
curl http://localhost:8000/healthz

# Expected:
# {"status": "ok"}
```

**Fix:**
```bash
# Nếu curl OK nhưng browser fail → Clear browser cache

# Nếu curl cũng fail → Firewall issue
# Windows: Firewall settings → Allow uvicorn
```

---

### ❌ Lỗi: Server restart liên tục (reload loop)

**Triệu chứng:**
```
INFO: Detected changes, reloading...
INFO: Detected changes, reloading...
INFO: Detected changes, reloading...
```

**Nguyên nhân:** File thay đổi liên tục (logs, __pycache__, v.v.)

**Fix: Exclude folders khỏi reload**
```bash
uvicorn src.api.app:app --reload --reload-exclude "*.log" --reload-exclude "__pycache__"
```

---

## 7. Git & Version Control

### ❌ Lỗi: `git: command not found`

**Fix (Windows):**
1. Download Git: https://git-scm.com/download/win
2. Install với default options
3. Restart terminal

**Fix (Linux):**
```bash
sudo apt install git
```

---

### ❌ Lỗi: Không push được lên GitHub

**Triệu chứng:**
```bash
git push origin main
# → Permission denied (publickey)
```

**Fix:**
```bash
# Setup SSH key
ssh-keygen -t ed25519 -C "your_email@example.com"

# Copy public key
cat ~/.ssh/id_ed25519.pub

# Thêm vào GitHub: Settings → SSH Keys → New SSH Key
```

---

### ❌ Lỗi: `.env` bị commit nhầm lên GitHub

**Nguyên nhân:** Quên thêm vào `.gitignore`

**Fix:**
```bash
# Bước 1: Xóa file khỏi Git (giữ local)
git rm --cached .env

# Bước 2: Thêm vào .gitignore
echo ".env" >> .gitignore

# Bước 3: Commit
git add .gitignore
git commit -m "chore: add .env to gitignore"

# Bước 4: Push
git push origin main

# ⚠️ QUAN TRỌNG: Rotate API key ngay!
# Vì key cũ đã public trên GitHub
```

---

## 🎓 Best Practices Để Tránh Lỗi

### 1. **Luôn activate venv trước khi code**
```bash
# Mỗi lần mở terminal
.venv\Scripts\activate
```

### 2. **Check requirements.txt thường xuyên**
```bash
pip list --outdated
```

### 3. **Dùng type hints**
```python
def fetch_articles(top_n: int) -> List[Article]:
    # IDE sẽ warn nếu truyền sai type
    pass
```

### 4. **Logging thay vì print**
```python
import logging

# ❌ BAD
print("Fetching articles...")

# ✅ GOOD
logging.info("Fetching articles from Techmeme")
```

### 5. **Try-except cho external calls**
```python
try:
    response = requests.get(url, timeout=10)
    response.raise_for_status()
except requests.exceptions.RequestException as e:
    logging.error(f"Failed to fetch: {e}")
    return []
```

### 6. **Test local trước khi commit**
```bash
# Checklist trước khi commit
1. Code chạy không lỗi
2. Test các tính năng chính
3. Không có hardcoded secrets
4. Format code (black/flake8)
```

---

## 📖 Đọc Tiếp

- **[QUICK_START.md](QUICK_START.md)** → Setup nhanh
- **[CONTRIBUTION_GUIDE.md](CONTRIBUTION_GUIDE.md)** → Quy trình coding
- **[ONBOARDING_GUIDE.md](ONBOARDING_GUIDE.md)** → Hiểu dự án

---

**Chúc bạn debug thành công! 🐛🔧**

*Nếu gặp lỗi không có trong list này, hỏi team lead hoặc search Stack Overflow.*
