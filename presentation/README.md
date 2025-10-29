# 🎓 Presentation - Tài Liệu Onboarding Hoàn Chỉnh

> **Mục đích:** Hướng dẫn developer mới join team hiểu triệt để dự án Summarize-with-AI từ vấn đề → giải pháp → code → practice.

**Đối tượng:** Junior Developer (biết Python + Git cơ bản, đã setup được môi trường)

**Thời gian học:** 2-3 ngày full-time (hoặc 1-2 tuần part-time)

---

## 📚 Mục Lục Tài Liệu

### 1. 🚀 QUICK_START.md
**Thời gian:** 5-10 phút  
**Mục đích:** Chạy được dự án ngay lập tức

**Nội dung:**
- Setup nhanh (venv, packages, API key)
- Chạy update script
- Khởi động server
- Troubleshooting cơ bản

**Khi nào đọc?** → Đầu tiên, trước khi làm gì khác!

---

### 2. 📖 ONBOARDING_GUIDE.md ⭐
**Thời gian:** 40-45 phút  
**Mục đích:** Hiểu tổng quan dự án (vấn đề → giải pháp → kiến trúc → code)

**Nội dung:**
- Vấn đề thực tế (information overload)
- Giải pháp (AI summarization + web UI)
- Kiến trúc tổng quan (client-server, services pattern)
- Code walkthrough (từng file làm gì)
- Data flow (request lifecycle)
- Next steps (task đầu tiên)

**Khi nào đọc?** → Sau khi chạy thành công (QUICK_START), muốn hiểu sâu

---

### 3. 💡 CODE_CONCEPTS.md
**Thời gian:** 30 phút  
**Mục đích:** Giải thích CỰC KỲ CHI TIẾT các khái niệm lập trình (giả sử không biết gì về web dev)

**Nội dung:**
- Web Development Basics (HTML/CSS/JS, Client-Server)
- FastAPI & REST API
- Async/Await trong Python
- Environment Variables (.env)
- Virtual Environment (.venv)
- JSON format
- HTTP Request/Response
- Client-Server Architecture

**Khi nào đọc?** → Nếu chưa quen với web development hoặc muốn ôn lại kiến thức nền

---

### 4. 🏗️ ARCHITECTURE_EXPLAINED.md
**Thời gian:** 25 phút  
**Mục đích:** Hiểu kiến trúc hiện tại, kiến trúc mục tiêu (hexagonal), và lý do refactor

**Nội dung:**
- Kiến trúc hiện tại (Monolithic with Services)
- Vấn đề của kiến trúc hiện tại (tight coupling, khó test)
- Kiến trúc mục tiêu (Hexagonal/Ports & Adapters)
- Roadmap refactoring (8 bước)
- Folder structure sau khi refactor

**Khi nào đọc?** → Sau khi đã quen với codebase, chuẩn bị làm refactoring tasks

---

### 5. 📝 TASK_EXAMPLES.md
**Thời gian:** 20 phút  
**Mục đích:** Ví dụ thực tế cách fix bug và thêm tính năng, với code mẫu

**Nội dung:**
- Task 1: Fix bug - API trả về data cũ (debug steps, root cause, fix)
- Task 2: Thêm tính năng - Search/filter (HTML + CSS + JS)
- Task 3: Thêm nguồn tin mới - Hacker News (create client, update service)
- Task 4: Refactor - Implement Port (hexagonal architecture)
- Task 5: Performance - Cache API response (in-memory caching)

**Khi nào đọc?** → Sau khi hiểu code, sẵn sàng làm task đầu tiên

---

### 6. ⚠️ COMMON_PITFALLS.md
**Thời gian:** 15 phút  
**Mục đích:** Tổng hợp lỗi thường gặp và cách fix (troubleshooting guide)

**Nội dung:**
- Setup & Environment (venv không activate, pip not found)
- Python & Dependencies (ModuleNotFoundError, ImportError)
- API & External Services (API key invalid, quota exceeded)
- Data & File Operations (FileNotFoundError, JSON decode error)
- Frontend & Browser (cache issues, API call failed)
- Server & Networking (port already in use, CORS)
- Git & Version Control (.env bị commit nhầm)

**Khi nào đọc?** → Khi gặp lỗi, hoặc đọc trước để tránh lỗi

---

### 7. 🤝 CONTRIBUTION_GUIDE.md
**Thời gian:** 15 phút  
**Mục đích:** Quy trình làm việc nhóm (Git workflow, code review, testing)

