# Watch session visibility handback

Watch version code 14. Implements the PM Watch Session Visibility handoff.

Begin Class persists the existing session and requests notification permission when needed. A single ClassSessionService foreground service posts one silent low-importance notification, ID 2001, with Wear OngoingActivity and the monochrome ticket icon. Tapping it resumes the existing singleTask MainActivity using SINGLE_TOP. Android 14+ uses the specialUse foreground-service type for the explicitly started classroom-control session.

The service has no timers, polling, network activity, or wake locks. Existing ambient dimming, sound lifecycle, odds, relay, Tile, and complication remain intact. A sticky service recreation checks persisted session and notification availability before restoring. Activity resume restores access idempotently. No boot receiver starts sessions. End Session clears persistence, stops the service, cancels notification/OngoingActivity, and removes the task.

Permission explanation appears on Begin Class. Denial leaves controls usable with a warning in the header. Long-press the header and select ENABLE SESSION ACCESS to retry; permanently denied permission or blocked notifications open app notification settings. Returning from settings rechecks availability. No prompts occur during rolls.

Physical acceptance required: Begin Class, allow notifications, leave untouched beyond ten seconds and several minutes, confirm ambient remains Ticket Toss and wrist raise restores controls; manually return to face and tap indicator; relaunch during active session and check no duplicate indicator; End Session and check notification/service disappear; deny permission and verify warning/retry plus local toss. Force-stop and OS/OEM policies remain outside a guarantee; device testing is necessary to verify the reported timeout is fixed.

Automated checks: Android phone/watch builds and existing unit suites; companion validation/build for compatibility. Desktop Style release remains 0.2.1 and is not changed by this work.
