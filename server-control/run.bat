@echo off
setlocal
cd /d "%~dp0"

:: Check for pythonw.exe in PATH
where pythonw >nul 2>&1
if %ERRORLEVEL% equ 0 (
    start "" pythonw "%~dp0main.py" %*
    exit /b 0
)

:: Check for pyw.exe (Windows Python Launcher) in PATH
where pyw >nul 2>&1
if %ERRORLEVEL% equ 0 (
    start "" pyw "%~dp0main.py" %*
    exit /b 0
)

:: Fallback to python.exe
where python >nul 2>&1
if %ERRORLEVEL% equ 0 (
    start "" python "%~dp0main.py" %*
    exit /b 0
)

echo [ERROR] Python was not found in your PATH.
echo Please install Python 3.10 or higher and make sure it is added to PATH.
pause
exit /b 1
