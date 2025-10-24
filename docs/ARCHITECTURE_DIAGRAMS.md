# ARCHITECTURE DIAGRAMS - Summarize-with-AI

This document visualizes the class structure and operational flows using Mermaid diagrams.

**How to view:**
- In VS Code: Install extension "Markdown Preview Mermaid Support" or "Mermaid Preview"
- On GitHub: Mermaid is natively rendered in markdown

---

## 1. Class Diagram (Core Domain & Services)

```mermaid
classDiagram
    %% Domain Models
    class FeedArticle {
        +String title
        +String techmeme_url
        +String original_url
        +String summary_text
        +String content_text
        +String hash
        +Dict raw
        +from_dict(payload) FeedArticle
        +to_summary_request() SummaryRequest
    }
    
    class SummaryRequest {
        +String title
        +String url
        +String text
        +to_prompt_dict() Dict
    }
    
    class SummaryResult {
        +String title
        +String url
        +List~String~ bullets
        +String why_it_matters
        +String type
        +List~String~ key_commands
        +List~String~ caveats
        +from_dict(payload) SummaryResult
        +to_dict() Dict
    }
    
    class SummaryPayload {
        +List~SummaryResult~ summaries
        +Dict extra
        +to_dict() Dict
        +empty() SummaryPayload
        +from_existing(summaries) SummaryPayload
    }
    
    %% Configuration
    class Settings {
        +GeminiConfig gemini
        +FeedConfig feed
        +StorageConfig storage
        +String summary_prompt_template
    }
    
    class GeminiConfig {
        +String api_key
        +String model
        +int max_retries
        +int batch_size
    }
    
    class FeedConfig {
        +String url
        +int timeout
        +String user_agent
    }
    
    class StorageConfig {
        +Path output_path
    }
    
    %% Clients
    class GeminiClient {
        -String _model_name
        -GenerativeModel _model
        -int _max_retries
        -float _retry_backoff
        +String model_name
        +generate(prompt) String
    }
    
    class TechmemeFeedClient {
        -String _feed_url
        -int _timeout
        -Session _session
        +String feed_url
        +Session session
        +_make_session(timeout, user_agent) Session
        +fetch_raw_feed() FeedParserDict
        +fetch_articles(limit) List~FeedArticle~
        +enrich_article(url) Dict
    }
    
    %% Services
    class FeedService {
        -TechmemeFeedClient _client
        +fetch_latest(limit) List~FeedArticle~
    }
    
    class SummarizationService {
        -GeminiClient _client
        -String _prompt_template
        -int _batch_size
        +summarize(articles) List~SummaryResult~
        -_build_prompt(requests) String
        -_parse_summaries(payload) List~SummaryResult~
        -_fallback_summaries(batch) List~SummaryResult~
    }
    
    class StorageService {
        -Path _output_path
        +Path output_path
        +save(payload) Path
        +load_existing() SummaryPayload
    }
    
    %% Pipeline
    class NewsPipeline {
        -FeedService _feeds
        -SummarizationService _summaries
        -StorageService _storage
        +run(top_n, metadata) SummaryPayload
    }
    
    %% FastAPI
    class FastAPIApp {
        +get_summaries_fast() JSONResponse
        +trigger_refresh(background_tasks) JSONResponse
        +get_refresh_status() JSONResponse
        -_run_update_job(started_at, correlation_id)
    }
    
    class CorrelationIdMiddleware {
        +dispatch(request, call_next) Response
    }
    
    %% Relationships
    FeedArticle --> SummaryRequest : creates
    SummaryRequest --> SummaryResult : transforms into
    SummaryPayload *-- SummaryResult : contains
    
    Settings --> GeminiConfig : has
    Settings --> FeedConfig : has
    Settings --> StorageConfig : has
    
    FeedService --> TechmemeFeedClient : uses
    FeedService --> FeedArticle : produces
    
    SummarizationService --> GeminiClient : uses
    SummarizationService --> SummaryRequest : consumes
    SummarizationService --> SummaryResult : produces
    
    StorageService --> SummaryPayload : reads/writes
    
    NewsPipeline --> FeedService : orchestrates
    NewsPipeline --> SummarizationService : orchestrates
    NewsPipeline --> StorageService : orchestrates
    
    FastAPIApp --> StorageService : reads from
    FastAPIApp --> NewsPipeline : triggers (via subprocess)
    FastAPIApp ..> CorrelationIdMiddleware : uses
```

---

## 2. Sequence Diagram - Fast Flow (User Refresh)

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant FastAPI as FastAPI Server
    participant Storage as StorageService
    participant File as summaries.json
    
    User->>Browser: Click "Làm mới" button
    Browser->>FastAPI: GET /api/summaries
    
    FastAPI->>FastAPI: get_current_correlation_id()
    FastAPI->>File: Check if exists
    File-->>FastAPI: exists=True
    
    FastAPI->>File: Read JSON
    File-->>FastAPI: data (items, last_updated)
    
    FastAPI->>FastAPI: check_if_stale(last_updated)
    FastAPI->>FastAPI: calculate_age(last_updated)
    
    FastAPI-->>Browser: JSONResponse {items, freshness, is_stale}
    Browser->>Browser: Render news cards
    Browser->>Browser: Show icon (✅ fresh / ⚠️ stale)
    Browser-->>User: Display news (<1s)
    
    Note over User,File: No AI calls, no network fetch<br/>Just read local file
