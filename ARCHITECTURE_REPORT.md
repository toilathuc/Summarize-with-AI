# Báo Cáo Phân Tích Kiến Trúc Phần Mềm - Dự Án Summarize-with-AI

Tài liệu này cung cấp cái nhìn sâu sắc về các quyết định kiến trúc, mẫu thiết kế (Design Patterns) và giải pháp kỹ thuật được áp dụng trong dự án. Mục tiêu là giải thích lý do lựa chọn (Why), cách thức hoạt động (How) và bằng chứng về hiệu quả (Evidence/Benefits) của từng giải pháp.

---

## 1. Kiến Trúc Tổng Thể: Hexagonal Architecture (Ports and Adapters)

Dự án áp dụng kiến trúc **Hexagonal (Lục giác)**, hay còn gọi là **Ports and Adapters**, kết hợp với nền tảng Spring Boot.

### Tại sao lựa chọn kiến trúc này?
Trong các ứng dụng truyền thống, logic nghiệp vụ thường bị dính chặt với Database hoặc các API bên ngoài. Điều này khiến việc thay đổi công nghệ (ví dụ: đổi Database) trở nên cực kỳ rủi ro và tốn kém. Hexagonal Architecture giải quyết vấn đề này bằng cách đảo ngược sự phụ thuộc.

### Giải thích chi tiết & Bằng chứng hiệu quả:
*   **Sự tách biệt tuyệt đối (Decoupling):**
    *   *Cách làm:* Logic nghiệp vụ cốt lõi (Domain) được đặt ở trung tâm và không phụ thuộc vào bất kỳ framework nào. Giao tiếp với bên ngoài thông qua các "Cổng" (Interfaces/Ports).
    *   *Bằng chứng hiệu quả:* Hệ thống hiện tại đang sử dụng Google Gemini để tóm tắt. Nếu ngày mai cần chuyển sang OpenAI GPT-4, ta chỉ cần viết một "Adapter" mới triển khai cổng `SummarizerPort` mà **không cần sửa bất kỳ dòng code nghiệp vụ nào**. Điều này giảm thiểu rủi ro lỗi hồi quy (regression bugs) xuống mức thấp nhất.

*   **Khả năng kiểm thử (Testability):**
    *   *Cách làm:* Vì logic nghiệp vụ chỉ giao tiếp qua Interface, ta có thể dễ dàng tạo các bản giả lập (Mock) cho Database hoặc API bên ngoài khi chạy Unit Test.
    *   *Bằng chứng hiệu quả:* Các bài test nghiệp vụ chạy cực nhanh (mili-giây) vì không cần kết nối mạng thực tế đến Google hay Database, giúp phát hiện lỗi sớm ngay trong quá trình phát triển.

---

## 2. Các Mẫu Thiết Kế (Design Patterns) Đã Áp Dụng

### 2.1. Facade Pattern (Mặt tiền)

*   **Mô tả:** Hệ thống sử dụng một lớp điều phối trung gian để che giấu sự phức tạp của quy trình xử lý tin tức.
*   **Tại sao cần thiết?** Quy trình "Làm mới tin tức" bao gồm rất nhiều bước nhỏ: (1) Gọi API lấy tin -> (2) Lọc tin trùng -> (3) Kiểm tra Cache -> (4) Gọi AI tóm tắt -> (5) Lưu Database. Nếu để Controller xử lý việc này, code sẽ rất rối và khó bảo trì.
*   **Hiệu quả:** Controller chỉ cần gọi một hàm duy nhất `refresh()`. Điều này giúp giảm sự phụ thuộc giữa giao diện người dùng và logic xử lý phức tạp bên dưới, làm cho code sạch (Clean Code) và dễ đọc hơn.

### 2.2. Strategy Pattern (Chiến lược)

*   **Mô tả:** Định nghĩa một "họ" các thuật toán và cho phép hoán đổi chúng linh hoạt.
*   **Tại sao cần thiết?** Trong bối cảnh AI thay đổi nhanh chóng, việc phụ thuộc cứng vào một nhà cung cấp (như Google) là rủi ro.
*   **Hiệu quả:** Hệ thống định nghĩa một "Chiến lược tóm tắt" chung. Hiện tại đang dùng chiến lược "Gemini". Trong tương lai, có thể cấu hình để tự động chuyển sang chiến lược "Local LLM" (chạy offline) nếu mạng bị mất, hoặc chuyển sang "GPT-4" cho các khách hàng VIP, mà không phá vỡ cấu trúc hệ thống.

### 2.3. Circuit Breaker Pattern (Cầu dao ngắt mạch)

*   **Mô tả:** Tự động ngắt kết nối đến dịch vụ bên ngoài (AI API) nếu phát hiện lỗi liên tục.
*   **Tại sao cần thiết?** Các dịch vụ AI thường có độ trễ cao hoặc đôi khi bị quá tải. Nếu cứ cố gắng gửi request khi dịch vụ đang chết, hệ thống của chúng ta sẽ bị treo theo (do chờ timeout) và lãng phí tài nguyên.
*   **Hiệu quả:**
    *   *Bảo vệ hệ thống:* Khi AI API bị lỗi 5 lần liên tiếp, "cầu dao" sẽ mở ra. Các request tiếp theo sẽ bị từ chối ngay lập tức (Fail Fast) thay vì bắt người dùng chờ 30 giây rồi mới báo lỗi.
    *   *Tự phục hồi:* Sau một khoảng thời gian, hệ thống sẽ tự động thử kết nối lại.

