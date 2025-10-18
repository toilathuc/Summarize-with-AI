@echo off
setlocal

REM Start the FastAPI server for the Tech News Summarizer

if exist .\.venv\Scripts\activate (
    call .\.venv\Scripts\activate
)

uvicorn src.api.app:app --host 0.0.0.0 --port 8000 --reload
