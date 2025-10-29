# 💡 Code Concepts - Giải Thích Các Khái Niệm

> **Mục đích:** Giải thích CỰC KỲ CHI TIẾT các khái niệm lập trình dùng trong dự án, giả sử bạn chưa biết gì về web development.

**Thời gian đọc:** ~30 phút | **Cấp độ:** Beginner

---

## 📚 Mục Lục

1. [Web Development Basics](#1-web-development-basics)
2. [FastAPI & REST API](#2-fastapi--rest-api)
3. [Async/Await trong Python](#3-asyncawait-trong-python)
4. [Environment Variables](#4-environment-variables)
5. [Virtual Environment](#5-virtual-environment)
6. [JSON - Data Format](#6-json---data-format)
7. [HTTP Request/Response](#7-http-requestresponse)
8. [Client-Server Architecture](#8-client-server-architecture)

---

## 1. Web Development Basics

### 🌐 Web hoạt động như thế nào?

Khi bạn mở trình duyệt và gõ `google.com`, điều gì xảy ra?

```
Bạn (Client)                          Google (Server)
    │                                      │
    │  1. Gửi request: "Cho tôi trang chủ"│
    ├─────────────────────────────────────►│
    │                                      │ 2. Xử lý request
    │                                      │    (Lấy data từ database)
    │                                      │
    │  3. Trả về HTML/CSS/JS               │
    │◄─────────────────────────────────────┤
    │                                      │
    │  4. Hiển thị trang web
    └──────────────────────
```

**Các thành phần:**

| Thành Phần | Vai Trò | Ví Dụ |
|------------|---------|-------|
| **Client** | Người dùng (trình duyệt) | Chrome, Firefox, Safari |
| **Server** | Máy chủ phục vụ data | Google's server, localhost:8000 |
| **HTTP** | Giao thức truyền tải | Ngôn ngữ client-server nói chuyện |
| **HTML** | Cấu trúc trang web | `<h1>Hello</h1>` |
| **CSS** | Styling (màu sắc, font) | `color: blue;` |
| **JavaScript** | Logic tương tác | Click button → Hiển thị thông báo |

---

### 📄 HTML, CSS, JavaScript là gì?

**Ví dụ: Một cái nhà**

```
HTML = Khung nhà (tường, cửa, phòng)
CSS  = Sơn tường, trang trí (màu sắc, nội thất)
JS   = Điện, nước, thang máy (chức năng)
```

**Ví dụ code:**

```html
<!-- HTML: Cấu trúc -->
<button id="myButton">Click Me!</button>
<p id="message"></p>

<!-- CSS: Styling -->
<style>
  button {
    background-color: blue;
    color: white;
    padding: 10px 20px;
  }
</style>

<!-- JavaScript: Logic -->
<script>
  document.getElementById('myButton').addEventListener('click', function() {
    document.getElementById('message').innerText = 'Hello World!';
  });
</script>
```

**Kết quả:**
- Bạn thấy một nút xanh viết "Click Me!"
- Khi click → Hiện chữ "Hello World!" phía dưới

---

## 2. FastAPI & REST API

### 🚀 FastAPI là gì?

**FastAPI** = Framework Python để xây dựng web API (giống như Flask, Django)

**Tại sao gọi là "Fast"?**
- ✅ Hiệu suất cao (ngang với Node.js, Go)
- ✅ Dùng async/await (xử lý nhiều request cùng lúc)
- ✅ Tự động generate API documentation

---

### 🔗 REST API là gì?

**REST API** = Cách để client và server giao tiếp theo chuẩn HTTP

**Ví dụ thực tế:**

```python
# File: src/api/app.py

from fastapi import FastAPI

app = FastAPI()

# Endpoint 1: Trang chủ
@app.get("/")
async def root():
    return {"message": "Welcome to Tech News API"}

# Endpoint 2: Lấy danh sách tin
@app.get("/api/summaries")
async def get_summaries():
    return {
        "summaries": [
            {"title": "AI breakthrough", "summary": "..."},
            {"title": "Python 4.0 released", "summary": "..."}
        ]
    }
```

**Giải thích:**

| Code | Ý Nghĩa |
|------|---------|
| `@app.get("/")` | Khi client gọi `GET http://localhost:8000/` |
| `async def root()` | Chạy function này (async = không chờ đợi) |
| `return {...}` | Trả về JSON cho client |

**Client gọi API như thế nào?**

```javascript
// Trong browser (JavaScript)
fetch('http://localhost:8000/api/summaries')
  .then(response => response.json())
  .then(data => console.log(data));
```

**Response (JSON):**
```json
{
  "summaries": [
    {"title": "AI breakthrough", "summary": "..."},
    {"title": "Python 4.0 released", "summary": "..."}
  ]
}
```

---

### 📡 HTTP Methods (GET, POST, PUT, DELETE)

| Method | Mục Đích | Ví Dụ |
|--------|----------|-------|
| **GET** | Lấy data (không thay đổi gì) | Xem danh sách tin |
| **POST** | Tạo mới | Thêm bài viết mới |
| **PUT** | Cập nhật | Sửa tiêu đề bài viết |
| **DELETE** | Xóa | Xóa bài viết |

**Trong dự án này, chúng ta chỉ dùng GET** (đọc data, không ghi).

---

## 3. Async/Await trong Python

### ⚡ Đồng Bộ (Synchronous) vs. Bất Đồng Bộ (Asynchronous)

**Ví dụ thực tế: Quán cà phê**

**Đồng bộ (Sync):**
```
1. Khách A order → Nhân viên pha cà phê (3 phút) → Đưa cho khách A
2. Khách B order → Nhân viên pha cà phê (3 phút) → Đưa cho khách B
3. Khách C order → ...

→ Tổng thời gian: 3 + 3 + 3 = 9 phút
```

**Bất đồng bộ (Async):**
```
1. Khách A order → Nhân viên bật máy pha (đợi 3 phút)
2. Trong lúc đợi, nhận order khách B → Bật máy thứ 2
3. Trong lúc đợi, nhận order khách C → Bật máy thứ 3
4. Sau 3 phút → Cả 3 ly cà phê xong cùng lúc

→ Tổng thời gian: ~3 phút (không phải 9 phút)
```

---

### 🔧 Code Async trong Python

**Synchronous code:**
```python
import time

def fetch_news():
    print("Fetching news...")
    time.sleep(2)  # Giả lập gọi API (mất 2 giây)
    return ["Article 1", "Article 2"]

def fetch_weather():
    print("Fetching weather...")
    time.sleep(2)
    return "Sunny"

# Chạy tuần tự
news = fetch_news()        # Chờ 2 giây
weather = fetch_weather()  # Chờ 2 giây nữa

# Tổng thời gian: 4 giây
```

**Asynchronous code:**
```python
import asyncio

async def fetch_news():
    print("Fetching news...")
    await asyncio.sleep(2)  # Không block, CPU làm việc khác
    return ["Article 1", "Article 2"]

async def fetch_weather():
    print("Fetching weather...")
    await asyncio.sleep(2)
    return "Sunny"

# Chạy song song
async def main():
    results = await asyncio.gather(
        fetch_news(),
        fetch_weather()
    )
    print(results)

# Tổng thời gian: ~2 giây (chạy đồng thời)
```

---

### 🎯 Trong FastAPI

```python
@app.get("/api/summaries")
async def get_summaries():
    # Nếu có nhiều request cùng lúc → FastAPI xử lý song song
    data = await load_summaries_from_file()
    return data
```

**Lợi ích:**
- ✅ Server có thể handle nhiều users cùng lúc
- ✅ Không bị block khi đợi I/O (đọc file, gọi API)

---

## 4. Environment Variables

### 🔐 Biến Môi Trường là gì?

**Environment Variable** = Biến lưu trữ thông tin nhạy cảm (API key, password) **KHÔNG** để trong code.

**Tại sao không để trong code?**
- ❌ Nếu push lên GitHub → Ai cũng thấy API key của bạn
- ❌ Hacker có thể lấy key → Dùng API miễn phí bằng tài khoản bạn

---

### 📝 File `.env`

**File: `.env`** (ở root folder)
```env
GEMINI_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXX
TECHMEME_RSS_URL=https://www.techmeme.com/feed.xml
PORT=8000
```

**Cách đọc trong Python:**

```python
# File: src/config/settings.py

import os
from dotenv import load_dotenv

# Load biến từ file .env
load_dotenv()

# Đọc biến
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
PORT = int(os.getenv("PORT", 8000))  # Default = 8000
```

**Giải thích:**
- `load_dotenv()`: Đọc file `.env` và load vào memory
- `os.getenv("KEY")`: Lấy giá trị của biến `KEY`
- `os.getenv("KEY", default)`: Nếu không có → Dùng giá trị default

---

### 🔒 Security Best Practices

**✅ Làm:**
- Thêm `.env` vào `.gitignore` (không push lên GitHub)
- Tạo file `.env.example` với giá trị giả:
  ```env
  GEMINI_API_KEY=your_api_key_here
  PORT=8000
  ```

**❌ Không làm:**
- Hardcode API key trong code:
  ```python
  API_KEY = "AIzaSyXXXXXXXX"  # ❌ BAD!
  ```

---

## 5. Virtual Environment

### 🌍 Virtual Environment là gì?

**Virtual Environment (venv)** = Môi trường Python riêng biệt cho mỗi project.

**Tại sao cần?**

**Scenario:**
- Project A cần `requests==2.28.0`
- Project B cần `requests==2.31.0` (version mới hơn)

→ Nếu cài chung → Conflict! Một trong hai project sẽ bị lỗi.

**Giải pháp: Dùng venv**

```
Computer
├── Project A
│   └── .venv_A (có requests 2.28.0)
│
└── Project B
    └── .venv_B (có requests 2.31.0)
```

---

### 🛠️ Cách Tạo & Sử Dụng venv

**Tạo venv (chỉ làm 1 lần):**
```bash
python -m venv .venv
```

**Kích hoạt venv (mỗi lần mở terminal):**

```bash
# Windows (PowerShell)
.venv\Scripts\activate

# Sau khi activate → Prompt sẽ hiện (.venv)
(.venv) PS C:\project>
```

**Cài packages vào venv:**
```bash
pip install fastapi uvicorn
```

**Tắt venv:**
```bash
deactivate
```

---

### 📦 File `requirements.txt`

**File: `requirements.txt`** = Danh sách tất cả packages cần thiết

```txt
fastapi==0.109.0
uvicorn[standard]==0.27.0
google-generativeai==0.3.0
feedparser==6.0.10
python-dotenv==1.0.0
```

**Cài tất cả packages:**
```bash
pip install -r requirements.txt
```

**Tạo file requirements.txt (sau khi cài xong):**
```bash
pip freeze > requirements.txt
```

---

## 6. JSON - Data Format

### 📋 JSON là gì?

**JSON** (JavaScript Object Notation) = Format để lưu trữ/truyền tải data dạng text.

**Tại sao dùng JSON?**
- ✅ Dễ đọc (human-readable)
- ✅ Nhẹ (lightweight)
- ✅ Tương thích với mọi ngôn ngữ (Python, JS, Java...)

---

### 🔤 Cú Pháp JSON

```json
{
  "name": "John Doe",
  "age": 30,
  "is_student": false,
  "courses": ["Python", "JavaScript"],
  "address": {
    "city": "Hanoi",
    "country": "Vietnam"
  }
}
```

**Quy tắc:**
- Key phải dùng dấu ngoặc kép: `"name"`
- String phải dùng dấu ngoặc kép: `"John"`
- Boolean: `true`, `false` (chữ thường)
- Null: `null`
- Không có comment (không viết `// comment` được)

---

### 🐍 JSON trong Python

**Đọc JSON từ file:**
```python
import json

# Đọc file
with open('data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)  # → Python dict

print(data['name'])  # John Doe
```

**Ghi JSON vào file:**
```python
data = {
    "name": "John",
    "age": 30
}

with open('data.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
```

**Tham số:**
- `ensure_ascii=False`: Cho phép ký tự Unicode (tiếng Việt)
- `indent=2`: Format đẹp (xuống dòng, thụt đầu dòng)

---

### 🌐 JSON trong JavaScript

```javascript
// Parse JSON string → Object
const jsonString = '{"name": "John", "age": 30}';
const obj = JSON.parse(jsonString);
console.log(obj.name);  // John

// Stringify Object → JSON
const obj2 = {name: "Jane", age: 25};
const jsonString2 = JSON.stringify(obj2);
console.log(jsonString2);  // {"name":"Jane","age":25}
```

---

## 7. HTTP Request/Response

### 📨 HTTP Request

**HTTP Request** = Client gửi yêu cầu lên server

**Cấu trúc:**
```
GET /api/summaries HTTP/1.1
Host: localhost:8000
User-Agent: Mozilla/5.0
Accept: application/json
```

**Các phần:**
| Phần | Ý Nghĩa | Ví Dụ |
|------|---------|-------|
| **Method** | Hành động | GET, POST, PUT, DELETE |
| **Path** | Đường dẫn | `/api/summaries` |
| **Headers** | Thông tin thêm | `Content-Type: application/json` |
| **Body** | Dữ liệu (chỉ có ở POST/PUT) | `{"title": "New article"}` |

---

### 📬 HTTP Response

**HTTP Response** = Server trả về kết quả

**Cấu trúc:**
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 1234

{
  "summaries": [...]
}
```

**Status Codes (Mã trạng thái):**

| Code | Ý Nghĩa | Ví Dụ |
|------|---------|-------|
| **200** | OK (thành công) | Data trả về thành công |
| **404** | Not Found | Endpoint không tồn tại |
| **500** | Internal Server Error | Server bị lỗi |
| **401** | Unauthorized | Chưa đăng nhập |
| **403** | Forbidden | Không có quyền truy cập |

---

### 🔧 Gọi API trong JavaScript

**Fetch API:**
```javascript
fetch('http://localhost:8000/api/summaries')
  .then(response => {
    if (!response.ok) {
      throw new Error('Network error');
    }
    return response.json();  // Parse JSON
  })
  .then(data => {
    console.log(data.summaries);
  })
  .catch(error => {
    console.error('Error:', error);
  });
```

**Async/Await version (dễ đọc hơn):**
```javascript
async function fetchSummaries() {
  try {
    const response = await fetch('/api/summaries');
    if (!response.ok) throw new Error('Network error');
    
    const data = await response.json();
    return data;
  } catch (error) {
    console.error('Error:', error);
    return null;
  }
}
```

---

## 8. Client-Server Architecture

### 🏛️ Kiến Trúc Tổng Quan

```
┌─────────────────────────────────────────────────┐
│                  Client Side                    │
│  (Browser - HTML/CSS/JavaScript)                │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │   UI     │  │  Logic   │  │   API    │     │
│  │ (HTML)   │  │   (JS)   │  │  Calls   │     │
│  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────┬───────────────────────────┘
                      │
                      │ HTTP Request (GET /api/summaries)
                      │
                      ▼
┌─────────────────────────────────────────────────┐
│                  Server Side                    │
│  (Python - FastAPI)                             │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │   API    │  │ Business │  │   Data   │     │
│  │  Layer   │  │  Logic   │  │  Layer   │     │
│  └──────────┘  └──────────┘  └──────────┘     │
│                                                 │
│                 ┌──────────┐                    │
│                 │   JSON   │                    │
│                 │   File   │                    │
│                 └──────────┘                    │
└─────────────────────────────────────────────────┘
```

---

### 🔄 Request Lifecycle (Vòng đời của 1 request)

**Scenario: User click "Refresh" button**

```
1. Browser (JS)
   ↓ Gọi: fetch('/api/summaries')
   
2. Network
   ↓ HTTP GET Request
   
3. FastAPI Server
   ↓ Route: @app.get("/api/summaries")
   ↓ Function: get_summaries()
   
4. Business Logic
   ↓ Đọc file: data/outputs/summaries.json
   ↓ Parse JSON
   ↓ Add metadata (freshness, total)
   
5. Response
   ↓ Return JSON: {"summaries": [...]}
   
6. Network
   ↓ HTTP Response (200 OK)
   
7. Browser (JS)
   ↓ Parse JSON
   ↓ Render UI (tạo HTML elements)
   
8. User
   ✅ Thấy data mới trên màn hình
```

**Thời gian:** ~500ms - 1s (nhanh vì chỉ đọc file, không gọi API bên ngoài)

---

## 🎓 Tổng Kết

Sau khi đọc xong tài liệu này, bạn đã hiểu:

✅ **Web Development:** Client-Server, HTML/CSS/JS  
✅ **FastAPI:** Framework Python xây dựng API  
✅ **REST API:** Cách client-server giao tiếp  
✅ **Async/Await:** Xử lý bất đồng bộ (hiệu suất cao)  
✅ **Environment Variables:** Bảo mật API key  
✅ **Virtual Environment:** Quản lý dependencies  
✅ **JSON:** Format dữ liệu phổ biến  
✅ **HTTP:** Request/Response, Status Codes  

---

## 📖 Đọc Tiếp

- **[ONBOARDING_GUIDE.md](ONBOARDING_GUIDE.md)** → Hiểu dự án tổng quan
- **[ARCHITECTURE_EXPLAINED.md](ARCHITECTURE_EXPLAINED.md)** → Kiến trúc chi tiết
- **[TASK_EXAMPLES.md](TASK_EXAMPLES.md)** → Ví dụ task thực tế

---

**Chúc bạn học tốt! 🚀**
