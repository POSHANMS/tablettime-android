# FEATURES.md — TabletTime

## 1. Home Screen
- Master "Reminders Active" on/off switch — turning off pauses all alarms without deleting them.
- List of configured reminders (card per reminder: label, time, active days, small toggle).
- Floating "+" button to add a new reminder.

## 2. Add/Edit Reminder Screen
- Material 3 `TimePicker`.
- Optional custom label (e.g. "Morning tablet", "Breakfast", "Evening dose") — defaults to a generic label if left blank.
- Day-of-week selector (defaults to every day).
- Saving writes to Room and immediately schedules the alarm.

## 3. Alarm trigger (core mechanic)
At the scheduled time:
- `AlarmReceiver` fires, plays an alarm-style looping sound (not a soft chime) for up to ~60 seconds or until dismissed.
- Launches `CheckInActivity` as a full-screen intent, shown even over the lock screen.

`CheckInActivity`:
- Shows the reminder's question (default: "Have you eaten your breakfast?", or the custom label if set).
- Two large buttons: **Yes** / **No**.
- **Yes** → sound stops → "✅ Please take your tablet now" → auto-closes after ~5–8 seconds.
- **No** → sound stops → snooze chips: **5 min / 10 min / 30 min / 1 hour** → picking one schedules a one-time alarm for that offset, **today only** — tomorrow's regular time is unaffected.
- The snoozed alarm, when it fires, repeats the same yes/no flow once; it does not loop indefinitely.

## 4. Reliability
- Alarms survive app swipe-away, device reboot, and Doze/App Standby.
- `BootReceiver` re-reads all active reminders from Room on `BOOT_COMPLETED` and re-registers their next alarm times.
- If a required permission is denied, show an explanation screen with a button that deep-links to the relevant system settings page — never fail silently.

## 5. Out of scope (do not build unless asked)
- Multiple user profiles
- Cloud backup/sync
- Medication history/analytics dashboard
- Any network calls of any kind
