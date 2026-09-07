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

echo Starting Ticket Toss Projector Companion...
dotnet run --project "TicketToss.Companion.csproj"

if errorlevel 1 (
    echo.
    echo The companion stopped because of the error shown above.
    pause
)

endlocal
