@echo off
setlocal
cd /d "%~dp0"

:: 1. Detect Python executable (python.exe or py.exe)
set "PYTHON_CMD="
where python >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "PYTHON_CMD=python"
) else (
    where py >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        set "PYTHON_CMD=py"
    )
)

if "%PYTHON_CMD%"=="" (
    echo ======================================================================
    echo [ERROR] Python was not found in your system PATH.
    echo Please install Python 3.10 or higher and make sure it is added to PATH.
    echo ======================================================================
    pause
    exit /b 1
)

:: 2. Verify critical dependencies
"%PYTHON_CMD%" -c "import PySide6, psutil, requests, qrcode, PIL" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ======================================================================
    echo [WARNING] Required dependencies are missing!
    echo Attempting to install from requirements.txt...
    echo ======================================================================
    "%PYTHON_CMD%" -m pip install -r requirements.txt
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] Failed to install dependencies. Please run manually:
        echo   pip install -r requirements.txt
        pause
        exit /b 1
    )
)

:: 3. Launch Server Control application
echo Starting CipherVault Server Control...
"%PYTHON_CMD%" main.py %*
set "EXIT_CODE=%ERRORLEVEL%"

if %EXIT_CODE% neq 0 (
    echo ======================================================================
    echo [ERROR] CipherVault Server Control exited with error code %EXIT_CODE%.
    echo Check logs in logs\server-manager.log for details.
    echo ======================================================================
    pause
    exit /b %EXIT_CODE%
)

exit /b 0
