# Tech News Summarizer

An AI powered news aggregation project that monitors Techmeme, summarises the latest headlines with Google Gemini, and serves the results in a modern web interface.

---

## Table of Contents

1. [Highlights](#highlights)  
2. [Repository Layout](#repository-layout)  
3. [Prerequisites](#prerequisites)  
4. [Quick Start](#quick-start)  
5. [Updating the Data](#updating-the-data)  
6. [Configuration](#configuration)  
7. [Available Scripts](#available-scripts)  
8. [FastAPI Endpoints](#fastapi-endpoints)  
9. [Troubleshooting](#troubleshooting)  
10. [Next Steps & Contributions](#next-steps--contributions)

---

## Highlights

- Real-time Techmeme feed ingestion with resilient HTTP retry logic.  
- AI summarisation powered by Google Gemini (configurable model + batching).  
- FastAPI backend that serves static assets and exposes `/api/refresh` endpoints.  
- Responsive frontend with live refresh button, search, filters, and statistics.  
- Utility scripts for full AI refresh, quick non-AI updates, and smoke tests.  
- Configuration centralised through `src/config/settings.py` (dotenv ready).

---

## Repository Layout

```
e:\Viscode\Demo_Skola/
├── README.md                     # Project documentation (this file)
├── .env                          # Local environment variables (not committed)
├── news.html                     # Frontend entry point
├── styles.css                    # Global styling
├── js/                           # Frontend logic & utilities
│   ├── main.js                   # Bootstrap & UI orchestration
│   └── services/newsService.js   # Fetch + refresh helpers
├── summaries.json                # Data consumed by the frontend
├── start_fastapi.bat             # Helper to launch the API server
├── quick_start.bat               # Windows quick-start convenience script
├── update_news.py                # Full AI update pipeline CLI
├── simple_update.py              # Lightweight refresh without AI
├── quick_refresh.py              # Smoke test (touch timestamp only)
├── requirements.txt              # Python dependency set
├── src/
│   ├── api/app.py                # FastAPI application
│   ├── clients/gemini.py         # Google Gemini wrapper
│   ├── config/settings.py        # Settings dataclasses & `.env` loading
│   ├── domain/models.py          # Core datamodels (FeedArticle, SummaryResult, …)
│   ├── feeds/techmeme.py         # Legacy helper CLI for Techmeme
│   ├── feeds/techmeme/client.py  # Production Techmeme feed client
│   ├── pipelines/news_pipeline.py# Orchestrated fetch → summarise → persist flow
│   └── services/                 # Feed, storage, and summarisation services
└── docs/                         # Additional documentation
```

---

## Prerequisites

- **Python**: 3.10 or newer (project tested on 3.13).  
- **Google Gemini API key** (free tier available).  
- Optional: PowerShell on Windows for `.bat` helpers.

---

## Quick Start

1. **Clone and enter the project directory**
   ```powershell
   git clone <repo> e:\Viscode\Demo_Skola
   cd e:\Viscode\Demo_Skola
   ```

2. **Create and populate `.env`**
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   # Optional overrides:
   # GEMINI_MODEL=gemini-2.0-flash
   # TECHMEME_FEED_URL=https://www.techmeme.com/feed.xml
   ```

3. **Create a virtual environment (recommended)**
   ```powershell
   python -m venv .venv
   .\.venv\Scripts\Activate.ps1
   ```

4. **Install dependencies**
   ```powershell
   pip install -r requirements.txt
   ```

5. **Generate initial data** (ensures `summaries.json` exists)
   ```powershell
   py -3.13 update_news.py --top 25
   ```

6. **Start the FastAPI server**
   ```powershell
   start_fastapi.bat
   # or
   uvicorn src.api.app:app --host 0.0.0.0 --port 8000 --reload
   ```

7. **Open the site**  
   Navigate to `http://localhost:8000` to browse the latest summaries.

---

## Updating the Data

| Command | Description |
| ------- | ----------- |
| `py -3.13 update_news.py --top 25` | Full pipeline: fetch feed, summarise with Gemini, copy to `summaries.json`. |
| `py -3.13 simple_update.py --limit 15` | Fast, non-AI refresh useful for demos or when the API key is unavailable. |
| `py -3.13 quick_refresh.py` | Touches `summaries.json` (updates timestamp/first title) for smoke testing. |
| Frontend refresh button | Calls `/api/refresh`, triggers `update_news.py` asynchronously, and polls `/api/refresh/status`. |

**Tip:** The FastAPI server automatically serves the latest `summaries.json`, so refreshing the browser (or clicking the floating refresh button) updates the UI after any of the commands above complete.

---

## Configuration

All configuration flows through `src/config/settings.py`. Important keys:

| Environment Variable | Description | Default |
| -------------------- | ----------- | ------- |
| `GEMINI_API_KEY`     | Required for AI summaries. | none (raises error when missing) |
| `GEMINI_MODEL`       | Gemini model name. | `gemini-2.5-flash` |
| `GEMINI_MAX_RETRIES` | Retry attempts when Gemini API fails. | `3` |
| `GEMINI_BATCH_SIZE`  | Number of articles grouped per prompt. | `6` |
| `TECHMEME_FEED_URL`  | RSS source URL. | `https://www.techmeme.com/feed.xml` |
| `TECHMEME_TIMEOUT`   | HTTP timeout in seconds. | `12` |
| `CRAWLER_UA`         | User-Agent string used for feed requests. | TechHubBot |
| `SUMMARY_OUTPUT_PATH`| Where the pipeline writes its JSON before copying to project root. | `data/outputs/summaries.json` |
| `SUMMARY_PROMPT_TEMPLATE` | Custom prompt template (optional). | default multi-line prompt |

Settings load values from the environment, falling back to defaults. `.env` is read automatically for local development.

---

## Available Scripts

| Script | Purpose |
| ------ | ------- |
| `start_fastapi.bat` | Activates `.venv` (if present) and runs FastAPI with uvicorn. |
| `quick_start.bat`   | Performs safety checks and launches the server + browser. |
| `update_news.py`    | Main CLI for the AI-powered refresh workflow. |
| `simple_update.py`  | Lightweight refresh that skips the AI step. |
| `quick_refresh.py`  | Smoke test utility to keep UI feedback working. |
| `src/feeds/techmeme.py` | Diagnostic CLI for fetching and optionally enriching Techmeme entries. |

---

## FastAPI Endpoints

| Method | Path | Description |
| ------ | ---- | ----------- |
| `GET`  | `/`                  | Redirects to `news.html`. |
| `GET`  | `/news.html`         | Serves the frontend. |
| `GET`  | `/styles.css`        | Stylesheet. |
| `GET`  | `/js/...`            | JavaScript modules. |
| `GET`  | `/summaries.json`    | Latest summaries for the UI. |
| `GET`  | `/api/refresh`       | Kick off background refresh (`update_news.py`). Returns immediate status. |
| `GET`  | `/api/refresh/status`| Poll job status (started, completed, succeeded, output/error). |
| `GET`  | `/healthz`           | Simple health probe. |

Static directories (`/public`, `/styles`, `/js`) are mounted automatically when present.

---

## Troubleshooting

| Problem | Possible Fix |
| ------- | ------------ |
| `ValueError: GEMINI_API_KEY is required` | Ensure `.env` is populated and the variable is exported before running commands. |
| `summaries.json` missing | Run `py -3.13 update_news.py --top 25` or copy `data\outputs\summaries.json` into the project root. |
| Port 8000 already in use | Stop the conflicting process (`Get-Process python | Stop-Process -Force`) or launch with `--port 8001`. |
| Gemini failures / timeouts | Check internet connection, API quota, or adjust `GEMINI_MAX_RETRIES` & `GEMINI_BATCH_SIZE`. |
| Refresh button spins forever | Inspect FastAPI logs; verify `update_news.py` runs without errors (missing key, network issues, etc.). |
| Non-UTF8 artefacts in console | Everything in the codebase is now ASCII-only. If issues persist, ensure the terminal encoding is `utf-8`. |

---

## Next Steps & Contributions

- Explore the `docs/` directory for step-by-step run guides and refresh flow write-ups.  
- Extend the pipeline with additional sources (e.g. Hacker News, RSS feeds) by implementing new feed clients.  
- Add automated tests (e.g. pytest + FastAPI TestClient) to validate endpoints and services.  
- Containerise the stack (uvicorn + static files) or integrate with a task queue (Celery/Arq) for production refresh jobs.  
- Contributions are welcome—submit a PR with clear descriptions and test notes.

---

**Status**: Active development. For questions or ideas, open an issue or reach out to the maintainer.
