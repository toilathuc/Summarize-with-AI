"""FastAPI server for the Tech News Summarizer frontend and refresh API."""

from __future__ import annotations

import logging
import subprocess
import sys
import threading
import time
from pathlib import Path
from typing import Dict

from fastapi import BackgroundTasks, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles

LOGGER = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

ROOT_DIR = Path(__file__).resolve().parents[2]
NEWS_FILE = ROOT_DIR / "news.html"
STYLES_FILE = ROOT_DIR / "styles.css"
SUMMARY_FILE = ROOT_DIR / "summaries.json"
JS_DIR = ROOT_DIR / "js"
PUBLIC_DIR = ROOT_DIR / "public"
STYLES_DIR = ROOT_DIR / "styles"

app = FastAPI(
    title="Tech News Summarizer",
    description="Serve static assets and provide refresh endpoints.",
    version="1.0.0",
)

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

RefreshStatus = Dict[str, object]
refresh_status: RefreshStatus = {
    "started": False,
    "completed": True,
    "success": True,
    "timestamp": None,
    "output": "",
    "error": "",
    "finished_at": None,
}
refresh_lock = threading.Lock()


def _run_update_job(job_started_at: float) -> None:
    """Invoke update_news.py and capture status for polling."""
    LOGGER.info("Starting refresh job at %.3f", job_started_at)
    try:
        result = subprocess.run(
            [sys.executable, "update_news.py"],
            capture_output=True,
            text=True,
            cwd=str(ROOT_DIR),
            timeout=900,
        )
        success = result.returncode == 0
        output = result.stdout
        error = result.stderr if not success else ""
        LOGGER.info("Refresh job finished with code %s", result.returncode)
        if error:
            LOGGER.error("Refresh job stderr: %s", error[:4000])
    except subprocess.TimeoutExpired as exc:
        success = False
        output = exc.stdout or ""
        error = f"Refresh timed out after {exc.timeout} seconds"
        LOGGER.exception("Refresh job timed out")
    except Exception as exc:  # pylint: disable=broad-except
        success = False
        output = ""
        error = str(exc)
        LOGGER.exception("Refresh job failed")

    with refresh_lock:
        refresh_status.update(
            {
                "started": False,
                "completed": True,
                "success": success,
                "error": error,
                "output": output,
                "finished_at": time.time(),
            }
        )


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


@app.get("/api/refresh", response_model=None)
async def trigger_refresh(background_tasks: BackgroundTasks) -> JSONResponse:
    with refresh_lock:
        if refresh_status.get("started") and not refresh_status.get("completed"):
            raise HTTPException(status_code=409, detail="Refresh already in progress")

        started_at = time.time()
        refresh_status.update(
            {
                "started": True,
                "completed": False,
                "success": False,
                "timestamp": started_at,
                "output": "",
                "error": "",
                "finished_at": None,
            }
        )

    background_tasks.add_task(_run_update_job, started_at)
    return JSONResponse(
        {
            "status": "started",
            "message": "Data refresh started in background",
            "timestamp": refresh_status["timestamp"],
        }
    )


@app.get("/api/refresh/status")
async def get_refresh_status() -> JSONResponse:
    with refresh_lock:
        status_copy = dict(refresh_status)
    return JSONResponse(status_copy)


@app.get("/healthz", include_in_schema=False)
async def healthcheck() -> JSONResponse:
    return JSONResponse({"status": "ok"})


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "src.api.app:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )
