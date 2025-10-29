# 🎬 Demo Script - Kịch Bản Demo Dự Án

> **Mục đích:** Kịch bản từng bước để demo các tính năng của dự án cho người mới.

**Thời gian demo:** 10-15 phút | **Cấp độ:** Beginner

---

## 📋 Chuẩn Bị Trước Demo

### ✅ Checklist

- [ ] Server đã chạy (`start_fastapi.bat`)
- [ ] Data đã update (`python update_news.py --top 30`)
- [ ] Browser đã mở (`http://localhost:8000`)
- [ ] Terminal/Console chuẩn bị sẵn (để show commands)
- [ ] File `summaries.json` có data (check `cat data/outputs/summaries.json`)

---

## 🎯 Demo Flow (10 phút)

### Part 1: Giới Thiệu Vấn Đề (2 phút)

**Script:**
```
"Chào mừng các bạn! Hôm nay tôi sẽ demo dự án Tech News Summarizer.

Vấn đề: Mỗi ngày có hàng trăm bài viết công nghệ mới. Làm sao để cập nhật 
tin tức mà không mất nhiều thời gian đọc từng bài?

Giải pháp: Dự án này tự động:
1. Lấy tin từ Techmeme (nguồn tin tech uy tín)
2. Dùng AI (Google Gemini) tóm tắt mỗi bài thành 2-3 câu
3. Hiển thị trên web UI đẹp mắt

Giờ chúng ta cùng xem cách hoạt động nhé!"
```

---

### Part 2: Demo Backend - Update News (3 phút)

**Bước 1: Mở Terminal**
```bash
# Chuyển đến project folder
cd e:\Viscode\Demo_Skola

# Activate virtual environment
.venv\Scripts\activate
```

**Script:**
```
"Đây là terminal. Đầu tiên, tôi activate virtual environment để đảm bảo 
dùng đúng Python packages cho dự án này."
```

---

**Bước 2: Chạy Update Script**
```bash
python update_news.py --top 10
```

**Script:**
```
"Command này sẽ:
1. Lấy tin từ Techmeme RSS feed
2. Gửi từng bài cho Gemini AI để tóm tắt
3. Lưu kết quả vào file JSON

Chờ khoảng 20-30 giây..."
```

**Expected Output:**
```
Fetching articles from Techmeme...
✓ Found 15 articles
Summarizing with Gemini AI...
✓ Summarized 15/15 articles
Saving to data/outputs/summaries.json...
✓ Saved successfully
```

**Script:**
```
"Xong! Data đã được update. Giờ chúng ta xem file JSON này."
```

---

**Bước 3: Xem File JSON**
```bash
# Show first 30 lines
cat data/outputs/summaries.json | head -30
```

**Script:**
```
"Đây là file lưu trữ data. Bạn thấy:
- last_updated: Timestamp cập nhật gần nhất
- total_items: Số bài viết (15 bài)
- summaries: Array chứa các bài đã tóm tắt
  + title: Tiêu đề
  + summary: Bản tóm tắt do AI sinh ra (2-3 câu ngắn gọn)
  + url: Link bài gốc
  + source: Nguồn tin

Format JSON giúp dễ đọc và tương thích với mọi ngôn ngữ lập trình."
```

---

### Part 3: Demo Frontend - Web UI (3 phút)

**Bước 1: Mở Browser**
```
http://localhost:8000
```

**Script:**
```
"Giờ chúng ta xem giao diện web. Tôi mở browser và truy cập localhost:8000."
```

---

**Bước 2: Giải Thích UI**

**Chỉ vào từng phần:**
```
[Header]
"Phía trên là tiêu đề 'Tech News Summaries' và thông tin freshness 
(data được cập nhật lúc nào)."

[Filters]
"Thanh filter này cho phép lọc theo nguồn tin. Hiện tại có TechCrunch, 
The Verge, v.v."

[News Cards]
"Mỗi card là 1 bài viết:
- Tiêu đề (click vào sẽ mở bài gốc ở tab mới)
- Bản tóm tắt 2-3 câu (do AI sinh)
- Thông tin meta: nguồn, thời gian

Giao diện responsive, xem trên mobile cũng đẹp."
```