```

---

## 3. Sequence Diagram - Slow Flow (Admin Update)

```mermaid
sequenceDiagram
    actor Admin
    participant CLI as update_news.py
    participant Pipeline as NewsPipeline
    participant FeedSvc as FeedService
    participant Techmeme as TechmemeFeedClient
    participant RSS as Techmeme RSS
    participant SumSvc as SummarizationService
    participant Gemini as GeminiClient
    participant API as Google Gemini API
    participant Storage as StorageService
    participant File as summaries.json
    
    Admin->>CLI: python update_news.py
    CLI->>Pipeline: run(top_n=15)
    
    Pipeline->>FeedSvc: fetch_latest(limit=15)
    FeedSvc->>Techmeme: fetch_articles(limit=15)
    Techmeme->>RSS: HTTP GET feed.xml
    RSS-->>Techmeme: RSS entries
    Techmeme->>Techmeme: normalize_feed()
    Techmeme-->>FeedSvc: List[FeedArticle]
    FeedSvc-->>Pipeline: articles
    
    Pipeline->>SumSvc: summarize(articles)
    
    loop For each batch (size=6)
        SumSvc->>SumSvc: _build_prompt(batch)
        SumSvc->>Gemini: generate(prompt)
        Gemini->>API: HTTP POST (AI request)
        API-->>Gemini: Raw text response
        Gemini-->>SumSvc: response text
        SumSvc->>SumSvc: extract_json_block()
        SumSvc->>SumSvc: _parse_summaries()
    end
    
    SumSvc-->>Pipeline: List[SummaryResult]
    
    Pipeline->>Pipeline: Create SummaryPayload<br/>(add last_updated, total_items)
    Pipeline->>Storage: save(payload)
    Storage->>File: Write JSON
    File-->>Storage: Path
    Storage-->>Pipeline: Path
    
    Pipeline-->>CLI: SummaryPayload
    CLI-->>Admin: "Refresh complete [OK]"
    
    Note over Admin,File: Takes 30-90s<br/>Fetch + AI + Save
```

---

## 4. Sequence Diagram - Background Refresh (via API)

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant FastAPI as FastAPI /api/refresh
    participant BG as Background Task
    participant Subprocess as update_news.py
    participant Pipeline as NewsPipeline
    participant File as summaries.json
    
    User->>Browser: (Optional) Trigger backend update
    Browser->>FastAPI: GET /api/refresh
    
    FastAPI->>FastAPI: Check refresh_lock
    alt Refresh in progress
        FastAPI-->>Browser: 409 Conflict
    else Not running
        FastAPI->>FastAPI: Set refresh_status[started]=True
        FastAPI->>BG: background_tasks.add_task(_run_update_job)
        FastAPI-->>Browser: 200 {status: "started"}
        
        BG->>Subprocess: subprocess.run(update_news.py)
        Subprocess->>Pipeline: Run full pipeline
        Pipeline->>File: Write summaries.json
        File-->>Pipeline: Done
        Pipeline-->>Subprocess: Exit 0
        Subprocess-->>BG: returncode=0
        
        BG->>BG: Update refresh_status<br/>(completed=True, success=True)
    end
    
    Browser->>FastAPI: GET /api/refresh/status (polling)
    FastAPI-->>Browser: {completed: True, success: True}
    Browser-->>User: Show success notification
    
    Note over User,File: Background job (30-90s)<br/>User can poll status
```

---

## 5. Component Architecture Overview

```mermaid
graph TB
    subgraph "Frontend (Browser)"
        UI[news.html + JS]
        UI_Render[render.js]
        UI_Service[newsService.js]
    end
    
    subgraph "FastAPI Server (Port 8000)"
        API[app.py]
        Middleware[CorrelationIdMiddleware]
        Fast[GET /api/summaries<br/>Fast endpoint]
        Slow[GET /api/refresh<br/>Slow endpoint]
    end
    
    subgraph "Backend Pipeline (Python)"
        CLI[update_news.py]
        Pipeline[NewsPipeline]
        FeedSvc[FeedService]
        SumSvc[SummarizationService]
        Storage[StorageService]
    end
    
    subgraph "External Services"
        Techmeme[Techmeme RSS]
        Gemini[Google Gemini API]
    end
    
    subgraph "Data Layer"
        JSON[summaries.json]
    end
    
    UI --> UI_Service
    UI_Service --> Fast
    Fast --> JSON
    
    Slow --> CLI
    CLI --> Pipeline
    
    Pipeline --> FeedSvc
    Pipeline --> SumSvc
    Pipeline --> Storage
    
    FeedSvc --> Techmeme
    SumSvc --> Gemini
    Storage --> JSON
    
    API --> Middleware
    Middleware --> Fast
    Middleware --> Slow
    
    style Fast fill:#90EE90
    style Slow fill:#FFB6C1
    style JSON fill:#FFD700
```

---

## Usage Guide

### View in VS Code
1. Install extension: "Markdown Preview Mermaid Support"
2. Open this file
3. Press `Ctrl+Shift+V` (preview) or right-click → "Open Preview"

### Export as Image
1. Use VS Code extension "Mermaid Markdown Syntax Highlighting" with export feature
2. Or use online tool: https://mermaid.live
3. Copy diagram code → paste → export PNG/SVG

### Customize
- Edit diagram code directly in this markdown file
- Syntax guide: https://mermaid.js.org/intro/

---

Generated: 2025-10-22
