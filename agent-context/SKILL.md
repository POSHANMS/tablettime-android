---
name: android-premium-local-app-builder
description: Use this skill whenever building or modifying the TabletTime Android app (or any similarly-scoped local-first, alarm-driven Android app). Covers Kotlin/Compose conventions, exact-alarm scheduling patterns, Room persistence, and the "premium but simple" UI bar this project must meet. Trigger for any task touching this repo's Kotlin source, Gradle config, or Compose UI.
---

# Android Premium Local App Builder — Skill

## When this skill applies
Any time you are writing, editing, or reviewing code in this repository, or scaffolding it from scratch.

## Engineering standards to follow

1. **Kotlin + Compose only.** No XML layouts, no Java files, no findViewById patterns.
2. **MVVM strictly.** UI (Composables) never talks to Room directly — always through a ViewModel exposing `StateFlow`/`State`.
3. **Alarms, not services.** Never use a long-running foreground service for scheduling. Use `AlarmManager.setExactAndAllowWhileIdle()` (or `setAlarmClock` if you need guaranteed delivery through Doze) scheduled one at a time, rescheduled after each fire and after `BOOT_COMPLETED`.
4. **Permissions handled explicitly.** Every dangerous/special permission (`SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`, full-screen intent, battery-optimization exemption) must have: a rationale screen shown before requesting it, graceful handling if denied, and a settings deep-link if the user needs to fix it manually later.
5. **No dead code, no TODOs left in.** If something isn't implemented, it isn't done — don't leave placeholder stubs in the final output.
6. **Self-review before declaring done.** After generating code, re-read it against `TESTING.md` in `/agent-context/` and fix anything that would fail before saying the task is complete.

## Design bar ("premium but simple")

- Material 3 with a **customized** color scheme — never ship default Compose demo purple/teal.
- Generous whitespace, rounded corners (12–20dp), soft elevation — not flat, not cluttered.
- Motion: use Compose's `animateXAsState` / `AnimatedVisibility` for toggles, list changes, and screen transitions. Nothing should just "snap" into place.
- Copy (text in the app) should be short, warm, and human — never clinical or robotic wording.
- Full spec: see `UI_GUIDELINES.md`.

## Anti-patterns to avoid

- Polling loops or `Handler.postDelayed` for scheduling instead of `AlarmManager`.
- Storing reminder data in `SharedPreferences` instead of Room (Room is required — it scales better and is required by `ARCHITECTURE.md`).
- Requesting permissions with no explanation shown first.
- Any hardcoded string that should come from `strings.xml` (for consistency and potential future localization).
- Committing generated build artifacts, keystores, or `local.properties`.

## Output expectation

A complete, buildable Android Studio project with no manual fix-up steps required, matching every file in `/agent-context/`.
