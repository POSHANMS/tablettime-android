# TabletTime 💊

A 100% local, offline-first Android medication reminder app built with modern Kotlin and Jetpack Compose.

## Key Principles

- **Zero Cloud / Zero Network**: No network permissions (`android.permission.INTERNET` is nowhere in the manifest). No Firebase, no analytics, no ads, no trackers.
- **Reliable Exact Alarms**: Utilizes Android's `AlarmManager.setAlarmClock()` to guarantee delivery even in aggressive Doze mode and across app kills.
- **Battery-Friendly Architecture**: No 24/7 foreground services. The app wakes up only when an alarm fires or when the user interacts with it.
- **Survives Device Reboots**: Listens to `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, and `TIMEZONE_CHANGED` to seamlessly restore all scheduled alarms.
- **Elderly-Friendly UI ("Premium but Simple")**:
  - Calming, medical-grade Sage & Mint palette.
  - Large tap targets (minimum 48-64dp) and readable typography (20-36sp for critical info).
  - High contrast for readability.
  - Full-screen lock-screen check-in flow with audio alert and 60-second auto-timeout.
  - Flexible snooze support (5, 10, 15, or 30 minutes).

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.2.10 (100% Kotlin, zero Java)
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Local Storage**: Room 2.6.1 SQLite Database (with KSP Kotlin codegen)
- **Alarm Scheduling**: Android `AlarmManager` (`setAlarmClock`)
- **Min SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)

---

## Project Structure

```
com.poshan.tablettime/
├── alarm/
│   ├── AlarmReceiver.kt       # BroadcastReceiver handling alarm triggers & notifications
│   ├── AlarmScheduler.kt      # Schedules exact AlarmClock intents & snooze alarms
│   ├── AlarmSoundPlayer.kt    # Plays alarm audio with vibration and 60s auto-silence
│   └── BootReceiver.kt        # Re-schedules alarms on device reboot / time zone changes
├── data/
│   ├── AppDatabase.kt         # Room database singleton
│   ├── AppSettings.kt         # Key-value persistent app configuration
│   ├── Reminder.kt            # Reminder entity with bitmask recurrence & time calculators
│   └── ReminderDao.kt         # Room DAO for reminders and settings
├── ui/
│   ├── checkin/
│   │   └── CheckInActivity.kt # Full-screen lock-screen alarm check-in dialog & sound player
│   ├── components/
│   │   └── CommonComponents.kt# Custom UI components (TabletCard, AnimatedSwitch, DaySelectorChips, etc.)
│   ├── home/
│   │   └── HomeScreen.kt      # Main dashboard with master switch, upcoming card, and list
│   ├── setup/
│   │   └── AddReminderScreen.kt# Material3 TimePicker, days selector, label suggestions
│   └── theme/
│       ├── Color.kt           # Sage & Mint color palette
│       ├── Theme.kt           # Material 3 Light/Dark color schemes
│       └── Type.kt            # Typography definitions
├── viewmodel/
│   ├── HomeViewModel.kt       # Dashboard state & master switch logic
│   └── ReminderViewModel.kt   # Reminder creation & update form state
└── MainActivity.kt            # Root activity hosting Compose navigation & permission handling
```

---

## Build and Run

### Prerequisites
- JDK 21
- Android SDK (API 35 platform, Build Tools 36+)

### Command Line
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Install to connected device or emulator
./gradlew installDebug
```

Debug APK location:
`app/build/outputs/apk/debug/app-debug.apk`
