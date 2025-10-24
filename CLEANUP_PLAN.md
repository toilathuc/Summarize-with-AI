# 🗂️ DỌN DẸP PROJECT - PHÂN TÍCH & ĐỀ XUẤT

## 📊 HIỆN TRẠNG

### ✅ FILE CHÍNH (CẦN GIỮ)

#### **1. Core Application**

- `src/` - Source code chính
- `js/` - Frontend JavaScript
- `styles/` - CSS files
- `data/` - Dữ liệu
- `.env` - Configuration
- `requirements.txt` - Dependencies

#### **2. Main Pages**

- `news.html` - **Trang chính cho user**
- `summaries.json` - **Dữ liệu công khai**

#### **3. Update Scripts**

- `update_news.py` - **Script cập nhật chính (dùng AI)**
- `update_news.bat` - **Batch file cho Windows**

#### **4. Server Scripts**

- `start_fastapi.bat` - **Khởi động server FastAPI**

#### **5. Documentation**

- `README.md` - Hướng dẫn chính
- `docs/` - Tài liệu chi tiết

---

## ❌ FILE CÓ THỂ XÓA

### **🗑️ Test Files (Tạm thời - có thể xóa)**

1. **`test.html`** ❌

   - File test cũ
   - Không dùng nữa
   - **→ XÓA**

2. **`test_refresh.html`** ❌

   - File test endpoint (tôi tạo để debug)
   - Đã hoàn thành debug
   - **→ XÓA**

3. **`test_fast_refresh.py`** ❌

   - Script test endpoint
   - Không cần thiết cho production
   - **→ XÓA**

4. **`test_endpoint.ps1`** ❌

   - PowerShell test script
   - Debug only
   - **→ XÓA**

5. **`test/` folder** ⚠️
   - Xem nội dung trước khi xóa
   - Nếu chỉ có test → **XÓA**

---

### **🔄 Alternative Scripts (Trùng lặp chức năng)**

6. **`quick_refresh.py`** ⚠️

   - "Touch" file timestamp mà không update thật
   - **Trùng chức năng với `update_news.py`**
   - **→ XÓA** (hoặc giữ nếu cần test nhanh)

7. **`simple_update.py`** ⚠️

   - Update KHÔNG dùng AI (chỉ lấy summary từ RSS)
   - **Trùng chức năng với `update_news.py`**
   - **→ XÓA** (vì bạn muốn dùng AI)

8. **`quick_start.bat`** ⚠️
   - Khởi động server + update
   - **Trùng với `start_fastapi.bat` + `update_news.bat`**
   - **→ XÓA** (hoặc merge)

---

### **🔧 Batch Files (Tùy chọn)**

9. **`start_auto_update.bat`** ⚠️
   - Tự động update mỗi 30 phút
   - **Giữ nếu cần tự động hóa**
   - **→ GIỮ** (nhưng review code)

---

### **📄 Duplicate HTML**

10. **`styles.css`** (root) vs **`styles/`** folder ❌
    - File CSS lẻ ở root
    - Đã có folder `styles/`
    - **→ XÓA file `styles.css` ở root**
    - **→ GIỮ folder `styles/`**

---

## 📋 ĐỀ XUẤT CẤU TRÚC MỚI

### **Cấu trúc sau khi dọn dẹp:**

```
Demo_Skola/
├── .env                          ✅ Config
├── .gitignore                    ✅ Git
├── README.md                     ✅ Docs chính
├── requirements.txt              ✅ Dependencies
│
├── news.html                     ✅ TRANG CHÍNH
├── summaries.json                ✅ DỮ LIỆU CÔNG KHAI
│
├── update_news.py                ✅ SCRIPT CẬP NHẬT (Admin)
├── update_news.bat               ✅ BATCH FILE (Windows)
├── start_fastapi.bat             ✅ KHỞI ĐỘNG SERVER
├── start_auto_update.bat         ⚠️  TỰ ĐỘNG HÓA (optional)
│
├── data/                         ✅ Dữ liệu
│   ├── outputs/
│   │   └── summaries.json        ← Source of truth
│   └── raw/
│
├── src/                          ✅ Source code
│   ├── api/                      ← FastAPI server
│   ├── services/                 ← Business logic
│   ├── pipelines/                ← Data processing
│   └── ...
│
├── js/                           ✅ Frontend JS
│   ├── main.js
│   ├── services/
│   │   └── newsService.js
│   └── ui/
│
├── styles/                       ✅ CSS
│   ├── base.css
│   ├── news-cards.css
│   └── ...
│
├── docs/                         ✅ Documentation
│   ├── REFRESH_IMPLEMENTATION.md
│   ├── ADMIN_GUIDE.md
│   └── ...
│
└── public/                       ⚠️  Xem lại nội dung
    └── index.html                ← Trùng với news.html?
```

---

## 🚀 HÀNH ĐỘNG ĐỀ XUẤT

### **Bước 1: XÓA file test**

```powershell
Remove-Item test.html
Remove-Item test_refresh.html
Remove-Item test_fast_refresh.py
Remove-Item test_endpoint.ps1
```

### **Bước 2: XÓA file trùng lặp**

```powershell
Remove-Item quick_refresh.py
Remove-Item simple_update.py
Remove-Item styles.css         # Giữ folder styles/
```

### **Bước 3: XÓA quick_start.bat (hoặc merge)**

```powershell
Remove-Item quick_start.bat    # Vì đã có start_fastapi.bat
```

### **Bước 4: Kiểm tra folder test/**

```powershell
Get-ChildItem test/
# Nếu chỉ có file test → Xóa toàn bộ folder
Remove-Item test/ -Recurse
```

### **Bước 5: Kiểm tra public/index.html**

```powershell
# Nếu trùng với news.html → Xóa
# Hoặc redirect index.html → news.html
```

---

## 📝 GHI CHÚ

### **CÁC FILE CÓ THỂ GIỮ LẠI NẾU:**

1. **`quick_refresh.py`**

   - Nếu cần test nhanh không gọi AI
   - Chỉ update timestamp

2. **`simple_update.py`**

   - Nếu muốn fallback không dùng AI
   - Tiết kiệm API quota

3. **`start_auto_update.bat`**

   - Nếu cần tự động update mỗi X phút
   - Dùng cho production

4. **`test/` folder**
   - Nếu có unit tests thật sự
   - Kiểm tra trước khi xóa

---

## ⚠️ BACKUP TRƯỚC KHI XÓA

```powershell
# Tạo backup
New-Item -ItemType Directory -Path "backup_$(Get-Date -Format 'yyyyMMdd')"
Copy-Item test.html, test_refresh.html, quick_refresh.py -Destination "backup_$(Get-Date -Format 'yyyyMMdd')/"
```

---

## ✅ KẾT QUẢ SAU KHI DỌN DẸP

- ✨ **Sạch sẽ hơn** - Chỉ giữ file cần thiết
- 🎯 **Dễ hiểu hơn** - Rõ ràng file nào làm gì
- 📦 **Nhỏ gọn hơn** - Giảm số lượng file
- 🚀 **Dễ deploy** - Ít file rác

---

**HÃY CHO TÔI BIẾT BẠN MUỐN XÓA FILE NÀO?**
