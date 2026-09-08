@echo off
setlocal
cd /d "%~dp0"

where dotnet >nul 2>nul
if errorlevel 1 (
    echo Ticket Toss needs the .NET 8 SDK, but dotnet was not found.
    echo Download it from: https://dotnet.microsoft.com/download/dotnet/8.0
    echo.
    pause
    exit /b 1
)

dotnet build "TicketToss.Companion.csproj" --nologo --verbosity quiet

if errorlevel 1 (
    echo.
    echo Ticket Toss could not start because of the build error shown above.
    pause
    exit /b 1
)

start "" "%~dp0bin\Debug\net8.0-windows\TicketToss.Companion.exe"

endlocal
