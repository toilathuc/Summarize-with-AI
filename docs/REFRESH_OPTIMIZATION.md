# Tối ưu Refresh - Tách Frontend Reload vs Backend Update

## 📝 Tóm tắt vấn đề

Bạn đúng rồi! Hiện tại kiến trúc chưa tối ưu:

### ❌ Vấn đề:

- Nút "Refresh" hiện tại chạy **toàn bộ pipeline**: Fetch Techmeme → AI Summarize → Save data
- Mất **30-90 giây** mỗi lần click
- Nếu data đã mới nhất thì **vẫn phải chờ** (lãng phí)
- User không biết data đã update hay chưa

### ✅ Giải pháp:

Tách làm **2 chức năng riêng**:

1. **Refresh UI** (NHANH < 1s): Chỉ reload data từ `summaries.json`
2. **Update Data** (CHẬM 30-90s): Chạy `update_news.py` để fetch + AI summarize

---

## 🏗️ Kiến trúc mới

```
┌─────────────────────────────────────────────────────────────┐
│                    BACKEND UPDATE                            │
│  (Chạy độc lập - Manual hoặc Scheduled)                      │
├─────────────────────────────────────────────────────────────┤
│  python update_news.py                                       │
│      ↓                                                       │
│  1. Fetch Techmeme (10-30s)                                  │
│  2. AI Summarize (20-60s)                                    │
│  3. Save summaries.json                                      │
│  4. Ghi timestamp                                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   FRONTEND REFRESH                           │
│  (User click button - NHANH < 1s)                            │
├─────────────────────────────────────────────────────────────┤
│  GET /api/summaries                                          │
│      ↓                                                       │
│  1. Đọc summaries.json                                       │
│  2. Check freshness (timestamp)                              │
│  3. Trả về data + metadata                                   │
│      {                                                       │
│         "items": [...],                                      │
│         "last_updated": "2024-10-19T10:30:00",               │
│         "is_stale": false,                                   │
│         "freshness": "15 phút trước"                         │
│      }                                                       │
│      ↓                                                       │
│  4. Frontend update UI (< 1s)                                │
│  5. Show notification về freshness                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 So sánh

| Tình huống                     | Cũ (Chậm)                  | Mới (Nhanh)                 |
| ------------------------------ | -------------------------- | --------------------------- |
| **Click refresh khi data mới** | 30-90s (vẫn fetch + AI) ❌ | < 1s (chỉ GET) ✅           |
| **Click refresh khi data cũ**  | 30-90s ❌                  | < 1s + show notification ✅ |
| **Biết data có mới không**     | Không ❌                   | Có ("15 phút trước") ✅     |
| **Waste resources**            | Có ❌                      | Không ✅                    |
| **User experience**            | Đợi lâu ❌                 | Instant feedback ✅         |

---

## 🎯 User Flow mới

### Scenario 1: Data đã mới (< 1 giờ)

```
User click "Làm mới" button
    ↓ (< 1 giây)
Frontend call GET /api/summaries
    ↓
Backend trả về data từ file
    ↓
UI update ngay lập tức
    ↓
Show: "Dữ liệu cập nhật 15 phút trước ✅"
```

### Scenario 2: Data cũ (> 1 giờ)

```
User click "Làm mới" button
    ↓ (< 1 giây)
Frontend call GET /api/summaries
    ↓
Backend trả về data + flag is_stale=true
    ↓
UI update ngay với data cũ
    ↓
Show notification:
    "Dữ liệu đã 2 giờ tuổi.
     Bạn có muốn cập nhật dữ liệu mới không?"
    [Cập nhật] [Để sau]
    ↓ (Nếu user click "Cập nhật")
Trigger POST /api/refresh
    ↓ (30-90 giây)
Poll status → Update UI khi done
```

### Scenario 3: Admin update manual

```
Admin chạy:
    python update_news.py
    ↓ (30-90 giây)
Data mới được save
    ↓
User click "Làm mới"
    ↓ (< 1 giây)
