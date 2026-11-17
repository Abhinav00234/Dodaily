# DoDaily

A daily to-do list for Android that lives on your home screen. Check tasks off, add new ones, and get reminders without opening the app.

[![Build APK](https://github.com/Abhinav00234/Dodaily/actions/workflows/android.yml/badge.svg)](https://github.com/Abhinav00234/Dodaily/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Website:** https://abhinav00234.github.io/Dodaily/
**Download:** [latest APK](https://github.com/Abhinav00234/Dodaily/releases/latest)

## Features

- **Home-screen widget:** today's list with real checkboxes, progress, and your streak
- **Quick add:** tap **+** on the widget and a small popup lets you type a task without opening the app
- **Repeat rules:** daily, weekdays, any days you pick, or one-time (one-time tasks carry over until done)
- **Reminders:**
  - Morning summary of today's list
  - Evening nudge if anything is left
  - Optional per-task reminder with a **Mark done** button on the notification
- **Skip today / move to tomorrow:** tap a task title on the widget
- **Streaks:** consecutive days where everything got done
- Material You colors, dark mode, works offline, no account, no tracking

## Install

1. Download the APK from [Releases](https://github.com/Abhinav00234/Dodaily/releases/latest) on your phone.
2. Open it and allow "Install unknown apps" for your browser when asked.
3. Long-press your home screen → **Widgets** → **DoDaily** → drag it onto the home screen.
4. Open the app once and tap **Allow** on the banners (notifications, exact alarms, battery).

> Samsung, Xiaomi, OnePlus and similar phones kill background alarms aggressively. Set DoDaily's battery usage to **Unrestricted** or reminders may arrive late.

## Build from source

Requirements: Android Studio (Ladybug or newer) or JDK 17 + Android SDK 34.

```bash
git clone https://github.com/Abhinav00234/Dodaily.git
cd Dodaily
./gradlew assembleDebug      # APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # schedule/streak logic tests
```

Every push to `main` builds an APK in GitHub Actions (check the run's **Artifacts**). Pushing a tag like `v0.2.0` publishes a release with the APK attached.

## How it's built

| Piece | Tech |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Widget | Jetpack Glance |
| Storage | Room (SQLite) |
| Reminders | AlarmManager exact alarms, re-armed after reboot |

```
app/src/main/java/com/abhinav/dodaily/
├── data/        Room entities, DAO, repository
├── logic/       Pure scheduling + streak rules (unit-tested)
├── widget/      Glance home-screen widget + checkbox action
├── reminders/   Alarm scheduling, notifications, boot receiver
└── ui/          Quick-add/edit popup, theme
```

**Data model:** a `Task` repeats on a 7-bit day mask (Mon = bit 0) or has a one-time date. Each tick-off is a `Completion(taskId, day)` row. Nothing gets reset at midnight: "today" is just a query, so history and streaks come for free.

## Roadmap

- [ ] Drag to reorder
- [ ] History / calendar view
- [ ] Backup & restore (JSON export)
- [ ] Widget size variants (compact 2×2)
- [ ] Play Store / F-Droid release

## Contributing

PRs and issues are welcome. Keep scheduling rules in `logic/Schedule.kt`, and add a test in `ScheduleTest.kt` for any rule change.

## License

[MIT](LICENSE) © 2026 Abhinav Sharma
