@echo off
echo ============================================================
echo LiveKit Location Backend Server
echo ============================================================
echo.

if not exist venv (
    echo ERROR: Virtual environment not found!
    echo Please run setup.bat first
    pause
    exit /b 1
)

if not exist .env (
    echo WARNING: .env file not found!
    echo Copying .env.example to .env...
    copy .env.example .env
    echo.
    echo Please edit .env and add your LiveKit credentials before continuing.
    echo Press any key to open .env in notepad...
    pause
    notepad .env
)

echo Activating virtual environment...
call venv\Scripts\activate.bat

echo.
echo Starting server...
echo API Documentation will be available at: http://localhost:8000/docs
echo.
python app.py