**Nội dung:**
- Git workflow (branching strategy, commit message format)
- Code style guidelines (PEP 8, naming conventions)
- Testing guidelines (unit tests, coverage goals)
- Code review process (reviewer checklist, comment examples)
- Documentation standards (README structure, docstrings)
- Common mistakes (commit trực tiếp vào main, PR quá lớn)

**Khi nào đọc?** → Trước khi tạo PR đầu tiên

---

### 8. 🎬 DEMO_SCRIPT.md
**Thời gian:** 10-15 phút  
**Mục đích:** Kịch bản demo dự án cho người mới (hoặc stakeholders)

**Nội dung:**
- Chuẩn bị trước demo (checklist)
- Demo flow (10 phút): Vấn đề → Backend → Frontend → Q&A
- Screenshots checklist
- Video demo tips

**Khi nào đọc?** → Khi cần demo dự án cho người khác (team mới, manager, v.v.)

---

### 9. 📊 diagrams/
**Thời gian:** 5 phút mỗi file  
**Mục đích:** Visual aids để hiểu kiến trúc và data flow

**Files:**
- `architecture-overview.mmd` → Mermaid diagram (kiến trúc tổng quan)
- `data-flow.mmd` → Sequence diagram (luồng dữ liệu chi tiết)
- `folder-structure.txt` → Cấu trúc thư mục có chú thích

**Khi nào xem?** → Khi đọc ONBOARDING_GUIDE hoặc ARCHITECTURE_EXPLAINED

---

## 🗺️ Learning Path (Lộ Trình Học)

### Day 1: Setup & Orientation (4 giờ)
```
Morning:
1. Đọc QUICK_START.md (10 phút)
2. Setup môi trường (30 phút)
3. Chạy thử dự án (20 phút)
4. Đọc ONBOARDING_GUIDE.md (45 phút)

Afternoon:
5. Đọc CODE_CONCEPTS.md (30 phút) - nếu cần
6. Explore codebase (1 giờ):
   - Mở từng file trong src/
   - Đọc docstrings
   - Chạy thử từng endpoint
7. Đọc COMMON_PITFALLS.md (15 phút)
8. Practice: Chạy lại update_news.py, sửa API key, v.v. (30 phút)
```

### Day 2: Deep Dive & First Task (6 giờ)
```
Morning:
1. Đọc ARCHITECTURE_EXPLAINED.md (25 phút)
2. Vẽ lại architecture diagram bằng tay (30 phút) - để nhớ
3. Đọc TASK_EXAMPLES.md (20 phút)
4. Chọn 1 task đơn giản (Task 1 hoặc Task 2) (15 phút)

Afternoon:
5. Làm task đầu tiên (2-3 giờ):
   - Tạo branch
   - Code
   - Test
   - Commit
6. Đọc CONTRIBUTION_GUIDE.md (15 phút)
7. Tạo PR đầu tiên (30 phút):
   - Fill PR template
   - Request review
8. Address review comments (nếu có) (1 giờ)
```

### Day 3: Advanced & Contribution (4 giờ)
```
Morning:
1. Refactor task (Task 4) hoặc Performance task (Task 5) (2 giờ)
2. Write unit tests cho code mới (1 giờ)

Afternoon:
3. Code review cho PR của người khác (30 phút) - học hỏi
4. Update docs nếu cần (30 phút)
5. Đọc DEMO_SCRIPT.md → Demo cho team lead (15 phút)
```

---

## 🎯 Mục Tiêu Sau Khi Học Xong

Sau khi hoàn thành tài liệu này, bạn sẽ:

✅ **Hiểu vấn đề** → Tại sao cần dự án này  
✅ **Hiểu giải pháp** → Cách dự án hoạt động (high-level)  
✅ **Hiểu kiến trúc** → Từng layer, từng component làm gì  
✅ **Đọc được code** → Biết file nào chứa logic gì  
✅ **Debug được** → Tìm lỗi và fix  
✅ **Thêm được feature** → Implement tính năng mới  
✅ **Refactor được** → Cải thiện code structure  
✅ **Làm việc nhóm** → Git workflow, code review  
✅ **Tự tin contribute** → Sẵn sàng làm task thực tế  

---

## 📖 Thứ Tự Đọc Khuyến Nghị

### Cho Junior Dev (Chưa có kinh nghiệm web dev)
```
1. QUICK_START.md
2. CODE_CONCEPTS.md ⭐ (đọc kỹ!)
3. ONBOARDING_GUIDE.md
4. diagrams/ (xem diagram)
5. COMMON_PITFALLS.md
6. TASK_EXAMPLES.md (làm Task 1, Task 2)
7. CONTRIBUTION_GUIDE.md
8. ARCHITECTURE_EXPLAINED.md (khi đã quen code)
```

