# Habit Tracker

A small offline habit tracker for Android: a single-page web UI (`www/index.html`) in a WebView,
with a home-screen widget. Data is stored on the device.

- Tick habits per day, see streaks, a calendar, a grid, and monthly stats
- Repeatable checklists (e.g. an office-day morning routine) that open on chosen weekdays,
  start fresh each time, and can tick a linked habit when finished

## Build

Requires the Android SDK command-line tools (build-tools 35, platform android-35) and a JDK:

```bash
./build_apk.sh
```

The APK is signed with `android/debug.keystore` (created on first build, not committed).
Download a ready-made APK from the [Releases](../../releases) page.
