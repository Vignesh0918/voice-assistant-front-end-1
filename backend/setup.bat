@echo off
echo ============================================================
echo LiveKit Location Backend - Setup Script
echo ============================================================
echo.

echo [1/3] Creating virtual environment...
python -m venv venv
if %errorlevel% neq 0 (
    echo ERROR: Failed to create virtual environment
    pause
    exit /b 1
)

echo [2/3] Activating virtual environment...
call venv\Scripts\activate.bat

echo [3/3] Installing dependencies...
pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo ERROR: Failed to install dependencies
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Setup completed successfully!
echo ============================================================
echo.
echo Next steps:
echo 1. Copy .env.example to .env
echo 2. Edit .env and add your LiveKit credentials
echo 3. Run start.bat to start the server
echo.
pause
