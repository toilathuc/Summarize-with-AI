# Script tự động build và chạy ứng dụng Spring Boot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Summarize with AI - Java Spring Boot" -ForegroundColor Cyan
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

# Verify Java version
Write-Host ""
Write-Host "Checking Java version..." -ForegroundColor Cyan
java -version
Write-Host ""

# 2. Build project
Write-Host "Building project (mvn package)..." -ForegroundColor Cyan
mvn -DskipTests package

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "[OK] Build successful!" -ForegroundColor Green
Write-Host ""

# 3. Run application
Write-Host "Starting Spring Boot application..." -ForegroundColor Cyan
Write-Host "Frontend: http://localhost:8000/" -ForegroundColor Yellow
Write-Host "Health:   http://localhost:8000/healthz" -ForegroundColor Yellow
Write-Host "API:      http://localhost:8000/api/summaries" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Yellow
Write-Host ""

# Run JAR
java -jar target\summarizer-0.0.1-SNAPSHOT.jar
