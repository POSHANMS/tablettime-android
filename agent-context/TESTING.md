# TESTING.md — TabletTime Acceptance Checklist

This is the definition of "done." Every item must be checked and confirmed before the project is reported complete. If an item genuinely cannot be verified without a physical device, state that explicitly instead of assuming a pass.

## Build & setup
- [ ] Project builds and runs from a fresh Android Studio checkout with zero manual dependency fixes.
- [ ] No internet/network permission appears anywhere in `AndroidManifest.xml`.
- [ ] No Firebase, analytics, or ad SDK dependencies in `build.gradle`.

## Scheduling & alarms
- [ ] Adding a reminder schedules a real exact alarm (verifiable via logcat / `AlarmManager` dump).
- [ ] Killing the app from recents does not cancel a scheduled alarm.
- [ ] Rebooting the device (or simulating `BOOT_COMPLETED`) restores all active alarms correctly.
- [ ] Turning the master switch off pauses all alarms without deleting reminder data; turning it back on re-schedules them.

## Check-in flow
- [ ] At trigger time, the alarm sound plays and the check-in screen appears, including over the lock screen.
- [ ] Yes path: sound stops immediately, confirmation message shows, screen auto-closes after ~5–8 seconds.
- [ ] No path: sound stops, snooze options (5/10/30/60 min) appear.
- [ ] Chosen snooze fires exactly once, only for that day; the next day's regular scheduled time is unaffected.
- [ ] The snoozed alarm repeats the same yes/no flow once — it does not loop indefinitely.

## Permissions
- [ ] Every special permission (exact alarms, notifications, full-screen intent, battery optimization) shows a rationale before requesting.
- [ ] Denial of any permission is handled gracefully with a clear explanation and a settings deep-link — no crash, no silent failure.

## UI/UX
- [ ] No default/unstyled Material components remain anywhere.
- [ ] Dark mode looks intentional, not just inverted colors.
- [ ] Toggles, list changes, and screen transitions are animated, not instant/jarring.
- [ ] Copy throughout matches the warm, human tone in `UI_GUIDELINES.md`.

## Code quality
- [ ] No leftover TODOs or stub functions in the final output.
- [ ] MVVM boundaries respected — no direct Room access from Composables.
- [ ] All strings pulled from `strings.xml`, not hardcoded inline.