### Cho Mid-Level Dev (Đã có kinh nghiệm Python/FastAPI)
```
1. QUICK_START.md
2. ONBOARDING_GUIDE.md (skim qua phần cơ bản)
3. ARCHITECTURE_EXPLAINED.md ⭐
4. TASK_EXAMPLES.md (làm Task 3, Task 4, Task 5)
5. CONTRIBUTION_GUIDE.md
6. COMMON_PITFALLS.md (reference khi cần)
```

### Cho Senior Dev (Chỉ cần hiểu business logic)
```
1. QUICK_START.md (setup)
2. ONBOARDING_GUIDE.md (chỉ đọc phần Architecture & Code Walkthrough)
3. ARCHITECTURE_EXPLAINED.md (focus vào refactoring roadmap)
4. TASK_EXAMPLES.md (Task 4 - Hexagonal refactor)
5. Đọc code trực tiếp (với folder-structure.txt làm reference)
```

---

## 🛠️ Tools Hỗ Trợ

### Xem Mermaid Diagrams
**Online:**
- https://mermaid.live/ (paste `.mmd` file content)

**VS Code Extension:**
- "Markdown Preview Mermaid Support" by Matt Bierner

### Code Editor
**VS Code Extensions khuyến nghị:**
- Python (Microsoft)
- Pylance (Microsoft)
- Black Formatter (Microsoft)
- GitLens (Eric Amodio)
- Markdown All in One (Yu Zhang)

---

## 📬 Hỏi Đáp & Support

**Nếu gặp vấn đề:**

1. **Lỗi kỹ thuật:** Xem `COMMON_PITFALLS.md`
2. **Không hiểu concept:** Đọc lại `CODE_CONCEPTS.md`
3. **Không biết làm task:** Xem `TASK_EXAMPLES.md`
4. **Vẫn không giải quyết được:** Hỏi team lead hoặc tạo issue trên GitHub

**Channels:**
- GitHub Issues: Bug reports, feature requests
- Team Slack/Discord: Quick questions
- Email team lead: Private concerns

---

## 🎓 Feedback & Improvement

Tài liệu này được tạo cho **developer mới join team**.

**Nếu bạn thấy:**
- ❓ Phần nào chưa rõ → Comment trên GitHub
- 🐛 Lỗi trong ví dụ code → Tạo issue
- 💡 Có ý tưởng cải thiện → Suggest trong PR

**Goal:** Giúp developer tiếp theo onboard nhanh hơn bạn! 🚀

---

## 📊 Checklist Hoàn Thành

Track tiến độ học của bạn:

### Setup & Basics
- [ ] Chạy thành công `update_news.py`
- [ ] Server chạy được, truy cập UI
- [ ] Hiểu workflow: Update → Storage → API → Frontend
- [ ] Đọc xong ONBOARDING_GUIDE.md

### Deep Dive
- [ ] Hiểu từng service làm gì (Feed, Summarization, Storage)
- [ ] Hiểu kiến trúc hiện tại (Monolithic with Services)
- [ ] Hiểu kiến trúc mục tiêu (Hexagonal)
- [ ] Đọc xong ARCHITECTURE_EXPLAINED.md

### Practice
- [ ] Fix được 1 bug (Task 1)
- [ ] Thêm được 1 feature (Task 2 hoặc Task 3)
- [ ] Viết được unit test
- [ ] Tạo được PR đầu tiên

### Contribution
- [ ] Hiểu Git workflow (branch, commit, PR)
- [ ] Biết cách code review
- [ ] Follow code style guidelines
- [ ] Có thể demo dự án cho người khác

**Khi hoàn thành checklist → Bạn đã sẵn sàng contribute full-time! 🎉**

---

## 📝 Ghi Chú

**Ngôn ngữ:**
- Tài liệu: Tiếng Việt (dễ hiểu cho junior)
- Code, comments: Hybrid (English cho code, Vietnamese cho giải thích phức tạp)
- Commit messages: English (chuẩn quốc tế)

**Cập nhật:**
- Tài liệu được tạo: October 29, 2025
- Version: 1.0
- Maintainer: Team Lead

**License:**
- Tài liệu này dành cho internal use (team members only)
- Không public trước khi được approve

---

**Chúc bạn onboarding thành công! Welcome to the team! 🚀🎉**

Nếu có thắc mắc, đừng ngần ngại hỏi. No question is stupid! 💪
