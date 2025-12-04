<p align="center">
  <img src="icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

A simple, privacy-focused bodyweight training tracker for Android.

---

<div align="center">
<table>
<tr><td align="center">
<h3>⚠️ This Repository Has Moved ⚠️</h3>
<p>Development has moved to <b><a href="https://codeberg.org/Gonbei774/CalisthenicsMemory">Codeberg</a></b></p>
<p>This GitHub repository is a <b>read-only mirror</b>.<br>
For latest code, releases, and contributions, please visit Codeberg.<br>
Issues are accepted here for convenience.</p>
</td></tr>
</table>
</div>

---

🌐 [日本語](docs/readme/README.ja.md) | [Deutsch](docs/readme/README.de.md) | [Español](docs/readme/README.es.md) | [Français](docs/readme/README.fr.md) | [Italiano](docs/readme/README.it.md) | [简体中文](docs/readme/README.zh-CN.md)

---

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/">
    <img src="https://fdroid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">
  </a>
</p>
<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton.png" alt="Get it on IzzyOnDroid" height="54">
  </a>
</p>

---

## About

Calisthenics Memory helps you track and manage bodyweight exercises like push-ups, pull-ups, and squats. Create custom exercises, organize them into progressive levels, and monitor your progress over time.

The app operates completely offline—no internet connection required, no ads, no tracking. Your data stays on your device only.

## Key Points

- **Full customization** - No locked features for custom exercises. Reps/time, unilateral/bilateral, goals, timers - all available for every exercise you create
- **Two recording modes** - Quick manual input or guided workout with timers
- **Offline only** - Your data never leaves your device

## Features

- **Home Dashboard** - View today's training records at a glance, long-press to copy
- **Fully Customizable** - Create exercises freely, organize by groups, manage with 10 levels, reorder with arrow buttons, track distance and weight per exercise
- **Favorites** - Quick access to frequently used exercises
- **Two Recording Modes**
  - Record mode: Quick manual input with "Apply Exercise Settings" button
  - Workout mode: Automatic guided training with per-exercise timer settings (rest interval, rep duration), LED flash notification on set completion
- **Progress Tracking** - View records as lists, graphs, or challenge progress bars
- **Unilateral/Bilateral Support** - Track left and right sides separately for one-sided exercises
- **Challenge Goals** - Set target sets × reps and track achievement status
- **Data Management** - Export/import in JSON or CSV format (complete backup support)
- **Multi-Language** - English, Japanese, Spanish, German, Chinese (Simplified), French, Italian
- **Privacy-First** - Completely offline, no runtime permissions, no internet access

## Screenshots

<p align="center">
  <img src="screenshots/1.png" width="250"><br>
  <b>Home</b> - Today's workout at a glance
</p>

<p align="center">
  <img src="screenshots/2.png" width="250"><br>
  <b>Exercises</b> - Organize with groups & favorites
</p>

<p align="center">
  <img src="screenshots/3.png" width="250"><br>
  <b>Record</b> - Quick manual input
</p>

<p align="center">
  <img src="screenshots/4.png" width="250"><br>
  <b>Workout</b> - Guided training with timer
</p>

<p align="center">
  <img src="screenshots/5.png" width="250"><br>
  <b>Graph</b> - Track your progress
</p>

<p align="center">
  <img src="screenshots/6.png" width="250"><br>
  <b>Challenge</b> - Goal achievement status
</p>

## Requirements

- **Android** 8.0 (API 26) or higher
- **Storage** ~10MB
- **Internet** Not required

## Permissions

This app uses only **normal (install-time) permissions**, which are automatically granted at installation without user prompts.

As of v1.9.0, the following permissions are included:

| Permission | Purpose | Added by | Source |
|------------|---------|----------|--------|
| `FOREGROUND_SERVICE` | Run workout timer as foreground service | App (v1.9.0) | [WorkoutTimerService.kt](app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Workout timer foreground service type | App (v1.9.0) | [WorkoutTimerService.kt](app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `WAKE_LOCK` | Keep timer running when screen is off | App (v1.8.1) | [WorkoutTimerService.kt](app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FLASHLIGHT` | LED flash notification during workout mode | App (v1.8.0) | [FlashController.kt](app/src/main/java/io/github/gonbei774/calisthenicsmemory/util/FlashController.kt) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Security protection for internal components | AndroidX library (automatic) | - |

### What are normal permissions?

Android classifies permissions into two types:
- **Normal permissions**: Low-risk permissions granted automatically at install. Users cannot revoke them individually.
- **Dangerous permissions**: High-risk permissions requiring explicit user approval (e.g., camera, location, contacts).

This app does not request any runtime permissions.

For more details:
- [Android permission types overview](https://developer.android.com/guide/topics/permissions/overview)
- [Complete list of normal permissions](https://developer.android.com/reference/android/Manifest.permission)

### Note

Normal permissions are automatically granted and may not appear in app store listings. We document them here for transparency.

## Building

```bash
git clone https://github.com/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

Requires JDK 17 or higher.

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

---

**Last Updated**: December 4, 2025
