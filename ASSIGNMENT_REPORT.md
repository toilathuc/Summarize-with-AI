**BÁO CÁO BÀI TẬP LỚN**

# 1. **Thông tin chung**
- **Nhóm:** [Tên Nhóm]
- **Thành viên:**
  - [Tên Sinh Viên 1] ([Mã SV])
  - [Tên Sinh Viên 2] ([Mã SV])
- **Ứng dụng:** Hệ thống tổng hợp tin tức AI (AI Summarizer)
- **Repo gốc (Phiên bản cũ - dev):** https://github.com/toilathuc/Summarize-with-AI/tree/dev
- **Repo bản cải tiến (Phiên bản mới - test):** https://github.com/toilathuc/Summarize-with-AI/tree/test

# 2. **Thông tin chi tiết**

|**STT**|**Vấn đề**|**Giải pháp**|**Kết quả**|**Minh chứng**|
| :-: | :-: | :-: | :-: | :-: |
|1|**Blocking I/O & Timeout:** Quy trình làm mới chạy trên luồng chính, gây treo trình duyệt và timeout (504) nếu xử lý quá 30s.|**Asynchronous Producer-Consumer:** Tách biệt luồng nhận yêu cầu và luồng xử lý. Sử dụng `Virtual Threads` để xử lý song song hàng loạt tin tức.|Loại bỏ hoàn toàn tình trạng treo trình duyệt. Tăng tốc độ xử lý lên gấp 5 lần.|**`SummarizationOrchestrator.java`**: Sử dụng `Executors.newVirtualThreadPerTaskExecutor()` và `CompletableFuture`.|
|2|**Race Conditions:** Nhiều người dùng bấm "Refresh" cùng lúc gây khởi chạy nhiều tiến trình trùng lặp, lãng phí tài nguyên.|**Distributed Locking:** Sử dụng Redis (`SETNX`) để tạo khóa phân tán, đảm bảo chỉ duy nhất một tiến trình chạy tại một thời điểm.|Đảm bảo tính nhất quán dữ liệu, loại bỏ xử lý dư thừa.|**`RefreshCoordinator.java`**: Phương thức `tryStartScheduled` gọi `lockService.tryLock(REFRESH_LOCK)`.|
|3|**Cascading Failures:** Lỗi từ API bên thứ 3 (Google/Firecrawl) làm treo các luồng xử lý, kéo theo sập toàn bộ hệ thống.|**Bulkhead & Rate Limiting:** Sử dụng `Semaphore` để cô lập tài nguyên và giới hạn số lượng request đồng thời.|Hệ thống vẫn hoạt động ổn định ngay cả khi API đối tác gặp sự cố hoặc phản hồi chậm.|**`SummarizationOrchestrator.java`**: Sử dụng `semaphore.acquire()` bao quanh logic gọi API.|
|4|**Fragile Scraping:** Phụ thuộc vào một nguồn lấy tin duy nhất (Firecrawl), dễ bị chặn hoặc lỗi.|**Chain of Responsibility:** Thiết lập chuỗi xử lý dự phòng đa lớp: Firecrawl -> Jina AI -> Jsoup Local.|Tỷ lệ lấy tin thành công đạt >99%, không bị gián đoạn dịch vụ.|**`CrawlClient.java`**: Logic fallback `try-catch` gọi lần lượt các provider.|
|5|**Hiệu năng đọc kém:** Truy vấn trực tiếp vào Database (Disk I/O) cho mọi request đọc danh sách tin tức.|**Cache-Aside:** Sử dụng Redis làm bộ nhớ đệm. Ưu tiên đọc từ Cache, chỉ truy vấn DB khi Cache Miss.|Giảm độ trễ đọc từ ~500ms xuống < 5ms. Giảm tải cho Database.|**`NewsCacheService.java`**: Phương thức `getSummaries` kiểm tra Redis trước khi gọi DB.|
|6|**Lỗi mạng tạm thời:** Các lỗi mạng thoáng qua (transient errors) làm hỏng cả quy trình xử lý batch.|**Resilience Patterns (Retry & Circuit Breaker):** Tự động thử lại với chiến lược Exponential Backoff. Ngắt mạch nếu lỗi lặp lại.|Tự động khắc phục các lỗi mạng nhỏ mà không cần can thiệp thủ công.|**`CrawlClient.java`**: Vòng lặp `while` với `Thread.sleep` tăng dần theo số lần thử.|
|7|**Khó kiểm thử & Phụ thuộc cứng:** Code gắn chặt với implementation cụ thể của AI Provider, khó mock để test.|**Strategy Pattern:** Định nghĩa giao diện chung cho các AI Provider, cho phép thay đổi chiến lược (Mock/Real) lúc runtime.|Dễ dàng viết Unit Test và chuyển đổi nhà cung cấp AI mà không sửa code lõi.|**`GeminiClient.java`**: Implement interface chung, có thể cấu hình switch qua Mock.|
|8|**Logic phức tạp & Khó bảo trì:** Logic nghiệp vụ bị phân tán ở nhiều nơi (Controller, Service, Utils).|**Facade Pattern:** Tập trung toàn bộ quy trình phức tạp vào một lớp điều phối duy nhất (`SummarizationOrchestrator`).|Code gọn gàng, dễ đọc, dễ bảo trì. Controller chỉ cần gọi 1 hàm duy nhất.|**`SummarizationOrchestrator.java`**: Phương thức `summarize` che giấu toàn bộ độ phức tạp bên dưới.|
|9|**Blocking Thread:** Thread chính bị block khi chờ kết quả từ các tác vụ I/O lâu (như gọi AI).|**Future/Promise Pattern:** Sử dụng `CompletableFuture` để đại diện cho kết quả trong tương lai, giúp luồng chính không bị chặn.|Tối ưu hóa tài nguyên CPU, cho phép thực hiện các tác vụ khác trong khi chờ đợi.|**`SummarizationOrchestrator.java`**: Trả về `List<Future<SummaryResult>>`.|
|10|**Code lẫn lộn (Cross-cutting Concerns):** Code log, transaction, caching bị trộn lẫn vào code nghiệp vụ.|**Proxy Pattern (AOP):** Sử dụng Spring AOP để tách biệt các mối quan tâm cắt ngang (logging, caching) ra khỏi logic chính.|Code nghiệp vụ trong sáng (Clean Code), tập trung vào logic lõi.|**`RefreshCoordinator.java`**: Sử dụng annotation `@Async`, `@Log` (được xử lý bởi Proxy).|
