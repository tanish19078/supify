@echo off
cd /d "%~dp0"
if exist "bin" rd /s /q bin
mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\app\TestApp.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed.
    pause
    exit /b %ERRORLEVEL%
)
cls
java -cp bin com.supify.app.Main
pause
