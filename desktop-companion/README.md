# Ticket Toss Projector Companion — Proof of Concept

This Windows utility displays a transparent result animation on a selected monitor. This first pass is deliberately manual; phone connectivity comes next.

## Run from source

1. Install the .NET 8 SDK.
2. Open a terminal in `desktop-companion`.
3. Run `dotnet run`.
4. Choose the projector display and test each result.

Closing the controls minimizes the companion to the notification tray. Right-click its tray icon to reopen it or exit.

## Build a portable Windows folder

```powershell
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```

The output is placed under `bin\Release\net8.0-windows\win-x64\publish`.
