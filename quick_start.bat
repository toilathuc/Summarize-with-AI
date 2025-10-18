@echo off
echo ===================================
echo TECH NEWS WEBSITE - QUICK START
echo ===================================
echo.

echo Checking Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python not found!
    pause
    exit /b 1
)

echo Python found!
echo.

echo Checking files...
if not exist "src\api\app.py" (
    echo ERROR: src\api\app.py not found!
    pause
    exit /b 1
)

if not exist "summaries.json" (
    echo WARNING: summaries.json not found
    if exist "data\outputs\summaries.json" (
        echo Copying from data\outputs\...
        copy "data\outputs\summaries.json" "summaries.json" >nul
        echo Data copied!
    ) else (
        echo ERROR: No data file found!
        echo Please run: python update_news.py
        pause
        exit /b 1
    )
)

echo All files OK!
echo.

echo Starting FastAPI server...
echo Opening browser at http://localhost:8000
echo.
echo *** Press Ctrl+C to stop server ***
echo.

start http://localhost:8000
call start_fastapi.bat

pause
