@echo off@echo off

REM ================================echo ================================

REM   UPDATE TECH NEWS - TECHMEMEecho    CAP NHAT TIN TUC TECHMEME

REM ================================echo ================================

echo.

echo.

echo [INFO] Starting data update...echo [INFO] Bat dau cap nhat du lieu...

echo.echo.



REM Run Python update scriptREM Chạy script Python cập nhật

echo [1/1] Fetching latest news from Techmeme...echo [1/2] Dang lay du lieu moi tu Techmeme...

.venv\Scripts\python.exe update_news.py --top 30E:\Viscode\Demo_Skola\.venv\Scripts\python.exe update_news.py



REM Check resultREM Kiểm tra kết quả

if %ERRORLEVEL% EQU 0 (if %ERRORLEVEL% EQU 0 (

    echo.    echo.

    echo ================================    echo ================================

    echo      UPDATE SUCCESSFUL!    echo     CAP NHAT THANH CONG!

    echo ================================    echo ================================

    echo.    echo.

    echo [INFO] Data has been updated at:    echo [INFO] Du lieu da duoc cap nhat tai:

    echo        - data/outputs/summaries.json    echo       - summaries.json

    echo        - summaries.json (public copy)    echo.

    echo.    echo [INFO] Website se hien thi du lieu moi

    echo [INFO] The website will show new data    echo       - Refresh trang web (F5) de xem

    echo        - Refresh the webpage (F5) to view    echo       - Hoac click nut Refresh tren web

    echo        - Or click Refresh button on the page    echo.

    echo.    echo [INFO] De cap nhat tu dong moi 30 phut:

) else (    echo       - Chay: start_auto_update.bat

    echo.    echo.

    echo ================================) else (

    echo       UPDATE FAILED!    echo.

    echo ================================    echo ================================

    echo.    echo       CAP NHAT THAT BAI!

    echo [ERROR] An error occurred during update    echo ================================

    echo [INFO] Please check:    echo.

    echo        - Internet connection    echo [ERROR] Co loi xay ra khi cap nhat

    echo        - API key in .env file    echo [INFO] Kiem tra:

    echo        - Try again in a few minutes    echo        - Ket noi internet

    echo.    echo        - API key trong .env

)    echo        - Thu chay lai sau it phut

    echo.

echo Press any key to exit...)

pause >nul

echo Nhan phim bat ky de dong...
pause >nul