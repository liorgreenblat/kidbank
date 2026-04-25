@echo off
:: One-time setup for the Windows version.
:: Double-click this file to run it.

echo Installing Python dependency...
pip install psutil

echo.
echo Done! To start the limiter, double-click windows_limiter.py
echo.
echo To auto-start at login (Windows Startup folder):
echo   1. Press Win+R, type: shell:startup, press Enter
echo   2. Create a shortcut to windows_limiter.py in that folder
echo.
pause
