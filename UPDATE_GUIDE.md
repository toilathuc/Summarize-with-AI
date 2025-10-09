# 🔄 Hướng dẫn cập nhật dữ liệu tin tức

## ✅ **Dữ liệu đã được cập nhật thành công!**

### 📊 **Kết quả cập nhật mới nhất:**

- ⏰ **Thời gian**: 02/10/2025 00:30
- 📈 **Tổng số bài**: 15 bài viết
- 📊 **Phân loại**: 8 tin tức + 7 thông báo
- 🆕 **Trạng thái**: Dữ liệu mới nhất từ Techmeme

---

## 🚀 **3 Cách cập nhật dữ liệu:**

### **1. 🖱️ Sử dụng nút Refresh trên website**

- Mở website: `http://localhost:8000`
- Click nút **"Refresh"** màu xanh ở thanh filter
- Đợi vài giây để tải dữ liệu mới
- ✅ **Dễ nhất - Chỉ cần 1 click!**

### **2. 💻 Chạy file batch (Windows)**

```batch
# Double-click file:
update_news.bat
```

- Tự động lấy dữ liệu mới
- Copy vào thư mục website
- Hiển thị thống kê chi tiết
- ✅ **Nhanh chóng - Tự động hoàn toàn**

### **3. 🐍 Chạy script Python thủ công**

```powershell
C:/Users/ADMIN/AppData/Local/Programs/Python/Python313/python.exe .\update_news.py
```

- Điều khiển chi tiết từng bước
- Xem log đầy đủ
- Xử lý lỗi tốt hơn
- ✅ **Linh hoạt - Có thể debug**

---

## ⚡ **Cập nhật tự động định kỳ:**

### **Cách 1: Auto-update mỗi 30 phút**

```batch
# Double-click file:
start_auto_update.bat
```

- Tự động cập nhật mỗi 30 phút
- Chạy liên tục trong background
- Nhấn `Ctrl+C` để dừng

### **Cách 2: Tạo Windows Task Scheduler**

1. Mở **Task Scheduler**
2. Tạo task mới chạy `update_news.bat`
3. Đặt lịch chạy theo ý muốn
4. ✅ **Chuyên nghiệp - Chạy ngầm**

---

## 🌐 **Cách kiểm tra dữ liệu mới:**

### **Trên website:**

- 🔄 Nhấn `F5` để refresh trang
- 👀 Xem thời gian "Cập nhật" ở header
- 📊 Kiểm tra số lượng bài viết
- 🎯 Click nút "Refresh" để cập nhật realtime

### **Kiểm tra timestamp:**

```javascript
// Mở Developer Tools (F12) và chạy:
fetch("./summaries.json")
  .then((r) => r.json())
  .then((d) => console.log("Last updated:", d.last_updated));
```

---

## 🔧 **Xử lý sự cố:**

### **Lỗi "No module named..."**

```bash
# Cài lại thư viện:
pip install beautifulsoup4 requests feedparser google-generativeai python-dotenv
```

### **Lỗi API key:**

- Kiểm tra file `.env`
- Đảm bảo API key đúng
- Kiểm tra quota Google AI

### **Lỗi network:**

- Kiểm tra kết nối internet
- Thử lại sau vài phút
- Xem có bị block không

### **Website không hiển thị dữ liệu mới:**

1. Hard refresh: `Ctrl+F5`
2. Clear cache: `Ctrl+Shift+R`
3. Check file `summaries.json` có update không
4. Restart web server

---

## 📁 **Files liên quan:**

```
Demo_Skola/
├── update_news.py         # Script cập nhật chính ✅
├── update_news.bat        # Windows batch file ✅
├── start_auto_update.bat  # Auto-update định kỳ ✅
├── summaries.json         # Dữ liệu tin tức ✅
├── news.html             # Website với nút Refresh ✅
└── script.js             # JavaScript xử lý refresh ✅
```

---

## 🎯 **Khuyến nghị sử dụng:**

### **Sử dụng hàng ngày:**

- 🖱️ **Nút Refresh trên web** - Dễ nhất, nhanh nhất

### **Cập nhật định kỳ:**

- 💻 **start_auto_update.bat** - Tự động mỗi 30 phút

### **Debug/troubleshoot:**

- 🐍 **update_news.py** - Xem log chi tiết

**🎉 Bây giờ bạn có thể cập nhật tin tức dễ dàng chỉ với 1 click!**
