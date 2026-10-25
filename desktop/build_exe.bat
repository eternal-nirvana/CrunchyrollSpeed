@echo off
echo ==========================================
echo Building CR-Speed.exe for Windows Laptop
echo ==========================================
pip install -r requirements.txt
pyinstaller --noconfirm --onedir --windowed --name "CR-Speed" cr_speed.py
echo.
echo ==========================================
echo Build Complete!
echo Executable is in: dist\CR-Speed\CR-Speed.exe
echo ==========================================
pause
