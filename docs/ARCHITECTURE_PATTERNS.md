# Phân tích Kiến trúc - 9 Middleware Patterns

## 📐 Tổng quan

Tài liệu này phân tích 9 middleware patterns theo góc độ **kiến trúc phần mềm** (Software Architecture), phân loại theo các architectural concerns và design patterns.

---

## 🏛️ Phân loại theo Kiến trúc

### 1. Cross-Cutting Concerns (Mối quan tâm xuyên suốt)

**Định nghĩa:** Các chức năng ảnh hưởng đến nhiều tầng/module trong hệ thống, khó gói gọn trong một module riêng.

**Middleware thuộc loại này:**

#### 1.1. Correlation ID ✅

- **Architectural Concern:** Distributed Tracing, Observability
- **Design Pattern:**
  - **Chain of Responsibility** (middleware chain)
  - **Context Object Pattern** (ContextVar lưu trữ correlation ID)
- **Layer:** Infrastructure/Cross-cutting
- **Architectural Style:** Aspect-Oriented Programming (AOP)

**Giải thích:**

```
┌─────────────────────────────────────────┐
│         Application Layers              │
├─────────────────────────────────────────┤
│  Presentation (API Endpoints)           │  ← Correlation ID flows through
│  Business Logic (Services)              │  ← All layers log with ID
│  Data Access (Database)                 │  ← Queries tagged with ID
│  External Services (AI, Feeds)          │  ← API calls include ID
└─────────────────────────────────────────┘
         ↑
    Correlation ID (Cross-cutting concern)
    - Không thuộc layer cụ thể nào
    - Xuyên suốt tất cả layers
```

---

#### 1.2. Audit Logging

- **Architectural Concern:** Compliance, Security, Accountability
- **Design Pattern:**
  - **Observer Pattern** (observe request/response events)
  - **Decorator Pattern** (wrap handlers with audit logic)
- **Layer:** Infrastructure/Cross-cutting
- **Architectural Style:** Event-Driven Architecture

**Giải thích:**

```
Request → Audit Log captures metadata → Handler → Audit Log captures result
          (timestamp, user, action)              (status, latency)
```

---

#### 1.3. Metrics & Observability

- **Architectural Concern:** Monitoring, Performance Analysis
- **Design Pattern:**
  - **Observer Pattern** (collect metrics events)
  - **Singleton Pattern** (metrics registry)
- **Layer:** Infrastructure/Cross-cutting
- **Architectural Style:** Instrumentation Pattern

**Ví dụ:**

```python
# Metrics xuyên suốt tất cả endpoints
REQUEST_COUNT.inc()  # Mọi endpoint đều được đếm
LATENCY.observe(duration)  # Mọi endpoint đều track latency
```

---

### 2. Security Architecture (Kiến trúc bảo mật)

**Định nghĩa:** Các pattern đảm bảo security properties: confidentiality, integrity, availability.

#### 2.1. Security Headers

- **Architectural Concern:** Defense in Depth (bảo vệ nhiều lớp)
- **Design Pattern:**
  - **Template Method Pattern** (set headers template)
- **Layer:** Presentation/Infrastructure
- **Architectural Style:** Security-by-Design

**Security Properties:**

```
┌──────────────────────────────────────────────┐
│  Security Headers (First line of defense)   │
├──────────────────────────────────────────────┤
│  - CSP: Prevent XSS, injection              │
│  - X-Frame-Options: Prevent clickjacking    │
│  - HSTS: Force HTTPS                        │
│  - X-Content-Type: Prevent MIME sniffing    │
└──────────────────────────────────────────────┘
         ↓
    Browser enforces policies
```

---

#### 2.2. CORS (Cross-Origin Resource Sharing)

- **Architectural Concern:** Same-Origin Policy, API Security
- **Design Pattern:**
  - **Policy Pattern** (define CORS policies)
  - **Chain of Responsibility** (preflight checks)
- **Layer:** Presentation/Network
- **Architectural Style:** Policy-Based Architecture

**Architecture:**