### 2.4. Adapter Pattern (Bộ chuyển đổi)

*   **Mô tả:** Chuyển đổi giao diện của một lớp có sẵn thành một giao diện khác mà hệ thống mong đợi.
*   **Hiệu quả:** Được sử dụng để xử lý thời gian hệ thống. Thay vì gọi trực tiếp `LocalDateTime.now()`, hệ thống dùng một Adapter. Điều này cho phép "đóng băng" thời gian trong các bài test, giúp kiểm thử các tính năng phụ thuộc thời gian (như lập lịch chạy job) chính xác tuyệt đối.

---

## 3. Middleware & Giải Pháp Kỹ Thuật Nâng Cao

### 3.1. Distributed Locking với Redis (Khóa phân tán)

*   **Vấn đề:** Khi triển khai hệ thống trên nhiều server (Scaling), rủi ro lớn nhất là các server cùng chạy một tác vụ (ví dụ: cùng lấy tin tức một lúc) gây trùng lặp dữ liệu và lãng phí tài nguyên.
*   **Giải pháp:** Sử dụng Redis làm trung gian để quản lý khóa.
*   **Bằng chứng hiệu quả:** Trước khi thực hiện tác vụ nặng, server phải "xin khóa" từ Redis. Chỉ server nào giữ khóa mới được chạy. Điều này đảm bảo tính **nhất quán dữ liệu (Data Consistency)** tuyệt đối trong môi trường phân tán.

### 3.2. Java 21 Virtual Threads (Luồng ảo)

*   **Vấn đề:** Mô hình truyền thống (1 request = 1 luồng hệ điều hành) rất tốn kém bộ nhớ. Với các ứng dụng gọi AI API (thường mất 5-10 giây để phản hồi), các luồng này bị "treo" chờ đợi, làm giảm khả năng chịu tải của server.
*   **Giải pháp:** Sử dụng Virtual Threads (Project Loom) của Java 21.
*   **Bằng chứng hiệu quả:**
    *   *Hiệu suất vượt trội:* Virtual Threads cực nhẹ (chỉ tốn vài KB bộ nhớ so với vài MB của luồng thường).
    *   *Khả năng mở rộng (Scalability):* Hệ thống có thể xử lý hàng nghìn request tóm tắt đồng thời mà không bị lỗi "Cạn kiệt luồng" (Thread Exhaustion), điều mà các phiên bản Java cũ không làm được.

### 3.3. Correlation ID & Observability (Khả năng quan sát)

*   **Vấn đề:** Trong hệ thống bất đồng bộ, rất khó để biết một lỗi xảy ra bắt nguồn từ request nào của người dùng.
*   **Giải pháp:** Tự động gán một mã định danh duy nhất (Correlation ID) cho mỗi request ngay từ khi nó đi vào hệ thống.
*   **Hiệu quả:** Giảm thời gian debug (MTTR - Mean Time To Recovery). Khi có lỗi, lập trình viên chỉ cần search ID này trong log là thấy toàn bộ hành trình của request, từ lúc nhận tin đến lúc lưu vào DB.

### 3.4. Caching Strategy (Chiến lược bộ nhớ đệm)

*   **Vấn đề:** Gọi AI tóm tắt rất tốn kém (chi phí tính theo token) và chậm.
*   **Giải pháp:** Lưu kết quả tóm tắt vào Redis với thời gian sống (TTL) hợp lý.
*   **Hiệu quả:**
    *   *Tiết kiệm chi phí:* Không bao giờ tóm tắt lại một bài báo đã từng xử lý.
    *   *Trải nghiệm người dùng:* Những người dùng sau khi xem cùng một tin tức sẽ nhận được kết quả ngay lập tức (độ trễ < 10ms) thay vì phải chờ AI xử lý.

### 3.5. Asynchronous Processing (Xử lý Bất đồng bộ)

*   **Vấn đề:** Các tác vụ như "Làm mới tin tức" hoặc "Gọi AI" thường mất nhiều thời gian (Long-running tasks). Nếu xử lý đồng bộ (Synchronous), người dùng sẽ phải chờ đợi rất lâu (loading xoay vòng) và server bị chiếm dụng tài nguyên.
*   **Giải pháp:**
    *   **Spring `@Async`:** Áp dụng cho quy trình `runAsyncRefresh`. Khi Controller gọi hàm này, nó trả về kết quả ngay lập tức (Accepted) để người dùng không phải chờ, trong khi server xử lý ngầm bên dưới.
    *   **CompletableFuture & Virtual Threads:** Trong `SummarizationOrchestrator`, hệ thống chia nhỏ danh sách bài báo thành các lô (batch) và xử lý song song.
*   **Bằng chứng hiệu quả:**
    *   *Non-blocking UI:* Giao diện người dùng luôn mượt mà, không bị đơ khi bấm nút "Refresh".
    *   *Tối ưu thời gian:* Thay vì tóm tắt tuần tự 10 bài báo (mất 10 x 5s = 50s), hệ thống tóm tắt song song 3 lô cùng lúc (chỉ mất khoảng 15-20s), giảm hơn 50% thời gian chờ đợi.
