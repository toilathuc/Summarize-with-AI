# Script chạy pipeline cập nhật tin (giống update_news.py)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Running News Update Pipeline" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Set JAVA_HOME
$jdkPath = 'C:\Program Files\Java\jdk-22'
if (Test-Path $jdkPath) {
    $env:JAVA_HOME = $jdkPath
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Write-Host "[OK] JAVA_HOME set to: $jdkPath" -ForegroundColor Green
} else {
    Write-Host "[WARN] JDK not found at $jdkPath, using system default" -ForegroundColor Yellow
}

Write-Host ""

# 2. Check if JAR exists
if (-not (Test-Path "target\summarizer-0.0.1-SNAPSHOT.jar")) {
    Write-Host "[INFO] JAR not found, building project first..." -ForegroundColor Yellow
    mvn -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Build failed!" -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
}

# 3. Run update pipeline
Write-Host "Running pipeline (fetch + summarize + save)..." -ForegroundColor Cyan
Write-Host ""

java -jar target\summarizer-0.0.1-SNAPSHOT.jar --update.now=true --update.top=25

Write-Host ""
if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Pipeline completed successfully!" -ForegroundColor Green
    Write-Host "Output: data\outputs\summaries.json" -ForegroundColor Yellow
} else {
    Write-Host "[ERROR] Pipeline failed!" -ForegroundColor Red
}

Write-Host ""
Read-Host "Press Enter to exit"
