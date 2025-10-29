"""FastAPI server for the Tech News Summarizer frontend and refresh API."""

from __future__ import annotations

import json
import logging
import os
import sys
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles

from src.middlewares.correlation import (
    CorrelationIdMiddleware,
    get_current_correlation_id,
)

LOGGER = logging.getLogger("tech_news_app")


class _CorrelationFilter(logging.Filter):
    """Attach the current correlation id to log records."""

    def filter(self, record: logging.LogRecord) -> bool:
        record.correlation_id = get_current_correlation_id() or "-"
        return True


LOG_FORMAT = "%(asctime)s %(levelname)s [%(correlation_id)s] %(name)s: %(message)s"
logging.basicConfig(level=logging.INFO, format=LOG_FORMAT)
logging.getLogger().addFilter(_CorrelationFilter())
LOGGER.addFilter(_CorrelationFilter())

ROOT_DIR = Path(__file__).resolve().parents[2]
NEWS_FILE = ROOT_DIR / "news.html"
STYLES_FILE = ROOT_DIR / "styles.css"
SUMMARY_FILE = ROOT_DIR / "data" / "outputs" / "summaries.json"
JS_DIR = ROOT_DIR / "js"
PUBLIC_DIR = ROOT_DIR / "public"
STYLES_DIR = ROOT_DIR / "styles"

app = FastAPI(
    title="Tech News Summarizer",
    description="Serve static assets and provide refresh endpoints.",
    version="1.0.0",
)

# Install correlation middleware first so downstream middleware/handlers see the id.
app.add_middleware(CorrelationIdMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

if JS_DIR.exists():
    app.mount("/js", StaticFiles(directory=JS_DIR), name="js")
if PUBLIC_DIR.exists():
    app.mount("/public", StaticFiles(directory=PUBLIC_DIR), name="public")
if STYLES_DIR.exists():
    app.mount("/styles", StaticFiles(directory=STYLES_DIR), name="styles")

# Background refresh via subprocess has been removed.
# Updates are expected to be performed by running `update_news.py` from the CLI
# (e.g. by an admin or a scheduler). The frontend reads `summaries.json` via
# the fast endpoint (`/api/summaries`) which returns the persisted file.


# ==================== HELPER FUNCTIONS ====================

def check_if_stale(last_updated_str: str | None, threshold_minutes: int = 60) -> bool:
    """
    Check if data is stale (older than threshold).
    
    Args:
        last_updated_str: ISO format timestamp string
        threshold_minutes: Age threshold in minutes (default: 60)
    
    Returns:
        True if data is stale or timestamp invalid
    """
    if not last_updated_str:
        return True
    
    try:
        last_updated = datetime.fromisoformat(last_updated_str.replace('Z', '+00:00'))
        age = datetime.now(last_updated.tzinfo) - last_updated
        return age > timedelta(minutes=threshold_minutes)
    except (ValueError, AttributeError):
        return True


def calculate_age(last_updated_str: str | None) -> str:
    """
    Calculate human-readable age of data.
    
    Args:
        last_updated_str: ISO format timestamp string
    
    Returns:
        Human-readable string like "5 phút trước", "2 giờ trước"
    """
    
    if not last_updated_str:
        return "Unknown"
    
    try:
        last_updated = datetime.fromisoformat(last_updated_str.replace('Z', '+00:00'))
        age = datetime.now(last_updated.tzinfo) - last_updated
        seconds = age.total_seconds()
        
        if seconds < 60:
            return "Just now"
        elif seconds < 3600:
            minutes = int(seconds / 60)
            return f"{minutes} minutes ago"
        elif seconds < 86400:
            hours = int(seconds / 3600)
            return f"{hours} hours ago"
        else:
            days = int(seconds / 86400)
            return f"{days} days ago"
    except (ValueError, AttributeError):
        return "Unknown"


# ==================== BACKGROUND JOB ====================

# Note: background subprocess-based refresh was intentionally removed to
# simplify the runtime. If you want an HTTP-triggered background update,
# consider calling the pipeline in-process (e.g., dispatching NewsPipeline.run
# in a thread or an async task) rather than spawning a subprocess. For now
# updates should be performed via the CLI: `python update_news.py` or
# `update_news.bat` (Windows).


@app.get("/", include_in_schema=False)
async def redirect_to_news() -> RedirectResponse:
    return RedirectResponse(url="/news.html")


@app.get("/news.html", include_in_schema=False)
async def serve_news() -> FileResponse:
    if not NEWS_FILE.exists():
        raise HTTPException(status_code=404, detail="news.html not found")
    return FileResponse(NEWS_FILE)


@app.get("/styles.css", include_in_schema=False)
async def serve_styles() -> FileResponse:
    if not STYLES_FILE.exists():
        raise HTTPException(status_code=404, detail="styles.css not found")
    return FileResponse(STYLES_FILE, media_type="text/css")


@app.get("/summaries.json", include_in_schema=False)
async def serve_summaries() -> FileResponse:
    if not SUMMARY_FILE.exists():
        raise HTTPException(status_code=404, detail="summaries.json not found")
    return FileResponse(SUMMARY_FILE, media_type="application/json")


@app.get("/api/summaries")
async def get_summaries_fast() -> JSONResponse:
    """
    Fast endpoint - reload data from file without triggering update.
    Returns data with freshness metadata.
    """
    correlation_id = get_current_correlation_id()
    
    # Read summaries.json
    if not SUMMARY_FILE.exists():
        raise HTTPException(
            status_code=404, 
            detail="No data available. Please trigger a refresh first."
        )
    
    try:
        with open(SUMMARY_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
    except json.JSONDecodeError as exc:
        LOGGER.error("Failed to parse summaries.json: %s", exc)
        raise HTTPException(status_code=500, detail="Data file is corrupted")
    
    # Extract last_updated timestamp
    last_updated = data.get("last_updated")
    
    # Calculate freshness
    is_stale = check_if_stale(last_updated, threshold_minutes=60)
    freshness = calculate_age(last_updated)
    
    LOGGER.info(
        "Fast reload: items=%d, last_updated=%s, is_stale=%s, freshness=%s",
        len(data.get("items", [])),
        last_updated,
        is_stale,
        freshness,
    )
    
    # Return data with metadata
    return JSONResponse(
        {
            "items": data.get("items", []),
            "last_updated": last_updated,
            "is_stale": is_stale,
            "freshness": freshness,
            "count": len(data.get("items", [])),
            "correlation_id": correlation_id,
        }
    )




# The HTTP-triggered refresh/status endpoints have been removed. Use the
# CLI (`python update_news.py`) or an external scheduler to perform updates.


@app.get("/healthz", include_in_schema=False)
async def healthcheck() -> JSONResponse:
    return JSONResponse({"status": "ok"})

if __name__ == "__main__":
    try:
        import uvicorn  # type: ignore
    except Exception:
        # If uvicorn is not importable in this environment (e.g. linter/IDE),
        # try to run it as a module via subprocess, otherwise instruct the user.
        try:
            import subprocess

            subprocess.run(
                [
                    sys.executable,
                    "-m",
                    "uvicorn",
                    "src.api.app:app",
                    "--host",
                    "0.0.0.0",
                    "--port",
                    "8000",
                    "--reload",
                ],
                check=True,
            )
        except Exception:
            print(
                "Uvicorn is not available; install it with 'pip install uvicorn[standard]' "
                "or run 'python -m uvicorn src.api.app:app'"
            )
            sys.exit(1)
    else:
        uvicorn.run(
            "src.api.app:app",
            host="0.0.0.0",
            port=8000,
            reload=True,
        )