UI load data MỚI NHẤT
    ↓
Show: "Dữ liệu cập nhật vừa xong ✅"
```

---

## 💻 Implementation

### Bước 1: Thêm endpoint GET /api/summaries

```python
# src/api/app.py

from datetime import datetime, timedelta
import json

def check_if_stale(last_updated_str: str, threshold_minutes: int = 60) -> bool:
    """Kiểm tra data có cũ không (> threshold)"""
    if not last_updated_str:
        return True

    try:
        last_updated = datetime.fromisoformat(last_updated_str)
        age = datetime.now() - last_updated
        return age > timedelta(minutes=threshold_minutes)
    except:
        return True

def calculate_age(last_updated_str: str) -> str:
    """Tính tuổi của data: '5 phút trước', '2 giờ trước'"""
    if not last_updated_str:
        return "không rõ"

    try:
        last_updated = datetime.fromisoformat(last_updated_str)
        age = datetime.now() - last_updated
        seconds = age.total_seconds()

        if seconds < 60:
            return "vừa xong"
        elif seconds < 3600:
            minutes = int(seconds / 60)
            return f"{minutes} phút trước"
        elif seconds < 86400:
            hours = int(seconds / 3600)
            return f"{hours} giờ trước"
        else:
            days = int(seconds / 86400)
            return f"{days} ngày trước"
    except:
        return "không rõ"

@app.get("/api/summaries")
def get_summaries():
    """
    Lấy summaries hiện tại KHÔNG trigger update.
    Endpoint NHANH cho UI refresh.
    """
    correlation_id = get_current_correlation_id()
    LOGGER.info("GET /api/summaries - Fetching current data")

    # Kiểm tra file tồn tại
    if not SUMMARY_FILE.exists():
        LOGGER.warning("summaries.json not found")
        return JSONResponse({
            "items": [],
            "last_updated": None,
            "is_stale": True,
            "freshness": "không có dữ liệu",
            "message": "Chưa có dữ liệu. Vui lòng chạy update_news.py",
            "correlation_id": correlation_id,
        })

    try:
        # Đọc file
        with open(SUMMARY_FILE, 'r', encoding='utf-8') as f:
            data = json.load(f)

        # Tính freshness
        last_updated = data.get("last_updated")
        is_stale = check_if_stale(last_updated, threshold_minutes=60)
        freshness = calculate_age(last_updated)

        LOGGER.info(f"Data loaded: {len(data.get('items', []))} items, freshness: {freshness}")

        # Trả về data + metadata
        return JSONResponse({
            **data,
            "is_stale": is_stale,
            "freshness": freshness,
            "correlation_id": correlation_id,
        })

    except Exception as e:
        LOGGER.error(f"Error reading summaries: {e}")
        raise HTTPException(status_code=500, detail=str(e))
```

---

### Bước 2: Update Frontend

```javascript
// js/services/newsService.js

export class NewsService {
  constructor() {
    this.baseURL = window.location.origin;
  }

  /**
   * Làm mới UI với data hiện tại (NHANH < 1s)
   * KHÔNG trigger backend update
   */
  async refreshNews() {
    try {
      console.log("📥 Fetching current summaries...");

      const response = await fetch(`${this.baseURL}/api/summaries`);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      console.log("✅ Data loaded:", {
        items: data.items?.length,
        freshness: data.freshness,
        isStale: data.is_stale,
      });

      // Update UI ngay lập tức
      this.renderNews(data.items || []);

      // Show notification về freshness
      if (!data.items || data.items.length === 0) {
        this.showWarningNotification(
          "Chưa có dữ liệu. Vui lòng chạy: python update_news.py"
        );
      } else if (data.is_stale) {
        // Data cũ - hỏi user có muốn update không
        this.showStaleDataNotification(data.freshness);
      } else {
        // Data mới - chỉ show info
        this.showSuccessNotification(
          `✅ Dữ liệu đã cập nhật ${data.freshness}`
        );
      }

      return data;
    } catch (error) {
      console.error("❌ Error refreshing:", error);
      this.showErrorNotification("Không thể tải dữ liệu: " + error.message);
      throw error;
    }
  }