---

**Bước 3: Demo Tính Năng - Filter**

**Action: Click vào filter "TechCrunch"**

**Script:**
```
"Tôi click vào TechCrunch... Và ngay lập tức chỉ hiển thị các bài từ 
TechCrunch. Đây là filter client-side (JavaScript), rất nhanh.

Click 'All' để hiển thị lại tất cả."
```

---

**Bước 4: Demo Tính Năng - Refresh**

**Action: Click nút Refresh (↻)**

**Script:**
```
"Nút Refresh này gọi API để lấy data mới nhất. Loading spinner hiện ra... 
và data được cập nhật.

Nếu có bài mới, sẽ có thông báo 'Loaded X new articles'."
```

---

**Bước 5: Demo Developer Tools**

**Action: Press F12 → Tab "Network"**

**Script:**
```
"Tôi mở Developer Tools để xem behind-the-scenes.

Tab Network → Refresh lại trang...

Bạn thấy request GET /api/summaries. Click vào đó...

[Tab Preview]
Đây là JSON response từ server:
- summaries: Array các bài viết
- freshness: "2 hours ago"
- total: 15

Response size ~11KB, thời gian < 1 giây. Rất nhanh!"
```

---

### Part 4: Demo Backend API (2 phút)

**Bước 1: Test API Endpoint**

**Mở tab mới trong browser:**
```
http://localhost:8000/api/summaries
```

**Script:**
```
"Đây là raw API endpoint. JSON được hiển thị trực tiếp.

Nếu bạn build mobile app hoặc desktop app, chỉ cần gọi API này 
để lấy data. Không cần parse HTML."
```

---

**Bước 2: Test Health Endpoint**
```
http://localhost:8000/healthz
```

**Expected:**
```json
{"status": "ok"}
```

**Script:**
```
"Endpoint /healthz dùng để monitoring. Nếu server còn sống → status: ok.

Trong production, load balancer hoặc Docker dùng endpoint này để 
health check."
```

---

**Bước 3: Explain Architecture**

**Show diagram (nếu có projector):**
```
[Trình chiếu file: presentation/diagrams/architecture-overview.mmd]
```

**Script:**
```
"Kiến trúc tổng quan:

[CLI Script] update_news.py
    ↓ Chạy pipeline
[Pipeline] Fetch → Summarize → Save
    ↓ Ghi file JSON
[FastAPI Server] Đọc JSON → Serve API
    ↓ HTTP Response
[Browser] JavaScript call API → Render UI

Ưu điểm:
- Đơn giản, dễ hiểu
- Fast API response (< 1s vì chỉ đọc file)
- Tách biệt update (CLI) và serving (API)

Nhược điểm:
- Không real-time (phải chạy script thủ công)
- File-based storage (không scale nếu > 1000 bài)

Trong roadmap, chúng ta sẽ refactor sang Hexagonal Architecture để 
dễ mở rộng hơn."
```

---

## 🎤 Q&A - Câu Hỏi Thường Gặp

### Q1: "Tại sao chỉ có 15 bài, không phải 30?"

**Answer:**
```
"Good question! Techmeme RSS feed chỉ cung cấp ~15 bài gần nhất. 
Đây là limitation của nguồn dữ liệu, không phải bug trong code.

Nếu muốn nhiều bài hơn, có 2 cách:
1. Thêm nguồn tin khác (Hacker News, Reddit)
2. Crawl toàn bộ Techmeme website (không chỉ RSS)

Hiện tại chúng ta đang implement option 1 trong roadmap."
```

---

### Q2: "AI tóm tắt có chính xác không?"

