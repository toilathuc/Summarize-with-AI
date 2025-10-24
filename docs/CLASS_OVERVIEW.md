# CLASS OVERVIEW - Summarize-with-AI

This document summarises the important classes in `src/` and gives a short description of other top-level files for visualization.

---

## src.domain.models.py

- FeedArticle
  - Purpose: Normalized representation of a Techmeme entry.
  - Fields: `title`, `techmeme_url`, `original_url`, `summary_text`, `content_text`, `hash`, `raw`.
  - Key methods:
    - `from_dict(payload)` - build from raw feed entry dict.
    - `to_summary_request()` - prepare `SummaryRequest` (truncates text to 8000 chars).

- SummaryRequest
  - Purpose: Input payload for the summarisation service.
  - Fields: `title`, `url`, `text`.
  - Key methods:
    - `to_prompt_dict()` - shape into prompt-ready dict.

- SummaryResult
  - Purpose: The structured output produced by the summariser.
  - Fields: `title`, `url`, `bullets`, `why_it_matters`, `type`, `key_commands`, `caveats`.
  - Key methods:
    - `from_dict(payload)` - build from generic dict.
    - `to_dict()` - serialise to primitive dict for persistence.

- SummaryPayload
  - Purpose: Container persisted to disk for the frontend.
  - Fields: `summaries` (list of SummaryResult), `extra` (metadata like timestamps)
  - Key methods:
    - `to_dict()` - serialise payload.
    - `empty()` - create empty payload.
    - `from_existing(summaries)` - build from list of dicts.

---

## src.config.settings.py

- GeminiConfig (dataclass)
  - Fields: `api_key`, `model`, `max_retries`, `batch_size`.

- FeedConfig (dataclass)
  - Fields: `url`, `timeout`, `user_agent`.

- StorageConfig (dataclass)
  - Fields: `output_path` (Path to `data/outputs/summaries.json`).

- Settings
  - Purpose: Centralised runtime configuration loaded from environment.
  - Behavior: Provides `settings.gemini`, `settings.feed`, `settings.storage`, `settings.summary_prompt_template`.
  - Note: `settings.gemini` raises if `GEMINI_API_KEY` is missing.

---

## src.clients.gemini.py

- GeminiClient
  - Purpose: Thin wrapper around Google Generative AI SDK.
  - Key behavior:
    - `__init__(api_key, model, ...)` - configure SDK and model object.
    - `generate(prompt)` - call model and return raw text, implements retry/backoff.

---

## src.services

- FeedService (`src/services/feed_service.py`)
  - Purpose: Facade for fetching articles via `TechmemeFeedClient`.
  - Methods: `fetch_latest(limit=None)` → returns list of `FeedArticle`.

- StorageService (`src/services/storage_service.py`)
  - Purpose: Read/write `SummaryPayload` to disk (`data/outputs/summaries.json`).
  - Methods: `save(payload)` → writes JSON and returns path; `load_existing()` → load existing payload.

- SummarizationService (`src/services/summarization_service.py`)
  - Purpose: Convert `FeedArticle` → `SummaryResult` using `GeminiClient`.
  - Key methods:
    - `summarize(articles)` - batch requests to Gemini, parse JSON block, fallback if parse fails.
    - `_build_prompt()` - create prompt from a batch.
    - `_parse_summaries(payload)` - convert to `SummaryResult`.
    - `_fallback_summaries(batch)` - produce safe fallback items.

---

## src.pipelines.news_pipeline.py

- NewsPipeline
  - Purpose: Orchestrate `FeedService` → `SummarizationService` → `StorageService`.
  - Method: `run(top_n=20, metadata=None)` → fetch articles, summarise, persist `SummaryPayload` with `last_updated` and `total_items`.

---

## src.middlewares.correlation.py

- CorrelationIdMiddleware (BaseHTTPMiddleware)
  - Purpose: Generate/propagate per-request correlation id for logs and background tasks.
  - Key helpers: `get_current_correlation_id()`, `set_current_correlation_id()`.

---

## src.feeds.techmeme.client.py

- TechmemeFeedClient
  - Purpose: Fetch and normalise Techmeme RSS feed.
  - Key methods:
    - `_make_session(timeout, user_agent)` - create requests session with retry.
    - `fetch_raw_feed()` - HTTP GET + parse with `feedparser`.
    - `fetch_articles(limit=None)` - returns list of `FeedArticle`.
    - `enrich_article(url)` - fetch original article HTML and extract details.
  - Helpers in file: `is_external`, `strip_tracking`, `html_to_text`, `to_iso8601`, `extract_links`, `as_plain_dict`.

---

## Other `src/` modules (headings only)

- `src/api/app.py`  — FastAPI application wiring, endpoints (`/api/summaries`, `/api/refresh`, static mounts).
- `src/tasks/celery_tasks.py` — Celery background tasks (referenced by pipeline for async updates).
- `src/clients/` — other client wrappers (e.g., additional APIs).
- `src/utils/` — utility helpers used across services (e.g., `chunked`, `extract_json_block`).

---

## Top-level files (headings only)

- `news.html` — Frontend entry page (loads `js/main.js`).
- `js/` — Frontend JS modules (main logic, services, render, ui).
- `update_news.py` — CLI script to run full pipeline (admin).
- `update_news.bat` — Windows batch wrapper for `update_news.py`.
- `start_fastapi.bat` — Start the FastAPI server (uses venv if present).
- `README.md`, `docs/` — Project documentation.

---

## Next steps (optional)

- Generate class diagrams (UML) for `src/` classes.
- Add method signatures and docstrings export (for API docs).
- Create a visual `ARCHITECTURE.md` with sequence diagrams for `fast` vs `slow` flows.


---

Generated by the repo assistant. If you want, I can now:
- Create an actual UML PNG or mermaid diagram file under `docs/`.
- Expand the headings into full sections in `README.md`.
