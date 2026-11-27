@echo off
cd /d "%~dp0"
echo ==========================================================
echo   Running Supify Backend Tests
echo ==========================================================
if exist "bin" rd /s /q bin
mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\app\TestApp.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)
java -cp bin com.supify.app.TestApp
pause
