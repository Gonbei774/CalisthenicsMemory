<p align="center">
  <img src="icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

A simple, privacy-focused bodyweight training tracker for Android.

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

A bodyweight training tracker. Create custom exercises, organize by groups and levels, track your progress—completely offline.

## Features

- **Home Dashboard** - View today's training records at a glance, long-press to copy
- **Fully Customizable** - Create exercises freely, organize by groups, manage with 10 levels, track distance and weight per exercise
- **Favorites** - Quick access to frequently used exercises
- **Two Recording Modes**
  - Record mode: Quick manual input with "Apply Exercise Settings" button
  - Workout mode: Automatic guided training with per-exercise timer settings (rest interval, rep duration), LED flash notification on set completion
- **Progress Tracking** - View records as lists, graphs, or challenge progress bars
- **Unilateral/Bilateral Support** - Track left and right sides separately for one-sided exercises
- **Challenge Goals** - Set target sets × reps and track achievement status
- **Data Management** - Export/import in JSON or CSV format (complete backup support)
- **Multi-Language** - English, Japanese, Spanish, German, Chinese (Simplified), French, Italian
- **Privacy-First** - Completely offline, no dangerous permissions, no internet access

## Screenshots

<p align="center">
  <img src="screenshots/1.png" width="150">
  <img src="screenshots/2.png" width="150">
  <img src="screenshots/3.png" width="150">
</p>
<p align="center">
  <img src="screenshots/4.png" width="150">
  <img src="screenshots/5.png" width="150">
  <img src="screenshots/6.png" width="150">
</p>

## Requirements

- **Android** 8.0 (API 26) or higher
- **Storage** ~10MB
- **Internet** Not required

## Permissions

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - Run timer in background
- `FLASHLIGHT` - Flash notification for rest intervals

## Building

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

Requires JDK 17 or higher.

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

---

**Last Updated**: December 4, 2025