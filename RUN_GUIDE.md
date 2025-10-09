# 🚀 HƯỚNG DẪN CHẠY CHƯƠNG TRÌNH

## 📋 **TÓM TẮT NHANH**

### Cách 1: Chạy ngay với dữ liệu có sẵn

```bash
python serve_website.py
```

→ Mở http://localhost:8000

### Cách 2: Cập nhật dữ liệu mới rồi chạy

```bash
python update_news.py
python serve_website.py
```

---

## 🔧 **THIẾT LẬP LẦN ĐẦU**

### 1. Kiểm tra Python

```bash
python --version
# Cần Python 3.8+
```

### 2. Cài đặt thư viện (nếu chưa có)

```bash
pip install feedparser beautifulsoup4 requests google-generativeai python-dotenv
```

### 3. Tạo file .env (nếu chưa có)

```bash
# Tạo file .env với nội dung:
GEMINI_API_KEY=AIzaSyDJa9y89ziAcAnV5iSU5ma-pwdAgUMProA
TECHMEME_FEED_URL=https://www.techmeme.com/feed.xml
```

---

## 🎯 **CÁC CÁCH CHẠY CHI TIẾT**

### 🟢 **Phương pháp 1: Quick Start (Khuyến nghị)**

```bash
# Chạy website với dữ liệu có sẵn
python serve_website.py
```

- ✅ Nhanh nhất
- ✅ Không cần internet để test
- ✅ Sử dụng dữ liệu từ summaries.json

### 🟡 **Phương pháp 2: Cập nhật dữ liệu**

```bash
# Lấy dữ liệu mới và chạy website
python update_news.py
python serve_website.py
```

- ✅ Dữ liệu mới nhất
- ⚠️ Cần internet và API key

### 🔵 **Phương pháp 3: Chạy từng bước**

```bash
# Bước 1: Lấy RSS
python src/feeds/techmeme.py --enrich 5

# Bước 2: Xử lý với AI
python src/pipelines/main_pipeline.py

# Bước 3: Copy kết quả
copy data/outputs/summaries.json ./summaries.json

# Bước 4: Chạy website
python serve_website.py
```

### 🟣 **Phương pháp 4: Batch Files (Windows)**

```bash
# Cập nhật dữ liệu:
./update_news.bat

# Chạy website:
./serve_website.py

# Auto-update định kỳ:
./start_auto_update.bat
```

---

## 📁 **CẤU TRÚC FILES**

```
Demo_Skola/
├── 🌐 serve_website.py     # Chạy web server
├── 🔄 update_news.py       # Cập nhật dữ liệu
├── 📄 news.html           # Website chính
├── 🎨 styles.css          # CSS styling
├── ⚡ script.js           # JavaScript
├── 📊 summaries.json      # Dữ liệu hiển thị
└── src/
    ├── feeds/techmeme.py  # Lấy RSS
    ├── pipelines/main_pipeline.py  # Xử lý AI
    └── services/summarize_with_gemini.py  # AI summarization
```

---

## 🌐 **SỬ DỤNG WEBSITE**

### Sau khi chạy `python serve_website.py`:

1. **Mở browser:** http://localhost:8000
2. **Features:**
   - 📰 Xem tin tức tech từ Techmeme
   - 🔍 Tìm kiếm và lọc
   - 🔄 Nút refresh ở góc trái (có hiệu ứng đặc biệt)
   - 📱 Responsive design

### Nút Refresh:

- **Click** → Tải lại dữ liệu mới
- **Loading** → Hiệu ứng xoay + glow
- **Success** → Màu xanh + animation
- **Error** → Màu đỏ + shake

---

## 🔧 **TROUBLESHOOTING**

### ❌ Lỗi "ModuleNotFoundError"

```bash
pip install [tên-module]
```

### ❌ Lỗi "Port 8000 already in use"

```bash
# Kill process đang dùng port
Get-Process | Where-Object {$_.ProcessName -eq "python"} | Stop-Process -Force

# Hoặc dùng port khác
python serve_website.py --port 8001
```

### ❌ Lỗi Gemini API

```bash
# Kiểm tra .env file có đúng API key
# Hoặc chạy với dữ liệu có sẵn:
python serve_website.py
```

### ❌ Lỗi "summaries.json not found"

```bash
# Copy dữ liệu mẫu:
copy data/outputs/summaries.json ./summaries.json
```

---

## 🧪 **TEST & DEBUG**

### Test Techmeme connection:

```bash
python test_techmeme.py
```

### Test pipeline:

```bash
python test_integration.py
```

### Quick test:

```bash
python test_quick.py
```

---

## ⚡ **AUTO-UPDATE**

### Cập nhật định kỳ (Windows):

```bash
# Chạy trong background
start_auto_update.bat
```

### Manual update:

```bash
python update_news.py
```

---

## 📞 **HỖ TRỢ**

### Files log để debug:

- `test_techmeme_output.json` - Dữ liệu raw
- `test_integration_results.json` - Test results
- Console output khi chạy các scripts

### Kiểm tra status:

```bash
# Xem process đang chạy
Get-Process python

# Xem port đang dùng
netstat -ano | findstr :8000
```