**Answer:**
```
"Gemini AI rất tốt trong việc tóm tắt, accuracy ~90-95%.

Tuy nhiên, có một số trường hợp:
- Bài quá ngắn → Summary giống y hệt original
- Bài có thuật ngữ kỹ thuật → AI có thể hiểu sai

Giải pháp:
- Add validation: Kiểm tra summary length, similarity
- Allow user feedback: User report nếu summary sai
- Fallback: Nếu API fail → Dùng excerpt từ RSS
```

---

### Q3: "Performance thế nào nếu có 1000 users?"

**Answer:**
```
"Hiện tại:
- API response < 1s (chỉ đọc file JSON)
- Có thể handle ~100 concurrent users

Nếu > 1000 users:
1. Add caching (Redis) → Response < 100ms
2. Move file JSON → SQLite hoặc PostgreSQL
3. Deploy lên cloud (AWS, Azure) với load balancer
4. Add CDN cho static files (HTML/CSS/JS)

Với setup này, có thể handle 10K+ users."
```

---

### Q4: "Có thể tự động update không? Không cần chạy script thủ công?"

**Answer:**
```
"Có 3 cách:

1. Cron job (Linux) hoặc Task Scheduler (Windows)
   - Chạy script mỗi 1 giờ tự động

2. Celery (Python task queue)
   - Background worker chạy định kỳ
   
3. Refactor: Add /api/refresh endpoint
   - User click button → Server trigger update
   - (Đã remove vì gây slowdown, nhưng có thể implement lại với queue)

Hiện tại recommend option 1 (cron job) - đơn giản nhất."
```

---

### Q5: "Code có thể chạy trên production không?"

**Answer:**
```
"Code hiện tại là MVP (Minimum Viable Product), chạy OK cho development.

Trước khi deploy production, cần:
1. ✅ Add proper logging (không dùng print)
2. ✅ Error handling đầy đủ (try-except)
3. ✅ Rate limiting (tránh spam API)
4. ✅ HTTPS (không dùng HTTP)
5. ✅ Environment config (dev/staging/prod)
6. ✅ Monitoring (Prometheus, Grafana)
7. ✅ Tests coverage > 80%

Estimate: 1-2 tuần để production-ready."
```

---

## 🎓 Kết Luận Demo

**Script:**
```
"Tóm tắt lại:

✅ Dự án giải quyết vấn đề: Information overload
✅ Tech stack: Python + FastAPI + Gemini AI + Vanilla JS
✅ Workflow: CLI update → File storage → API serving → Web UI
✅ Performance: Fast API (< 1s), scalable với caching
✅ Roadmap: Thêm nguồn tin, refactor architecture, production deploy

Câu hỏi nào khác không?

Nếu muốn join team, đọc:
- ONBOARDING_GUIDE.md → Hiểu dự án
- QUICK_START.md → Setup local
- TASK_EXAMPLES.md → Làm task đầu tiên

Cảm ơn các bạn đã nghe! 🎉"
```

---

## 📸 Screenshots Checklist

**Nếu demo qua slides, cần capture:**
- [ ] Terminal chạy `update_news.py` (với output)
- [ ] File `summaries.json` (formatted JSON)
- [ ] Web UI - Homepage (full view)
- [ ] Web UI - Filter active (TechCrunch only)
- [ ] Web UI - Loading spinner
- [ ] Developer Tools - Network tab (API call)
- [ ] Raw API response (`/api/summaries`)
- [ ] Architecture diagram

**Tool:** Snipping Tool (Windows) hoặc ShareX

---

## 🎥 Video Demo (Optional)

**Nếu cần record video:**

**Tools:**
- OBS Studio (free, professional)
- Loom (browser-based, easy)

**Script duration:** ~10 phút
- Intro: 1 phút
- Backend demo: 3 phút
- Frontend demo: 3 phút
- Architecture explain: 2 phút
- Outro: 1 phút

**Tips:**
- Zoom browser (125% hoặc 150%) để dễ nhìn
- Slow down cursor movement
- Pause giữa các actions
- Add captions/subtitles

---

**Chúc bạn demo thành công! 🚀**