  /**
   * Show notification cho data cũ với option cập nhật
   */
  showStaleDataNotification(freshness) {
    const message = `
            ⚠️ Dữ liệu đã ${freshness} tuổi.
            Bạn có muốn cập nhật dữ liệu mới không?
            (Sẽ mất 30-90 giây)
        `;

    // Nếu có notification system
    if (window.showConfirmNotification) {
      window.showConfirmNotification(message, {
        confirmText: "Cập nhật ngay",
        cancelText: "Để sau",
        onConfirm: () => this.triggerBackendUpdate(),
      });
    } else {
      // Fallback: confirm dialog
      if (confirm(message)) {
        this.triggerBackendUpdate();
      } else {
        this.showInfoNotification(`Đang hiển thị dữ liệu ${freshness}`);
      }
    }
  }

  /**
   * Trigger backend update job (CHẬM - 30-90s)
   * Chỉ gọi khi user confirm hoặc admin
   */
  async triggerBackendUpdate() {
    try {
      console.log("🔄 Triggering backend update...");
      this.showInfoNotification("Đang cập nhật dữ liệu mới... (30-90 giây)");

      const response = await fetch(`${this.baseURL}/api/refresh`, {
        method: "POST",
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      console.log("Update triggered:", result);

      if (result.status === "already_running") {
        this.showWarningNotification(
          "Đang có job update đang chạy, vui lòng đợi..."
        );
      } else if (result.status === "started") {
        this.showInfoNotification(
          "Đã bắt đầu cập nhật. Vui lòng đợi 30-90 giây..."
        );
      }

      // Poll status
      await this.pollUpdateStatus();
    } catch (error) {
      console.error("❌ Error triggering update:", error);
      this.showErrorNotification(
        "Không thể kích hoạt cập nhật: " + error.message
      );
      throw error;
    }
  }

  /**
   * Poll update status (giữ nguyên logic cũ)
   */
  async pollUpdateStatus(pollInterval = 2000, maxAttempts = 120) {
    let attempts = 0;

    while (attempts < maxAttempts) {
      await this.delay(pollInterval);
      attempts++;

      try {
        const response = await fetch(`${this.baseURL}/api/refresh/status`);
        const status = await response.json();

        console.log(`Status check (${attempts}/${maxAttempts}):`, status);

        // Check if completed
        if (status.completed === true) {
          if (status.success) {
            this.showSuccessNotification("✅ Cập nhật thành công!");
            // Reload UI với data mới
            await this.refreshNews();
          } else {
            this.showErrorNotification(
              "❌ Cập nhật thất bại: " + (status.error || "Unknown error")
            );
          }
          return status;
        }
      } catch (error) {
        console.error("Error polling status:", error);
      }
    }

    // Timeout
    this.showErrorNotification("⏱️ Timeout: Cập nhật quá lâu");
    throw new Error("Polling timeout");
  }

  renderNews(items) {
    // ... existing render logic
  }

  delay(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  // Notification helpers
  showSuccessNotification(msg) {
    console.log("✅", msg);
    // TODO: Show toast/notification
  }

  showErrorNotification(msg) {
    console.error("❌", msg);
    // TODO: Show error toast
  }

  showInfoNotification(msg) {
    console.log("ℹ️", msg);
    // TODO: Show info toast
  }

  showWarningNotification(msg) {
    console.warn("⚠️", msg);
    // TODO: Show warning toast
  }
}
```

---

### Bước 3: Update UI (2 buttons riêng biệt)

```html
<!-- public/index.html hoặc news.html -->

<div class="refresh-controls">
  <!-- Button 1: Làm mới UI (NHANH) -->
  <button id="refreshButton" class="btn btn-primary">🔄 Làm mới</button>

  <!-- Button 2: Cập nhật data (CHẬM) -->
  <button id="updateButton" class="btn btn-secondary">
    ⚡ Cập nhật dữ liệu mới
  </button>

  <!-- Status display -->
  <div id="dataStatus" class="status-info">
    <span id="freshnessIndicator">Đang tải...</span>
  </div>
</div>
```

```javascript
// js/main.js

import { NewsService } from "./services/newsService.js";

const newsService = new NewsService();

// Button 1: Làm mới UI (NHANH)
document.getElementById("refreshButton").addEventListener("click", async () => {
  console.log("User clicked Refresh button");
  const data = await newsService.refreshNews();

  // Update status indicator
  document.getElementById(
    "freshnessIndicator"
  ).textContent = `Cập nhật ${data.freshness}`;
});

// Button 2: Cập nhật data mới (CHẬM)
document.getElementById("updateButton").addEventListener("click", async () => {
  console.log("User clicked Update button");

  const confirmed = confirm(
    "Cập nhật dữ liệu mới sẽ mất 30-90 giây.\n" + "Bạn có chắc muốn tiếp tục?"
  );

  if (confirmed) {
    await newsService.triggerBackendUpdate();
  }
});

// Auto-refresh UI mỗi 30 giây (KHÔNG trigger backend)
setInterval(async () => {
  console.log("Auto-refresh UI...");
  await newsService.refreshNews();
}, 30000);

// Load initial data
newsService.refreshNews();
```

---

## 📅 Bonus: Scheduled Auto-Update

### Cron job (Linux/Mac):

```bash
# Crontab: Update mỗi 1 giờ
0 * * * * cd /path/to/Demo_Skola && /path/to/.venv/bin/python update_news.py >> /tmp/update_news.log 2>&1
```

### Windows Task Scheduler:

```powershell
# Tạo task update mỗi 1 giờ
schtasks /create /tn "UpdateTechNews" /tr "C:\path\to\.venv\Scripts\python.exe C:\path\to\Demo_Skola\update_news.py" /sc hourly /st 09:00
```

### Python APScheduler (trong FastAPI):

```python
# src/api/app.py

from apscheduler.schedulers.background import BackgroundScheduler
import subprocess

scheduler = BackgroundScheduler()

def scheduled_update():
    """Chạy update_news.py tự động"""
    LOGGER.info("Running scheduled update...")
    try:
        result = subprocess.run(
            [sys.executable, "update_news.py"],
            capture_output=True,
            text=True,
            timeout=300  # 5 phút timeout
        )
        if result.returncode == 0:
            LOGGER.info("Scheduled update completed successfully")
        else:
            LOGGER.error(f"Scheduled update failed: {result.stderr}")
    except Exception as e:
        LOGGER.error(f"Error in scheduled update: {e}")

# Schedule update mỗi 1 giờ
scheduler.add_job(scheduled_update, 'interval', hours=1)

@app.on_event("startup")
def start_scheduler():
    scheduler.start()
    LOGGER.info("Scheduler started - auto update every 1 hour")

@app.on_event("shutdown")
def shutdown_scheduler():
    scheduler.shutdown()
```

---

## ✅ Kết luận

### Trước (Chậm ❌):

- Click refresh → Chạy toàn bộ pipeline → Đợi 30-90s
- Không biết data có mới không
- Lãng phí resources

### Sau (Nhanh ✅):

- Click "Làm mới" → Reload UI < 1s → Show freshness
- Nếu data cũ → Notification hỏi có muốn update không
- Click "Cập nhật dữ liệu mới" → Trigger backend job
- Optional: Auto-update theo schedule (cron/scheduler)

### Lợi ích:

- ✅ **Nhanh**: < 1s thay vì 30-90s
- ✅ **Thông minh**: Biết data mới/cũ
- ✅ **Tiết kiệm**: Không fetch/AI nếu data đã mới
- ✅ **UX tốt**: Instant feedback + smart notification

---

**Bạn có muốn tôi implement giải pháp này không?** 🚀
