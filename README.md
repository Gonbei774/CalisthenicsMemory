<p align="center">
  <img src="icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

<h1 align="center">Calisthenics Memory</h1>

<p align="center">
  <a href="docs/readme/README.ja.md">日本語</a> |
  <a href="docs/readme/README.de.md">Deutsch</a> |
  <a href="docs/readme/README.es.md">Español</a> |
  <a href="docs/readme/README.fr.md">Français</a> |
  <a href="docs/readme/README.it.md">Italiano</a> |
  <a href="docs/readme/README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <a href="https://ci.codeberg.org/repos/15682"><img src="https://ci.codeberg.org/api/badges/15682/status.svg" alt="Build Status"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-green.svg" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License: GPL-3.0">
  <a href="https://apt.izzysoft.de/packages/io.github.gonbei774.calisthenicsmemory"><img src="https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/monthly/rolling.json&query=$.['io.github.gonbei774.calisthenicsmemory']&label=IzzyOnDroid%20monthly%20downloads" alt="IzzyOnDroid Downloads"></a>
  <a href="https://shields.rbtlog.dev/io.github.gonbei774.calisthenicsmemory"><img src="https://shields.rbtlog.dev/simple/io.github.gonbei774.calisthenicsmemory" alt="Reproducible Builds"></a>
</p>

<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="80" alt="Get it on IzzyOnDroid"></a>
</p>

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/"><img src="https://fdroid.org/badge/get-it-on.png" height="119" alt="Get it on F-Droid"></a>
</p>

## About

A bodyweight training tracker. Create custom exercises, organize by groups and levels, track your progress—completely offline.

## Features

- **Home Dashboard** - View today's training records at a glance, long-press to copy
- **Fully Customizable** - Create exercises freely, organize by groups, manage with 10 levels, track distance and weight per exercise
- **Favorites** - Quick access to frequently used exercises
- **To Do** - Plan your workout by adding exercises to a daily task list, tap to jump directly to Record or Workout screens
- **Two Recording Modes**
  - Record mode: Quick manual input with "Apply Exercise Settings" button
  - Workout mode: Automatic guided training with per-exercise timer settings (rest interval, rep duration), LED flash notification on set completion
- **Program** - Create multi-exercise routines with Timer ON (countdown-based) or Timer OFF (self-paced) modes, configurable rest intervals between sets
- **Progress Tracking** - View records as lists, graphs, or challenge progress bars
- **Unilateral/Bilateral Support** - Track left and right sides separately for one-sided exercises
- **Challenge Goals** - Set target sets × reps and track achievement status
- **Data Management** - Export/import in JSON or CSV format (complete backup support)
- **Multi-Language** - English, Japanese, Spanish, German, Chinese (Simplified), French, Italian
- **Privacy-First** - Completely offline, no dangerous permissions, no internet access

## Screenshots

<p align="center">
  <img src="screenshots/1.png" width="250">
  <img src="screenshots/2.png" width="250">
  <img src="screenshots/3.png" width="250">
</p>
<p align="center">
  <img src="screenshots/4.png" width="250">
  <img src="screenshots/5.png" width="250">
  <img src="screenshots/6.png" width="250">
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

**Last Updated**: December 6, 2025