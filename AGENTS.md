# AGENTS.md — TabletTime

This file tells any AI coding agent (Antigravity, Kimi, etc.) how to work in this repo. Read this file first, before touching any code.

## Project

**TabletTime** — a 100% local, offline Android medication reminder app.
Package: `com.poshan.tablettime`
Language: Kotlin only. UI: Jetpack Compose (Material 3). Architecture: MVVM. Local storage: Room.

Full specs live in `/agent-context/`:
- `GOAL.md` — the north-star objective and hard constraints
- `ARCHITECTURE.md` — tech stack, package layout, folder structure
- `FEATURES.md` — full feature spec, screen by screen
- `UI_GUIDELINES.md` — visual/design direction ("premium but simple")
- `TESTING.md` — the acceptance checklist that must pass before anything is considered done

Read all five before writing code. They are the source of truth — if this file and one of them ever disagree, the file in `/agent-context/` wins.

## Build & run

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew test
./gradlew connectedAndroidTest
```

## Non-negotiable rules

- No network permissions anywhere. No Firebase, no analytics SDKs, no ad SDKs.
- No 24/7 foreground service. Scheduling must use `AlarmManager` exact alarms only.
- Everything persists locally via Room. No cloud sync.
- Kotlin idioms only — no Java files.
- Every new screen must follow `UI_GUIDELINES.md` exactly (colors, spacing, motion). Do not use default unstyled Material components.

## Definition of done

A feature or the whole app is only "done" when every item in `TESTING.md` passes. Do not report the project as complete until you have gone through that checklist item by item and confirmed each one. If something can't be verified without a physical device, say so explicitly instead of assuming it passes.

## Commit style

Small, descriptive commits. One logical change per commit (e.g. "Add Room entity + DAO for Reminder", "Implement exact-alarm scheduling", "Build check-in full-screen activity UI"). Do not squash the whole build into a single commit.
