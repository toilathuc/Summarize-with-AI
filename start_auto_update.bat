@echo off
echo ================================
echo   AUTO UPDATE TIN TUC - 30 PHUT
echo ================================
echo.
echo [INFO] Script se cap nhat du lieu moi 30 phut
echo [INFO] Nhan Ctrl+C de dung
echo.

:loop
echo.
echo [%date% %time%] Bat dau cap nhat...
echo.

REM Chạy script cập nhật
call update_news.bat

echo.
echo [%date% %time%] Hoan thanh. Cho 30 phut...
echo.

REM Chờ 30 phút (1800 giây)
timeout /t 1800 /nobreak

goto loop