```
┌─────────────┐         ┌──────────────┐
│  Frontend   │         │  Backend API │
│  (Origin A) │ ──X──►  │  (Origin B)  │  ← Blocked by browser
└─────────────┘         └──────────────┘
                              ↓
                        CORS Middleware
                              ↓
                    Check allowed origins
                              ↓
                    Add CORS headers
                              ↓
┌─────────────┐         ┌──────────────┐
│  Frontend   │ ──✓──►  │  Backend API │  ← Allowed
└─────────────┘         └──────────────┘
```

---

#### 2.3. Authentication/Authorization

- **Architectural Concern:** Identity & Access Management (IAM)
- **Design Pattern:**
  - **Strategy Pattern** (multiple auth strategies: JWT, API-key, OAuth2)
  - **Decorator Pattern** (protect endpoints with @requires_auth)
  - **Proxy Pattern** (auth middleware = proxy to real handler)
- **Layer:** Application/Security
- **Architectural Style:** Token-Based Authentication, Role-Based Access Control (RBAC)

**Architecture:**

```
┌──────────────────────────────────────────┐
│  Authentication Layer                    │
│  ┌────────────┐  ┌────────────┐         │
│  │ JWT Auth   │  │ API-Key    │  ← Strategies
│  └────────────┘  └────────────┘         │
└──────────────────────────────────────────┘
         ↓
    Verify token/key
         ↓
    Extract user identity
         ↓
┌──────────────────────────────────────────┐
│  Authorization Layer                     │
│  - Check user roles                      │
│  - Enforce permissions                   │
└──────────────────────────────────────────┘
         ↓
    Allow/Deny request
```

---

### 3. Quality of Service (QoS) Patterns

**Định nghĩa:** Đảm bảo chất lượng dịch vụ: availability, reliability, performance.

#### 3.1. Rate Limiting

- **Architectural Concern:** Availability, Fair Usage, Cost Control
- **Design Pattern:**
  - **Token Bucket Pattern** (rate limit algorithm)
  - **Leaky Bucket Pattern** (alternative algorithm)
  - **Throttling Pattern**
- **Layer:** Infrastructure/Gateway
- **Architectural Style:** API Gateway Pattern

**Architecture:**

```
┌─────────────────────────────────────────┐
│  Rate Limiter (Token Bucket)            │
│  ┌───────────────────────────────────┐  │
│  │ Bucket: [▓▓▓▓▓░░░░░]  10/100     │  │
│  │ Refill: +10 tokens/minute         │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
         ↓
    Check tokens available
         ↓
    Consume token (if available)
         ↓
    ┌─────────────┬─────────────┐
    │  Allow      │   Deny      │
    │  (200 OK)   │   (429)     │
    └─────────────┴─────────────┘
```

---

#### 3.2. Request Guard

- **Architectural Concern:** Resource Protection, DOS Prevention
- **Design Pattern:**
  - **Guard Pattern** (validate before processing)
  - **Fail-Fast Pattern** (reject invalid requests early)
- **Layer:** Infrastructure/Gateway
- **Architectural Style:** Input Validation Architecture

**Architecture:**

```
Request arrives
     ↓
┌──────────────────────────────────┐
│  Request Guard (Gateway)         │
│  ┌────────────────────────────┐  │
│  │ Check Content-Length       │  │
│  │ Check Content-Type         │  │
│  │ Check request size         │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
     ↓
   Valid?
     ↓
  ┌───┴───┐
  │  NO   │ → 413/415 Error
  │  YES  │ → Continue to handler
  └───────┘
```

---

#### 3.3. GZip Compression

- **Architectural Concern:** Performance, Bandwidth Optimization
- **Design Pattern:**
  - **Proxy Pattern** (compress responses transparently)
  - **Decorator Pattern** (wrap response with compression)
- **Layer:** Infrastructure/Transport
- **Architectural Style:** Content Negotiation

**Architecture:**

```
Handler returns response (500KB JSON)
     ↓
┌──────────────────────────────────┐
│  GZip Middleware                 │
│  - Check Accept-Encoding header  │
│  - Check response size > min     │
│  - Compress response             │
└──────────────────────────────────┘
     ↓
Compressed response (50KB)
     ↓
Client receives & decompresses
```

