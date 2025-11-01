# Hướng Dẫn Chạy Chương Trình (Java Spring Boot)

## Yêu cầu hệ thống

- **JDK 17 trở lên** (đã có JDK 22 trên máy: `C:\Program Files\Java\jdk-22`)
- **Maven** (đã cài)
- **PowerShell** (Windows)

## Các bước chạy nhanh

### 1. Set JAVA_HOME (nếu chưa đúng phiên bản)

Mở PowerShell và chạy:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-22'
$env:PATH = "$env:JAVA_HOME\bin;${env:PATH}"
java -version
```

### 2. Build project

```powershell
cd E:\Viscode\Project_Big\Summarize-with-AI\java-spring
mvn -DskipTests package
```

Kết quả: file JAR ở `target\summarizer-0.0.1-SNAPSHOT.jar`

### 3. Chạy ứng dụng

**Cách 1: Chạy trực tiếp (xem log console)**

```powershell
java -jar target\summarizer-0.0.1-SNAPSHOT.jar
```

- Nhấn `Ctrl+C` để dừng.

**Cách 2: Dùng script tự động** (khuyến nghị)

```powershell
.\run-app.ps1
```

Script sẽ tự động set JAVA_HOME, build (nếu cần), và chạy app.

### 4. Truy cập ứng dụng

**Frontend (giao diện web)**

- Mở trình duyệt: **http://localhost:8000/**

**Backend API**

- Health check: http://localhost:8000/healthz
- Lấy danh sách summaries: http://localhost:8000/api/summaries
- Tạo summaries mới (POST): http://localhost:8000/api/summarize

### 5. Chạy pipeline cập nhật tin (giống `update_news.py`)

```powershell
java -jar target\summarizer-0.0.1-SNAPSHOT.jar --update.now=true --update.top=25
```

Hoặc dùng script:

```powershell
.\run-update.ps1
```

Kết quả: file `data\outputs\summaries.json` được tạo/cập nhật.

### 6. Cấu hình Gemini API (nếu muốn gọi AI thật)

**Cách 1: Thay đổi trong file**

- Mở: `src\main\resources\application.properties`
- Sửa:
  ```properties
  gemini.provider=google
  gemini.model=gemini-pro
  gemini.apiKey=YOUR_API_KEY_HERE
  gemini.useApiKeyAsQuery=true
  ```

**Cách 2: Truyền qua dòng lệnh** (không lưu vào mã)

```powershell
java -jar target\summarizer-0.0.1-SNAPSHOT.jar `
  --gemini.provider=google `
  --gemini.model=gemini-pro `
  --gemini.apiKey=YOUR_KEY `
  --gemini.useApiKeyAsQuery=true
```

### 7. Dừng ứng dụng

**Nếu chạy foreground**: `Ctrl+C`

**Nếu chạy background**:

```powershell
# Tìm process
Get-Process java

# Dừng bằng PID
Stop-Process -Id <PID> -Force

# Hoặc dừng tất cả java (cẩn thận)
Get-Process java | Stop-Process -Force
```

## Kiểm tra nhanh (PowerShell)

### Test health endpoint

```powershell
Invoke-RestMethod -Uri http://localhost:8000/healthz
```

### Test summaries endpoint

```powershell
Invoke-RestMethod -Uri http://localhost:8000/api/summaries
```

### Test POST summarize (ví dụ)

```powershell
$body = @(
  @{
    title = 'Tin mẫu'
    url = 'https://example.com'
    content = 'Nội dung bài viết cần tóm tắt...'
  }
) | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8000/api/summarize `
  -Method POST `
  -Body $body `
  -ContentType 'application/json'
```

## Vấn đề thường gặp

### Lỗi: UnsupportedClassVersionError

- **Nguyên nhân**: Java runtime quá cũ (< 17)
- **Giải pháp**: Set `JAVA_HOME` tới JDK 17/21/22 (xem bước 1)

### Lỗi 500 khi gọi /api/summaries

- **Nguyên nhân**: File `data\outputs\summaries.json` không tồn tại
- **Giải pháp**: Chạy pipeline update (bước 5) hoặc copy file mẫu

### Port 8000 bị chiếm

- **Giải pháp**: Thay đổi port
  ```powershell
  java -jar target\summarizer-0.0.1-SNAPSHOT.jar --server.port=8080
  ```

### Không kết nối được Gemini API

- Kiểm tra `gemini.apiKey` đã đúng chưa
- Kiểm tra `gemini.provider=google` và endpoint
- Xem log để biết lỗi chi tiết

## File dữ liệu quan trọng

- **Summaries output**: `data\outputs\summaries.json`
- **Sample feed input**: `data\raw\techmeme_sample_full.json`
- **Logs** (nếu chạy script): `logs\app.log`

## Scripts tiện ích

- `run-app.ps1` - Build và chạy app (dev mode)
- `run-update.ps1` - Chạy pipeline cập nhật tin và thoát

## Rebuild sau khi sửa code

```powershell
mvn clean package
java -jar target\summarizer-0.0.1-SNAPSHOT.jar
```

## Tài liệu kỹ thuật

- Chi tiết API endpoints: xem `README_JAVA.md`
- Source code: `src\main\java\com\example\summarizer\`
- Frontend: `src\main\resources\static\` (HTML/JS/CSS)
