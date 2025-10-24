# 🗑️ SCRIPT DỌN DẸP TỰ ĐỘNG

Write-Host "================================" -ForegroundColor Cyan
Write-Host "   DỌN DẸP PROJECT" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Backup trước khi xóa
$backupDir = "backup_cleanup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
Write-Host "📦 Tạo backup: $backupDir" -ForegroundColor Yellow
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

# ===== PHẦN 1: FILE TEST (XÓA AN TOÀN) =====

Write-Host ""
Write-Host "🗑️  Xóa file test..." -ForegroundColor Green

$testFiles = @(
    "test.html",
    "test_refresh.html",
    "test_fast_refresh.py",
    "test_endpoint.ps1"
)

foreach ($file in $testFiles) {
    if (Test-Path $file) {
        Write-Host "  ✓ Backup: $file" -ForegroundColor Gray
        Copy-Item $file -Destination $backupDir -ErrorAction SilentlyContinue
        
        Write-Host "  ✓ Xóa: $file" -ForegroundColor Red
        Remove-Item $file -Force
    } else {
        Write-Host "  - Không tìm thấy: $file" -ForegroundColor DarkGray
    }
}

# ===== PHẦN 2: FILE TRÙNG LẶP (XÓA) =====

Write-Host ""
Write-Host "🗑️  Xóa file trùng lặp..." -ForegroundColor Green

$duplicateFiles = @(
    "quick_refresh.py",
    "simple_update.py",
    "quick_start.bat",
    "styles.css"  # Giữ folder styles/, xóa file styles.css ở root
)

foreach ($file in $duplicateFiles) {
    if (Test-Path $file) {
        Write-Host "  ✓ Backup: $file" -ForegroundColor Gray
        Copy-Item $file -Destination $backupDir -ErrorAction SilentlyContinue
        
        Write-Host "  ✓ Xóa: $file" -ForegroundColor Red
        Remove-Item $file -Force
    } else {
        Write-Host "  - Không tìm thấy: $file" -ForegroundColor DarkGray
    }
}

# ===== PHẦN 3: FOLDER PUBLIC (XÓA - App khác) =====

Write-Host ""
Write-Host "🗑️  Xóa folder public/ (app khác - Skola)..." -ForegroundColor Green

if (Test-Path "public") {
    Write-Host "  ✓ Backup: public/" -ForegroundColor Gray
    Copy-Item -Path "public" -Destination "$backupDir\public" -Recurse -ErrorAction SilentlyContinue
    
    Write-Host "  ✓ Xóa: public/" -ForegroundColor Red
    Remove-Item "public" -Recurse -Force
} else {
    Write-Host "  - Không tìm thấy: public/" -ForegroundColor DarkGray
}

# ===== PHẦN 4: TẠO FILE INSTRUCTIONS =====

Write-Host ""
Write-Host "📝 Tạo file hướng dẫn..." -ForegroundColor Green

$instructionsContent = @"
# 🚀 HƯỚNG DẪN SỬ DỤNG

## 📁 CẤU TRÚC SAU KHI DỌN DẸP

``````
Demo_Skola/
├── news.html                  ← TRANG CHÍNH
├── summaries.json             ← DỮ LIỆU
│
├── update_news.py             ← CẬP NHẬT (Admin)
├── update_news.bat            ← Batch file (Windows)
├── start_fastapi.bat          ← KHỞI ĐỘNG SERVER
│
├── data/outputs/              ← Dữ liệu gốc
├── src/                       ← Source code
├── js/                        ← Frontend
├── styles/                    ← CSS
└── docs/                      ← Tài liệu
``````

---

## 👤 CHO USER (Xem tin)

### **1. Khởi động server:**
``````powershell
.\start_fastapi.bat
``````

### **2. Mở browser:**
``````
http://localhost:8000/news.html
``````

### **3. Click nút "Làm mới"**
- Góc trên bên trái
- Thấy tin ngay (<1s)
- Icon: ✅ (fresh) hoặc ⚠️ (stale)

---

## 👨‍💼 CHO ADMIN (Cập nhật data)

### **Cách 1: Python**
``````powershell
python update_news.py
``````

### **Cách 2: Batch file**
``````powershell
.\update_news.bat
``````

### **Cách 3: Double-click**
- Mở File Explorer
- Double-click `update_news.bat`

**Quá trình:**
- ⏱️ 30-90 giây
- 🌐 Fetch Techmeme
- 🤖 AI Gemini
- 💾 Lưu JSON
- ✅ Done!

---

## 📦 BACKUP

Nếu cần khôi phục file đã xóa:
``````powershell
# Xem các backup
Get-ChildItem backup_*

# Khôi phục từ backup
Copy-Item backup_YYYYMMDD_HHMMSS\* -Destination . -Force
``````

---

## 📚 TÀI LIỆU

- `README.md` - Hướng dẫn tổng quan
- `docs/ADMIN_GUIDE.md` - Hướng dẫn Admin
- `docs/REFRESH_IMPLEMENTATION.md` - Chi tiết kỹ thuật

---

## 🆘 HỖ TRỢ

**Server không chạy?**
``````powershell
# Kiểm tra port
netstat -ano | findstr :8000

# Dừng process cũ
Get-Process python | Stop-Process -Force

# Khởi động lại
.\start_fastapi.bat
``````

**Không thấy tin tức?**
1. Hard refresh: `Ctrl + Shift + R`
2. Clear cache: `Ctrl + Shift + Delete`
3. Incognito: `Ctrl + Shift + N`

**Update lỗi?**
- Kiểm tra `.env` có API key
- Kiểm tra internet
- Xem log lỗi chi tiết
"@

$instructionsContent | Out-File -FilePath "INSTRUCTIONS.md" -Encoding UTF8
Write-Host "  ✓ Tạo: INSTRUCTIONS.md" -ForegroundColor Green

# ===== PHẦN 5: THỐNG KÊ =====

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "   HOÀN THÀNH!" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "📊 Thống kê:" -ForegroundColor Yellow
Write-Host "  ✓ Đã backup: $backupDir" -ForegroundColor Green
Write-Host "  ✓ Đã xóa: $($testFiles.Count + $duplicateFiles.Count + 1) file/folder" -ForegroundColor Green
Write-Host "  ✓ Đã tạo: INSTRUCTIONS.md" -ForegroundColor Green
Write-Host ""

Write-Host "📁 Cấu trúc mới:" -ForegroundColor Yellow
Write-Host "  - news.html           ← Trang chính" -ForegroundColor Cyan
Write-Host "  - summaries.json      ← Dữ liệu" -ForegroundColor Cyan
Write-Host "  - update_news.py      ← Update script" -ForegroundColor Cyan
Write-Host "  - start_fastapi.bat   ← Khởi động" -ForegroundColor Cyan
Write-Host ""

Write-Host "✨ Project đã sạch sẽ hơn!" -ForegroundColor Green
Write-Host ""
Write-Host "📖 Đọc INSTRUCTIONS.md để biết cách sử dụng" -ForegroundColor Yellow
Write-Host ""

# Hỏi có muốn xem backup không
$response = Read-Host "Bạn có muốn xem nội dung backup không? (y/n)"
if ($response -eq "y") {
    explorer $backupDir
}

Write-Host ""
Write-Host "Nhấn phím bất kỳ để đóng..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