---

## 🏗️ Architectural Layers

### Phân tầng theo Clean Architecture / Hexagonal Architecture

```
┌───────────────────────────────────────────────────────────────┐
│  PRESENTATION LAYER (API/UI)                                  │
│  - CORS Middleware                                            │
│  - Security Headers Middleware                                │
├───────────────────────────────────────────────────────────────┤
│  GATEWAY LAYER (API Gateway Pattern)                          │
│  - Rate Limiting                                              │
│  - Request Guard                                              │
│  - Authentication/Authorization                               │
├───────────────────────────────────────────────────────────────┤
│  APPLICATION LAYER (Business Logic)                           │
│  - Handlers/Endpoints                                         │
│  - Services                                                   │
├───────────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE LAYER (Cross-cutting)                         │
│  - Correlation ID                                             │
│  - Audit Logging                                              │
│  - Metrics & Observability                                    │
│  - GZip Compression                                           │
└───────────────────────────────────────────────────────────────┘
```

---

## 📊 Ma trận Kiến trúc

| Middleware           | Architectural Concern      | Design Pattern                          | Layer                    | Architectural Style |
| -------------------- | -------------------------- | --------------------------------------- | ------------------------ | ------------------- |
| **Correlation ID**   | Distributed Tracing        | Chain of Responsibility, Context Object | Infrastructure           | AOP, Cross-cutting  |
| **Security Headers** | Defense in Depth           | Template Method                         | Presentation             | Security-by-Design  |
| **CORS**             | Same-Origin Policy         | Policy Pattern                          | Presentation/Network     | Policy-Based        |
| **Request Guard**    | Resource Protection        | Guard Pattern, Fail-Fast                | Gateway                  | Input Validation    |
| **Rate Limiting**    | Availability, QoS          | Token Bucket, Throttling                | Gateway                  | API Gateway         |
| **Auth**             | Identity & Access          | Strategy, Proxy                         | Application/Security     | IAM, RBAC           |
| **Audit Log**        | Compliance, Accountability | Observer, Decorator                     | Infrastructure           | Event-Driven        |
| **Metrics**          | Observability              | Observer, Singleton                     | Infrastructure           | Instrumentation     |
| **GZip**             | Performance                | Proxy, Decorator                        | Infrastructure/Transport | Content Negotiation |

---

## 🎯 Architectural Concerns (Theo Quality Attributes)

### 1. Security (Bảo mật)

- **Security Headers** → Prevent common web attacks
- **CORS** → Control cross-origin access
- **Auth** → Identity & access control
- **Request Guard** → Input validation, DOS prevention

**Architecture:** Defense in Depth (nhiều lớp bảo vệ)

---

### 2. Observability (Quan sát hệ thống)

- **Correlation ID** → Request tracing
- **Audit Log** → Activity tracking
- **Metrics** → System monitoring

**Architecture:** Three Pillars of Observability (Logs, Metrics, Traces)

---

### 3. Reliability (Độ tin cậy)

- **Rate Limiting** → Prevent overload
- **Request Guard** → Resource protection

**Architecture:** Resilience Patterns

---

### 4. Performance (Hiệu năng)

- **GZip** → Bandwidth optimization
- **Rate Limiting** → Fair resource allocation

**Architecture:** Performance Optimization Patterns

---

### 5. Compliance (Tuân thủ)

- **Audit Log** → Regulatory requirements
- **Auth** → Access control audit trail

**Architecture:** Compliance-by-Design

---

## 🏛️ Architectural Styles liên quan

### 1. Layered Architecture (Kiến trúc phân tầng)

```
┌─────────────────────────┐
│  Presentation Layer     │  ← CORS, Security Headers
├─────────────────────────┤
│  Application Layer      │  ← Auth, Business Logic
├─────────────────────────┤
│  Infrastructure Layer   │  ← Correlation ID, Metrics, Audit Log
└─────────────────────────┘
```

**Middleware phục vụ:** Tất cả 9 patterns đều là horizontal concerns trong Layered Architecture.

---

