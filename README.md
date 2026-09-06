# TabletTime 💊

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-FF6F00)](#architecture)
[![Database](https://img.shields.io/badge/Database-Room%20(SQLite)-00599C?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%20Zero%20Network-success)](#zero-network-privacy-first)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**TabletTime** is a 100% local, offline-first Android medication reminder application built with modern Kotlin and Jetpack Compose. Designed with a calming, medical-grade **Sage & Mint** design language, TabletTime delivers reliable, Doze-proof medication alerts without battery drain, tracking, or cloud dependencies.

---

## ✨ Features

- **🔒 100% Offline & Private**: Zero network permissions (`android.permission.INTERNET` is completely absent). No Firebase, no analytics, no ads, and no external telemetry.
- **⏰ Doze-Proof Exact Scheduling**: Uses Android's `AlarmManager.setExactAndAllowWhileIdle()` to guarantee timely delivery even during deep sleep, app termination, or aggressive battery optimization.
- **🔋 Zero Battery Drain**: No 24/7 foreground services or polling loops. The app sleeps until the exact millisecond of a scheduled dose.
- **📱 Clean Status Bar**: Unlike typical reminder apps, TabletTime does not clutter your top status bar with a persistent alarm icon, keeping it reserved exclusively for your phone's default wake-up alarm.
- **🔔 Full-Screen Check-In Flow**:
  - Automatically wakes the display and presents the check-in card directly over the lock screen.
  - Plays a custom, peaceful acoustic chime sequence (`peaceful_tablet_reminder.wav`) crafted specifically for medication alerts, paired with a gentle pulsing vibration.
  - Automatically times out after 60 seconds if unanswered.
- **💊 Interactive Response & Snooze**:
  - **"Yes, I took it"**: Instantly silences the audio, logs the confirmation, and smoothly auto-closes after 5 seconds.
  - **"Not yet"**: Silences audio and presents flexible snooze chips (**1 min**, **5 min**, **10 min**, **30 min**, **1 hour**) that schedule a one-off reminder today only without disrupting tomorrow's regular schedule.
- **🔄 Survives Device Reboots**: Registers system broadcasts for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, and `TIMEZONE_CHANGED` to automatically restore all scheduled reminders.
- **🎛️ Master Pause Switch**: Pause all reminders at once (e.g. during doctor adjustments or vacations) without losing any scheduled times.

---

## 🎨 UI/UX & Design Language

TabletTime is designed around an elderly-friendly, anxiety-reducing philosophy:
- **Calm Sage & Mint Palette**: High-contrast, medical-grade sage green tones that provide clarity without looking sterile.
- **Large Touch Targets**: 56–64dp action buttons and readable typography (20–36sp for vital information).
- **Smooth Motion**: Custom spring-animated switches, animated reminder card insertions/deletions, and fluid full-screen transitions.
- **Adaptive Launcher Icons**: Custom vector capsule pill artwork with high-resolution PNG fallbacks across all screen densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).

---

## 🏗️ Architecture & Tech Stack

```
com.poshan.tablettime/
├── alarm/
│   ├── AlarmReceiver.kt       # BroadcastReceiver handling wake locks, sound & heads-up alerts
│   ├── AlarmScheduler.kt      # Precision scheduling via setExactAndAllowWhileIdle()
│   ├── AlarmSoundPlayer.kt    # Custom peaceful chime player with gentle vibration & 60s timeout
│   └── BootReceiver.kt        # Auto-reschedules alarms on reboot or timezone updates
├── data/
│   ├── AppDatabase.kt         # Thread-safe Room database singleton
│   ├── AppSettings.kt         # Key-value persistent settings entity
│   ├── Reminder.kt            # Room entity with bitmask recurrence & trigger calculators
│   └── ReminderDao.kt         # Asynchronous Room DAO with Flow streams
├── ui/
│   ├── checkin/
│   │   └── CheckInActivity.kt # Full-screen lock-screen check-in & snooze workflow
│   ├── components/
│   │   └── CommonComponents.kt# Custom TabletCard, AnimatedSwitch, Day chips & dialogs
│   ├── home/
│   │   └── HomeScreen.kt      # Main dashboard with master switch, hero card & animated list
│   ├── setup/
│   │   └── AddReminderScreen.kt# Material 3 TimePicker, quick label chips & recurrence
│   └── theme/
│       ├── Color.kt           # Sage & Mint color palette (light & dark schemes)
│       ├── Theme.kt           # Material 3 dynamic color scheme application
│       └── Type.kt            # Clean typography hierarchy
├── viewmodel/
│   ├── HomeViewModel.kt       # Dashboard state & master switch reactive logic
│   └── ReminderViewModel.kt   # Reminder form state, validation, and Room persistence
└── MainActivity.kt            # Root Compose host activity & runtime permission handlers
```

### Technical Specifications
| Attribute | Specification |
|---|---|
| **Language** | 100% Kotlin 2.2.10 (Zero Java) |
| **UI Toolkit** | Jetpack Compose (Material 3) |
| **Min SDK** | API 26 (Android 8.0 Oreo) |
| **Target / Compile SDK** | API 35 (Android 15) |
| **Database** | Room 2.6.1 with KSP Kotlin codegen |
| **Architecture** | MVVM with Kotlin Coroutines & `StateFlow` |
| **Scheduling** | Android `AlarmManager` exact RTC wakeups |

---

## 🛠️ Build and Installation

### Prerequisites
- **Android Studio** (Ladybug / Iguana or newer)
- **JDK 21** (Configured as Gradle JDK)
- **Android SDK** with Platform 35 and Build-Tools 36+

### Command Line
```bash
# Clone the repository
git clone https://github.com/<your-username>/tablettime-android.git
cd tablettime-android

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Install directly to USB-connected device
./gradlew installDebug
```

The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Privacy & Permissions

TabletTime respects user autonomy and privacy. It requests only the exact system privileges required for core alarm operation:

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: Ensures medication reminders fire precisely on time.
- `POST_NOTIFICATIONS`: Displays high-priority heads-up reminder notifications on Android 13+.
- `USE_FULL_SCREEN_INTENT`: Allows the check-in screen to wake the phone and appear over the lock screen.
- `RECEIVE_BOOT_COMPLETED`: Automatically restores active reminders after device reboot.
- `WAKE_LOCK` & `VIBRATE`: Briefly turns on the screen and delivers gentle haptic feedback.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Guides the user to ensure vendor-specific battery killers do not kill reminders.

**Network Access**: None. No telemetry, no ads, no crash reporting SDKs.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
