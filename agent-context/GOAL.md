# GOAL.md — TabletTime

## App name
**TabletTime**

## Package
`com.poshan.tablettime`

## One-sentence goal
Build a 100% local, offline, battery-friendly native Android app that alarms the user at scheduled times, asks a yes/no check-in question, and lets them snooze for that day only if they say no — with a premium-but-simple UI.

## Hard constraints (never violate)
- No internet permission, no backend, no analytics, no ads.
- No always-on foreground service — scheduling must use exact `AlarmManager` alarms.
- All data local, via Room. Survives app kill and device reboot.
- Kotlin + Jetpack Compose (Material 3) only.
- UI must feel premium and intentional, not like a default Compose template.

## Definition of success
Every checklist item in `TESTING.md` passes, and the project opens and builds in Android Studio with zero manual fixes.

## Read next
1. `ARCHITECTURE.md` — tech stack & folder structure
2. `FEATURES.md` — full feature spec
3. `UI_GUIDELINES.md` — design direction
4. `TESTING.md` — the checklist that defines "done"
