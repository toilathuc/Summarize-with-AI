# 🌐 Tech News Website - Hướng dẫn sử dụng

## ✅ **Website đã được tạo thành công!**

### 🎯 **Tính năng chính:**

- ✅ **Giao diện đẹp và responsive** - Hoạt động tốt trên mọi thiết bị
- ✅ **Hiển thị tiêu đề** - Tên bài viết được hiển thị rõ ràng
- ✅ **Bullet points** - Các điểm chính được liệt kê dễ đọc
- ✅ **Click để điều hướng** - Click vào bài viết sẽ mở trang web gốc
- ✅ **Tìm kiếm và lọc** - Tìm kiếm theo từ khóa và lọc theo loại tin
- ✅ **Phân loại màu sắc** - Mỗi loại tin có màu riêng biệt
- ✅ **"Tại sao quan trọng?"** - Giải thích ý nghĩa của từng tin tức

### 🚀 **Cách khởi chạy website:**

#### **Phương pháp 1: Sử dụng Python server (Đề xuất)**

```powershell
C:/Users/ADMIN/AppData/Local/Programs/Python/Python313/python.exe .\serve_website.py
```

- Server sẽ khởi chạy tại: `http://localhost:8000`
- Browser sẽ tự động mở
- Press `Ctrl+C` để dừng server

#### **Phương pháp 2: Mở file trực tiếp**

- Double-click vào file `news.html`
- Hoặc kéo file vào browser

### 🎨 **Giao diện website:**

#### **Header (Đầu trang):**

- Tiêu đề chính với icon
- Thống kê: thời gian cập nhật và số lượng bài viết
- Gradient background đẹp mắt

#### **Filter Bar (Thanh lọc):**

- **Dropdown filter**: Lọc theo loại tin (News, Announcement, Video, etc.)
- **Search box**: Tìm kiếm theo từ khóa trong tiêu đề, bullets, hoặc nội dung

#### **News Cards (Thẻ tin tức):**

- **Title**: Tiêu đề bài viết
- **Type badge**: Nhãn phân loại với màu sắc riêng
- **Bullet points**: Các điểm chính của bài viết
- **"Why it matters"**: Giải thích tại sao tin này quan trọng
- **"Đọc bài gốc"**: Nút link đến trang web gốc

### 🎯 **Cách sử dụng:**

1. **Xem tất cả tin tức**: Scroll xuống để xem tất cả bài viết
2. **Tìm kiếm**: Nhập từ khóa vào ô search
3. **Lọc theo loại**: Chọn loại tin trong dropdown
4. **Đọc chi tiết**: Click vào bài viết hoặc nút "Đọc bài gốc"
5. **Keyboard shortcuts**:
   - `Ctrl+F`: Focus vào ô tìm kiếm
   - `Escape`: Xóa tìm kiếm

### 🎨 **Phân loại màu sắc:**

- 🔵 **News** (Tin tức): Màu xanh dương
- 🟢 **Announcement** (Thông báo): Màu xanh lá
- 🟡 **Video**: Màu vàng
- 🟣 **How-to** (Hướng dẫn): Màu tím
- 🔴 **Troubleshooting** (Khắc phục): Màu đỏ

### 📁 **Cấu trúc files:**

```
Demo_Skola/
├── news.html          # Trang web chính ✅
├── styles.css         # CSS styling ✅
├── script.js          # JavaScript logic ✅
├── summaries.json     # Dữ liệu tin tức ✅
├── serve_website.py   # Python web server ✅
└── WEBSITE_README.md  # Hướng dẫn này
```

### 🔄 **Cập nhật dữ liệu:**

Để cập nhật tin tức mới:

1. Chạy lại pipeline AI:
   ```powershell
   C:/Users/ADMIN/AppData/Local/Programs/Python/Python313/python.exe .\src\pipelines\main_pipeline.py
   ```
2. Copy file mới:
   ```powershell
   Copy-Item .\data\outputs\summaries.json .\summaries.json
   ```
3. Refresh trang web (F5)

### ✨ **Tính năng responsive:**

- **Desktop**: Layout 2 cột, đầy đủ tính năng
- **Tablet**: Layout 1 cột, tối ưu cho touch
- **Mobile**: Giao diện nhỏ gọn, dễ sử dụng

### 🎯 **Website đã sẵn sàng sử dụng!**

- URL: `http://localhost:8000`
- Giao diện đẹp, dễ sử dụng ✅
- Click để điều hướng ✅
- Hiển thị đầy đủ thông tin ✅

**🎉 Thành công hoàn toàn! Website tin tức đã được tạo và đang hoạt động.**