### 2. Microservices Architecture (Kiến trúc microservices)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Service A   │────→│  Service B   │────→│  Service C   │
└──────────────┘     └──────────────┘     └──────────────┘
       ↓                    ↓                    ↓
   Correlation ID propagates across services
       ↓                    ↓                    ↓
┌──────────────────────────────────────────────────────────┐
│  Distributed Tracing (Correlation ID)                    │
│  Centralized Logging (Audit Logs)                        │
│  Metrics Aggregation (Prometheus)                        │
└──────────────────────────────────────────────────────────┘
```

**Middleware quan trọng:**

- **Correlation ID** → Trace requests qua nhiều services
- **Metrics** → Monitor distributed system
- **Auth** → Service-to-service authentication

---

### 3. API Gateway Pattern

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       ↓
┌──────────────────────────────────────┐
│         API Gateway                  │
│  ┌────────────────────────────────┐  │
│  │ Rate Limiting                  │  │  ← QoS
│  │ Authentication                 │  │  ← Security
│  │ Request Validation (Guard)     │  │  ← Input validation
│  │ CORS                           │  │  ← Cross-origin
│  │ Metrics Collection             │  │  ← Observability
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
       ↓
┌──────────────────┐
│  Backend Service │
└──────────────────┘
```

**Middleware API Gateway cần:**

- Rate Limiting
- Auth
- Request Guard
- CORS
- Metrics

---

### 4. Event-Driven Architecture (Kiến trúc hướng sự kiện)

```
Request Event
     ↓
┌──────────────────────────────────┐
│  Event Bus                       │
│  ┌────────────────────────────┐  │
│  │ Audit Logger (subscriber)  │  │  ← Observe events
│  │ Metrics Collector          │  │  ← Observe events
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

**Middleware event-driven:**

- Audit Log (subscribe to request/response events)
- Metrics (subscribe to all events)

---

### 5. Aspect-Oriented Programming (AOP)

```
┌────────────────────────────────────────────┐
│  Core Business Logic                       │
│  - Fetch news                              │
│  - Summarize articles                      │
│  - Save results                            │
└────────────────────────────────────────────┘
         ↓
    Aspects (Cross-cutting concerns):
         ↓
┌────────────────────────────────────────────┐
│  - Logging (Correlation ID in all logs)   │
│  - Security (Auth checks)                  │
│  - Performance (Metrics collection)        │
│  - Audit (Activity tracking)               │
└────────────────────────────────────────────┘
```

**Middleware = Aspects:**

- Correlation ID
- Auth
- Metrics
- Audit Log

---

## 📚 Design Patterns chi tiết

### 1. Chain of Responsibility Pattern

**Middleware nào dùng:** Tất cả (middleware chain)

**Cách hoạt động:**

```python
# FastAPI middleware chain
app.add_middleware(CorrelationIdMiddleware)     # Handler 1
app.add_middleware(SecurityHeadersMiddleware)   # Handler 2
app.add_middleware(CORSMiddleware)              # Handler 3
app.add_middleware(RateLimitMiddleware)         # Handler 4

# Request flows through chain:
Request → Handler 1 → Handler 2 → Handler 3 → Handler 4 → Endpoint
       ← Handler 1 ← Handler 2 ← Handler 3 ← Handler 4 ← Response
```

**Lợi ích:**

- Decouple handlers (mỗi middleware độc lập)
- Dễ thêm/bỏ middleware
- Flexible ordering

---

### 2. Decorator Pattern

**Middleware nào dùng:** GZip, Audit Log, Metrics

**Cách hoạt động:**

```python
# Decorator wraps original response
original_response = handler(request)
decorated_response = decorator(original_response)  # Add behavior

# GZip example:
response = handler()  # 500KB JSON
gzip_response = compress(response)  # 50KB compressed
```

**Lợi ích:**

- Add behavior without modifying original code
- Transparent to client (client không biết có compression)

---

### 3. Strategy Pattern

**Middleware nào dùng:** Auth

**Cách hoạt động:**

```python
# Multiple auth strategies
class JWTAuthStrategy:
    def authenticate(self, request):
        token = extract_jwt(request)
        return verify_jwt(token)

