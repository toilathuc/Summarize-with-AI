@echo off
echo ================================
echo    CAP NHAT TIN TUC TECHMEME
echo ================================
echo.

echo [INFO] Bat dau cap nhat du lieu...
echo.

REM Chạy script Python cập nhật
echo [1/2] Dang lay du lieu moi tu Techmeme...
C:/Users/ADMIN/AppData/Local/Programs/Python/Python313/python.exe update_news.py

REM Kiểm tra kết quả
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ================================
    echo     CAP NHAT THANH CONG!
    echo ================================
    echo.
    echo [INFO] Du lieu da duoc cap nhat tai:
    echo       - summaries.json
    echo.
    echo [INFO] Website se hien thi du lieu moi
    echo       - Refresh trang web (F5) de xem
    echo       - Hoac click nut Refresh tren web
    echo.
    echo [INFO] De cap nhat tu dong moi 30 phut:
    echo       - Chay: start_auto_update.bat
    echo.
) else (
    echo.
    echo ================================
    echo       CAP NHAT THAT BAI!
    echo ================================
    echo.
    echo [ERROR] Co loi xay ra khi cap nhat
    echo [INFO] Kiem tra:
    echo        - Ket noi internet
    echo        - API key trong .env
    echo        - Thu chay lai sau it phut
    echo.
)

echo Nhan phim bat ky de dong...
pause >nul