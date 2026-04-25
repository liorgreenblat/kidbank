@echo off
:: ──────────────────────────────────────────────────────────────────────────────
:: Roblox Limiter — Windows Task Scheduler installer
::
:: Run this file as Administrator (right-click → Run as administrator).
::
:: Usage:
::   windows_task.bat install    ← register task, start now
::   windows_task.bat uninstall  ← remove task
::   windows_task.bat status     ← show whether the task exists
::
:: The task runs under the current user account at every logon and restarts
:: automatically if it exits. It uses pythonw.exe so no console window appears.
:: ──────────────────────────────────────────────────────────────────────────────
setlocal EnableDelayedExpansion

set TASK_NAME=RobloxLimiter
set SCRIPT_DIR=%~dp0
set SCRIPT_PATH=%SCRIPT_DIR%windows_limiter.py

:: Detect pythonw.exe (suppresses the console window)
for /f "delims=" %%i in ('where pythonw 2^>nul') do set PYTHONW=%%i
if not defined PYTHONW (
    for /f "delims=" %%i in ('where python 2^>nul') do set PYTHONW=%%i
)
if not defined PYTHONW (
    echo ERROR: Python not found. Install Python 3 from https://python.org
    pause
    exit /b 1
)

:: ── dispatch ──────────────────────────────────────────────────────────────────
set CMD=%~1
if "%CMD%"=="" set CMD=install

if /i "%CMD%"=="install"   goto :do_install
if /i "%CMD%"=="uninstall" goto :do_uninstall
if /i "%CMD%"=="status"    goto :do_status

echo Usage: windows_task.bat [install^|uninstall^|status]
exit /b 1

:: ── install ───────────────────────────────────────────────────────────────────
:do_install
    :: Check psutil
    python -c "import psutil" 2>nul
    if errorlevel 1 (
        echo Installing psutil...
        pip install psutil
    )

    :: Remove old task if present
    schtasks /delete /tn "%TASK_NAME%" /f >nul 2>&1

    :: Create XML for the task (gives us full control over settings)
    set XML_FILE=%TEMP%\roblox_limiter_task.xml
    call :write_xml

    :: Import the XML task
    schtasks /create /tn "%TASK_NAME%" /xml "%XML_FILE%" /f
    if errorlevel 1 (
        echo ERROR: Could not create the task. Make sure you are running as Administrator.
        pause
        exit /b 1
    )
    del "%XML_FILE%" >nul 2>&1

    :: Start it immediately (won't wait for next logon)
    schtasks /run /tn "%TASK_NAME%"

    echo.
    echo  Roblox Limiter is installed and running.
    echo  It will start automatically at every login.
    echo.
    pause
    exit /b 0

:: ── uninstall ─────────────────────────────────────────────────────────────────
:do_uninstall
    schtasks /end    /tn "%TASK_NAME%"        >nul 2>&1
    schtasks /delete /tn "%TASK_NAME%" /f     >nul 2>&1
    if errorlevel 1 (
        echo Task not found or could not be removed.
    ) else (
        echo  Roblox Limiter uninstalled.
    )
    pause
    exit /b 0

:: ── status ────────────────────────────────────────────────────────────────────
:do_status
    schtasks /query /tn "%TASK_NAME%" /fo LIST 2>nul
    if errorlevel 1 (
        echo  Task "%TASK_NAME%" is NOT registered.
    )
    pause
    exit /b 0

:: ── write_xml ─────────────────────────────────────────────────────────────────
:write_xml
    :: Get the current username for the task principal
    set CUR_USER=%USERDOMAIN%\%USERNAME%

(
echo ^<?xml version="1.0" encoding="UTF-16"?^>
echo ^<Task version="1.4" xmlns="http://schemas.microsoft.com/windows/2004/02/mit/task"^>
echo   ^<RegistrationInfo^>
echo     ^<Description^>Controls Roblox playtime for children^</Description^>
echo   ^</RegistrationInfo^>
echo   ^<Triggers^>
echo     ^<LogonTrigger^>
echo       ^<Enabled^>true^</Enabled^>
echo       ^<Delay^>PT10S^</Delay^>
echo     ^</LogonTrigger^>
echo   ^</Triggers^>
echo   ^<Principals^>
echo     ^<Principal id="Author"^>
echo       ^<UserId^>%CUR_USER%^</UserId^>
echo       ^<LogonType^>InteractiveToken^</LogonType^>
echo       ^<RunLevel^>HighestAvailable^</RunLevel^>
echo     ^</Principal^>
echo   ^</Principals^>
echo   ^<Settings^>
echo     ^<MultipleInstancesPolicy^>IgnoreNew^</MultipleInstancesPolicy^>
echo     ^<DisallowStartIfOnBatteries^>false^</DisallowStartIfOnBatteries^>
echo     ^<StopIfGoingOnBatteries^>false^</StopIfGoingOnBatteries^>
echo     ^<ExecutionTimeLimit^>PT0S^</ExecutionTimeLimit^>
echo     ^<RestartOnFailure^>
echo       ^<Interval^>PT1M^</Interval^>
echo       ^<Count^>999^</Count^>
echo     ^</RestartOnFailure^>
echo   ^</Settings^>
echo   ^<Actions Context="Author"^>
echo     ^<Exec^>
echo       ^<Command^>%PYTHONW%^</Command^>
echo       ^<Arguments^>"%SCRIPT_PATH%" --startup-delay^</Arguments^>
echo       ^<WorkingDirectory^>%SCRIPT_DIR%^</WorkingDirectory^>
echo     ^</Exec^>
echo   ^</Actions^>
echo ^</Task^>
) > "%XML_FILE%"
    exit /b 0