class APIKeyAuthStrategy:
    def authenticate(self, request):
        key = extract_api_key(request)
        return verify_api_key(key)

# Choose strategy at runtime
auth_strategy = JWTAuthStrategy() if use_jwt else APIKeyAuthStrategy()
user = auth_strategy.authenticate(request)
```

**Lợi ích:**

- Support multiple auth methods
- Easy to add new strategies
- Testable in isolation

---

### 4. Observer Pattern

**Middleware nào dùng:** Audit Log, Metrics

**Cách hoạt động:**

```python
# Request/Response events
class RequestEvent:
    def __init__(self, request):
        self.request = request

# Observers subscribe to events
audit_logger.subscribe(RequestEvent)
metrics_collector.subscribe(RequestEvent)

# When event occurs, notify all observers
event = RequestEvent(request)
for observer in observers:
    observer.handle(event)
```

**Lợi ích:**

- Decouple event source from handlers
- Multiple observers per event
- Easy to add new observers

---

### 5. Proxy Pattern

**Middleware nào dùng:** Auth, GZip

**Cách hoạt động:**

```python
# Middleware acts as proxy
class AuthProxy:
    def __init__(self, real_handler):
        self.real_handler = real_handler

    def handle(self, request):
        # Pre-processing (auth check)
        if not self.is_authenticated(request):
            return 401

        # Delegate to real handler
        response = self.real_handler.handle(request)

        # Post-processing (optional)
        return response
```

**Lợi ích:**

- Control access to real handler
- Add behavior transparently
- Lazy initialization, caching, logging

---

## 🔍 Architectural Trade-offs

### Security vs Performance

```
More Security → More Overhead
- Auth checks add latency (~10-50ms per request)
- Rate limiting requires Redis lookups
- Request validation adds CPU cycles

Trade-off:
✅ Accept small latency for security
❌ Don't over-engineer (e.g., encrypt everything = slow)
```

---

### Observability vs Privacy

```
More Logs/Metrics → More Data Exposure
- Audit logs contain sensitive info (user actions)
- Correlation ID can link requests (privacy concern)

Trade-off:
✅ Log metadata, not sensitive data (PII)
✅ Use correlation ID with short TTL
❌ Don't log passwords, tokens, PII
```

---

### Flexibility vs Complexity

```
More Middleware → More Complexity
- 9 middleware = harder to debug
- Ordering matters (bugs if wrong order)

Trade-off:
✅ Only add middleware you need
✅ Document middleware order
❌ Don't add middleware "just in case"
```

---

## 🎓 Architectural Principles tuân theo

### 1. Separation of Concerns (SoC)

- Mỗi middleware làm **một việc** (Single Responsibility)
- Correlation ID: chỉ lo tracing
- Auth: chỉ lo authentication/authorization
- Rate Limiting: chỉ lo throttling

---

### 2. Don't Repeat Yourself (DRY)

- Correlation ID middleware → **tất cả** logs có ID (không cần log manually mỗi nơi)
- Security Headers middleware → **tất cả** responses có headers (không set manually mỗi endpoint)

---

### 3. Open/Closed Principle (OCP)

- Hệ thống **open for extension** (thêm middleware mới)
- Hệ thống **closed for modification** (không sửa core code)

```python
# Thêm middleware mới không cần sửa core app
app.add_middleware(NewCustomMiddleware)  # ✅ Extension
# Không cần sửa app.py logic  # ✅ Closed for modification
```

---

### 4. Dependency Inversion Principle (DIP)

- High-level modules (business logic) không phụ thuộc low-level modules (middleware)
- Cả hai phụ thuộc abstractions (FastAPI middleware interface)

```python
# Business logic không biết middleware
def fetch_news():
    # Just fetch, correlation ID tự động có trong logs
    logger.info("Fetching news")
