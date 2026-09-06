# MASTER PROMPT — "TabletTime" Medication Reminder App (Native Android)

> Copy everything below into Antigravity (or Kimi) as the initial project prompt.
> This is written as a full spec so the AI agent has zero ambiguity and can scaffold the entire project in one pass.

---

## ROLE

You are a senior Android engineer building a production-quality, polished, premium-feeling native Android app called **TabletTime**. Build the complete project — folder structure, Gradle setup, all source files, resources, and UI — so it opens and runs in Android Studio with zero manual fixes.

## GOAL (one sentence)

A 100% local, offline, battery-friendly medication reminder app that alarms the user at set times, asks a yes/no check-in question, and lets the user snooze just for that day if they say no.

## NON-NEGOTIABLE CONSTRAINTS

- **100% local** — no backend, no internet permission, no analytics, no ads, no cloud sync.
- **Battery-safe** — no foreground service running 24/7. Use `AlarmManager.setExactAndAllowWhileIdle()` / `AlarmClock`-style exact alarms scheduled ahead of time, not polling.
- Must work correctly through Doze mode and after device reboot (re-register alarms on `BOOT_COMPLETED`).
- Must request and gracefully handle: `SCHEDULE_EXACT_ALARM` (Android 12+), `POST_NOTIFICATIONS` (Android 13+), and full-screen intent / "display over other apps" if needed to auto-launch the activity from a locked/background state.
- Data persists locally only (Room database), survives app restarts and reboots.

## TECH STACK

- Kotlin only
- Jetpack Compose for all UI (Material 3)
- MVVM architecture (ViewModel + StateFlow)
- Room database for reminders + snooze state
- `AlarmManager` + `BroadcastReceiver` for scheduling/triggering
- `NotificationManager` with full-screen intent for the alarm trigger
- No third-party network libraries. No Firebase.

## SUGGESTED PROJECT STRUCTURE

```
app/
 └── src/main/java/com/tablettime/app/
      ├── MainActivity.kt
      ├── ui/
      │    ├── home/HomeScreen.kt          (master on/off switch, list of reminders)
      │    ├── setup/AddReminderScreen.kt  (time picker, label, days)
      │    ├── checkin/CheckInActivity.kt  (full-screen yes/no alarm screen)
      │    ├── theme/ (Color.kt, Type.kt, Theme.kt)
      │    └── components/ (reusable buttons, cards)
      ├── data/
      │    ├── Reminder.kt (Room entity)
      │    ├── ReminderDao.kt
      │    └── AppDatabase.kt
      ├── alarm/
      │    ├── AlarmScheduler.kt   (schedules/cancels exact alarms)
      │    ├── AlarmReceiver.kt    (BroadcastReceiver, fires on trigger time)
      │    └── BootReceiver.kt     (re-schedules alarms after reboot)
      └── viewmodel/
           ├── HomeViewModel.kt
           └── ReminderViewModel.kt
```

## FEATURE SPEC

### 1. Home Screen
- Big, clean master ON/OFF switch: "Reminders Active" — turning this off disables all scheduled alarms without deleting them.
- List of configured reminders (card per reminder: label, time, active days, small toggle).
- Floating "+" button to add a new reminder.
- Premium feel: soft rounded cards, generous spacing, a calm accent color (e.g. soft teal or sage — health-app feel, NOT clinical white/blue cliché), subtle elevation/shadow, smooth Compose animations on toggle and list changes.

### 2. Add/Edit Reminder Screen
- Time picker (Material 3 `TimePicker`).
- Optional label (e.g. "Morning tablet", "Breakfast").
- Day-of-week selector (defaults to every day).
- Save writes to Room and immediately schedules the alarm via `AlarmScheduler`.

### 3. Alarm Trigger (the core mechanic)
- At the scheduled time, `AlarmReceiver` fires:
  - Plays an alarm-style sound (looping, escalating like a real alarm, not a soft notification chime) for up to ~60 seconds or until dismissed.
  - Launches `CheckInActivity` as a full-screen intent, showing over the lock screen if needed.
- `CheckInActivity` UI:
  - Large, friendly question: **"Have you eaten your breakfast?"** (label should pull from the reminder's custom text if set).
  - Two big buttons: **Yes** / **No**.
  - **Yes** → sound stops instantly → screen changes to "✅ Please take your tablet now" → auto-dismiss/close after ~5–8 seconds.
  - **No** → sound stops → show snooze chips: **5 min / 10 min / 30 min / 1 hour** → user picks one → schedule a ONE-TIME alarm for that offset **for today only** → the original recurring daily time is untouched for tomorrow onward → close screen.
- After a snooze fires and is answered, no further snooze on the same instance loops back to the same yes/no flow.

### 4. Reliability requirements
- Alarms must survive: app being swiped away, phone reboot, Doze/App Standby.
- On `BOOT_COMPLETED`, re-read all active reminders from Room and re-register their next alarm times.
- Handle permission-not-granted states gracefully with a clear in-app explanation screen (not a silent failure) — e.g. if exact alarm permission is denied, show a card explaining why it's needed with a button that deep-links to the system settings page.

## UI/UX DIRECTION ("premium but simple")

- Material 3 design language, but customized — not default Compose demo colors.
- One clear accent color + neutral backgrounds; avoid clutter.
- Big tap targets, minimal text, no jargon.
- Micro-animations: switch toggles animate, cards animate in/out, check-in screen has a gentle pulse or fade rather than a jarring pop-up feel.
- Dark mode support matching the same premium aesthetic.
- Should feel like a small, well-crafted single-purpose app (think: a well-designed alarm clock app), not an enterprise form.

## ACCEPTANCE CHECKLIST (must all pass — self-verify before finishing)

- [ ] App builds and runs from a fresh Android Studio project with no manual dependency fixes.
- [ ] No internet/network permissions requested anywhere in the manifest.
- [ ] Adding a reminder actually schedules a real exact alarm (verify via `AlarmManager` logs).
- [ ] Killing the app from recents does not cancel a scheduled alarm.
- [ ] Rebooting the device (or simulating `BOOT_COMPLETED`) restores all active alarms.
- [ ] Yes path: sound stops, confirmation shown, auto-closes.
- [ ] No path: sound stops, snooze options shown, chosen snooze fires once, today only, doesn't affect tomorrow's schedule.
- [ ] Master on/off switch actually disables/re-enables all alarms.
- [ ] UI matches the "premium but simple" direction — no default unstyled Material components left in place.
- [ ] All permissions are requested with a clear explanation, and denial is handled without crashing.

## DELIVERABLE

Output the complete Android Studio project: full folder structure, all Kotlin files, `AndroidManifest.xml`, Gradle files (`build.gradle.kts`, `settings.gradle.kts`, version catalogs if used), and Compose theme files — ready to open and run.
