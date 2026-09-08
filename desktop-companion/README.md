# Ticket Toss Projector Companion — Proof of Concept

This Windows utility displays a transparent result animation on a selected monitor. It can be triggered manually or by the Ticket Toss Android app over the local network.

## Run from source

1. Install the .NET 8 SDK.
2. Open a terminal in `desktop-companion`.
3. Run `dotnet run`.
4. Choose the projector display and test each result.

## Connect the phone

1. Put the PC and phone on the same Wi-Fi network.
2. Start the companion and note the address and four-digit code it displays.
3. In the Android app, press and hold the **TICKET TOSS** heading.
4. Enter the PC address and pairing code, then tap **Test connection** and **Save**.
5. If Windows Firewall asks, allow access on **Private networks**.

The phone continues to work normally if the companion is closed or unreachable.
The companion keeps the same pairing code between launches, and the Android app remembers the connection details. Pairing normally only needs to be completed once. If the PC's local network address changes, update only the address in the phone's hidden settings.

Closing the controls minimizes the companion to the notification tray. Right-click its tray icon to reopen it or exit.

## Build a portable Windows folder

```powershell
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```

The output is placed under `bin\Release\net8.0-windows\win-x64\publish`.