```

---

### 5. Single Responsibility Principle (SRP)

- Mỗi middleware có **một trách nhiệm duy nhất**
- CorrelationIdMiddleware: Generate/propagate ID
- SecurityHeadersMiddleware: Add security headers
- RateLimitMiddleware: Enforce rate limits

---

## 🏆 Best Practices Architecture

### 1. Middleware Ordering (Thứ tự kiến trúc)

```python
# ĐÚNG thứ tự:
app.add_middleware(CorrelationIdMiddleware)    # 1. Tracing first
app.add_middleware(SecurityHeadersMiddleware)  # 2. Security
app.add_middleware(CORSMiddleware)             # 3. Cross-origin
app.add_middleware(RequestGuardMiddleware)     # 4. Input validation
app.add_middleware(RateLimitMiddleware)        # 5. Throttling
app.add_middleware(AuthMiddleware)             # 6. Authentication
app.add_middleware(AuditLogMiddleware)         # 7. Logging
app.add_middleware(MetricsMiddleware)          # 8. Metrics
app.add_middleware(GZipMiddleware)             # 9. Compression (last)

# Lý do:
# - Correlation ID đầu tiên → all logs có ID
# - Security sớm → block malicious requests early
# - GZip cuối cùng → compress final response
```

---

### 2. Configuration Management

```python
# Centralized config (12-factor app)
class Settings(BaseSettings):
    # Middleware configs
    CORS_ORIGINS: list[str] = ["*"]
    RATE_LIMIT_PER_MINUTE: int = 60
    MAX_REQUEST_SIZE: int = 10 * 1024 * 1024
    AUTH_ENABLED: bool = True

    class Config:
        env_file = ".env"

settings = Settings()

# Use in middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS  # From config
)
```

---

### 3. Error Handling Strategy

```python
# Centralized exception handler (works with all middleware)
@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    correlation_id = get_current_correlation_id()  # From middleware

    return JSONResponse(
        status_code=500,
        content={
            "error": str(exc),
            "correlation_id": correlation_id,  # Include for debugging
        }
    )
```

---

### 4. Testing Strategy

```python
# Test middleware in isolation (unit test)
def test_correlation_middleware():
    app = FastAPI()
    app.add_middleware(CorrelationIdMiddleware)
    client = TestClient(app)

    response = client.get("/test")
    assert "X-Correlation-ID" in response.headers

# Test middleware integration (integration test)
def test_full_middleware_stack():
    # Test với tất cả middleware
    response = client.get("/api/refresh")
    assert "X-Correlation-ID" in response.headers
    assert "X-Content-Type-Options" in response.headers
    # ...
```

---

## 📖 Tổng kết

### Phân loại theo Kiến trúc

| Loại                       | Middleware                         | Mục đích                  |
| -------------------------- | ---------------------------------- | ------------------------- |
| **Cross-Cutting Concerns** | Correlation ID, Audit Log, Metrics | Observability, Compliance |
| **Security Architecture**  | Security Headers, CORS, Auth       | Defense in Depth          |
| **Quality of Service**     | Rate Limiting, Request Guard, GZip | Availability, Performance |

---

### Design Patterns chính

1. **Chain of Responsibility** → Tất cả middleware
2. **Decorator** → GZip, Audit Log, Metrics
3. **Strategy** → Auth (multiple strategies)
4. **Observer** → Audit Log, Metrics (event-driven)
5. **Proxy** → Auth, GZip (control access)

---

### Architectural Styles

1. **Layered Architecture** → Presentation, Application, Infrastructure layers
2. **Microservices** → Correlation ID for distributed tracing
3. **API Gateway** → Rate Limiting, Auth, Request Guard
4. **Event-Driven** → Audit Log, Metrics (event subscribers)
5. **AOP** → Cross-cutting concerns (logging, security, metrics)

---

### Key Takeaways

1. **Middleware = Cross-cutting concerns** trong kiến trúc phần mềm
2. **9 patterns** giải quyết 3 architectural concerns chính: **Security, Observability, QoS**
3. Tuân theo **SOLID principles** và **design patterns** cổ điển
4. Là nền tảng cho **production-ready architecture**

---

**Kết luận:** 9 middleware patterns này không phải "add-ons" mà là **core architectural components** của một hệ thống production hiện đại. Chúng implement các design patterns cổ điển (Gang of Four) và giải quyết các architectural concerns quan trọng (Security, Observability, Reliability). 🏛️
