# Ticket Toss — Build 2

## Development scope

Watch audio, persistent controller destinations, and an acknowledged watch → phone → projector relay. This is a development build; the desktop Style identity and release manifest are unchanged.

## Use

Install the phone and watch APKs from the same build/signing environment. Both use the existing application ID. If Android rejects an update because an older debug build used another signing key, uninstall that debug app and reinstall; uninstalling removes its settings. The Data Layer requires matching phone/watch signatures.

On the phone, long-press **TICKET TOSS** to open projector settings. Enter the existing desktop address and pairing code, test the connection, and choose **Phone only**, **PC only**, or **Phone and PC**.

On the watch, begin/resume the class and long-press the header to choose **Watch only**, **PC only**, or **Watch and PC**. The saved choices are independent. The watch defaults to local only. A previously configured phone defaults to combined; an unconfigured phone defaults to local only.

Tap or slide/release to toss as before. Local-only plays local audio. Combined displays locally but leaves audio to the desktop. PC-only gives neutral delivery status/haptics and never shows the outcome locally, including failures. Watch sounds use the existing assets; drumroll is stopped after one second. Playback stops when the activity pauses or enters ambient mode.

The phone relay service operates without opening the phone activity. A compatible Build 2 desktop companion must be running for PC delivery; earlier companions retain their old protocol and do not understand V1 events.

## Event protocol

Existing TCP port 45832 and pairing prefix are retained:

`TICKETTOSS|code|V1|event-id|GREEN-or-RED|0-or-1|probability-percent|PHONE-or-WATCH|destination|timestamp-ms`

`0` means not awarded; `1` means awarded. Green probabilities are 15/35/60/80/100; red probabilities are 10/25/50/75/100. IDs are UUIDs. A successful receipt is `OK|event-id`; malformed, unsupported, incorrectly paired, or conflicting duplicate outcomes receive `DENIED`. Existing `PING` and legacy outcomes remain supported.

Data Layer request/reply paths are `/ticket-toss/request` and `/ticket-toss/reply`. The phone forwards the exact watch event and replies only after desktop receipt or bounded failure. Status probes distinguish phone unavailable, phone connected with PC unavailable, and PC ready. No relay performs another roll.

## Delivery limits

The phone tries a retained event at most three times with a 500 ms pause between attempts. The watch may repeat the same request once if its acknowledgement fails. Socket and Data Layer waits are bounded. Retention is in-memory during the operation, not an indefinite background backlog.

Desktop event IDs are deduplicated for ten minutes within the running companion, with a 4096-entry cap. A duplicate receipt does not schedule another animation. Different events arriving during an animation are queued. The deduplication cache does not survive restarting the companion; avoid restarting it during a pending delivery.

PC-only never falls back to a local result or sound. Combined retains local visuals/haptics and reports failure discreetly; it does not switch on local sound when PC delivery fails. Local-only never contacts the projector.

## Validation

- Shared Kotlin checks cover both origins, every colour, all odds levels, wins/misses, routing restoration, privacy/audio flags, and malformed protocol input.
- Desktop console checks cover strict parsing, all four exact outcomes, concurrent duplicate handling, TCP receipts/retries, pairing rejection, undefined legacy enum rejection, and oversized input.
- GitHub validates phone/watch debug builds and unit tests, plus the desktop WPF build and TCP checks.
- Physical acceptance remains necessary for speaker volume, wrist raise/ambient/session restoration, all delivery modes, closed-phone relay, disconnected phone/PC, settings persistence, and End Session.

Build commands:

```
./gradlew :app:assembleDebug :wear:assembleDebug :app:testDebugUnitTest :wear:testDebugUnitTest
dotnet run --project desktop-companion-tests/ProtocolTests.csproj
dotnet build desktop-companion/TicketToss.Companion.csproj -p:EnableWindowsTargeting=true
```

Development APK and companion artifacts expire after three days. The companion ZIP is a self-contained Windows x64 development build: extract it and open `TicketToss.Companion.exe`. They are not store builds or approved Style releases.

## Verified handback — September 30, 2026

Implementation source: `a5a2eb102c8f6ac648320aa3ac0b2236cd5564aa` on `master`.

- Phone: version 1.1.0, version code 3.
- Watch: version 0.2.0, version code 4.
- [Phone/watch build, both unit-test suites, and APK artifact](https://github.com/TheMisterKyle/ticket-app/actions/runs/36800990650): passed.
- [Desktop TCP tests, WPF build, and portable development companion artifact](https://github.com/TheMisterKyle/ticket-app/actions/runs/36800990683): passed.
- Independent local Kotlin check: 120 exact outcome/routing round trips plus malformed input checks passed.
- Desktop Style manifest/version, release identity, and updater policy were preserved. No approved Style release was published.

Changed modules: phone UI/settings and background watch relay; watch UI/settings/audio; shared protocol and transports; desktop protocol/server/animation queue; shared Kotlin and desktop protocol tests; targeted development validation/artifact workflows. Full implementation and acceptance instructions are above.

Next step: physical acceptance on Kyle's Pixel Watch 3, paired phone, and classroom PC. Automated checks do not establish real speaker volume, wrist-raise behaviour, Bluetooth reconnection, closed-phone service delivery, display layout, or classroom audio routing. Confirm those, settings after restarting both controllers, ambient/session restoration, and deliberate End Session before promoting this development build.
