<table><tr>
  <td><img src="icon.png" width="80" alt="Calisthenics Memory Icon"></td>
  <td>
    <h1>Calisthenics Memory &nbsp;|&nbsp; <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.ja.md">日本語</a></h1>
    <p>
      <img src="https://img.shields.io/badge/Android-8.0%2B-green.svg" alt="Android 8.0+">
      <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License: GPL-3.0">
      <a href="https://shields.rbtlog.dev/io.github.gonbei774.calisthenicsmemory"><img src="https://shields.rbtlog.dev/simple/io.github.gonbei774.calisthenicsmemory" alt="Reproducible Builds"></a>
    </p>
  </td>
</tr></table>

## About

A privacy-focused bodyweight training tracker. Customize everything to your liking.

## Features

- **To Do** - Plan your workout, tap to jump directly, schedule repeats by day of week
- **Record Mode** - Quick manual input
- **Workout Mode** - Auto-guided with timer, set completion notification
  - Single - Focus on one exercise
  - Program - Create and run multi-exercise routines, flexible navigation
  - Interval - Timed work/rest cycles with customizable rounds
- **Exercise Creation** - Configure dynamic/isometric, unilateral/bilateral, intervals and more
- **Progress Tracking** - Calendar, lists, graphs, challenge status
- **Data Management** - Export/import in JSON/CSV

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

## Official Distribution

This app is officially distributed only through F-Droid, IzzyOnDroid, and Codeberg Releases.
We cannot guarantee the safety of APKs downloaded from any other source.

<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="80" alt="Get it on IzzyOnDroid"></a>
</p>

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/"><img src="https://fdroid.org/badge/get-it-on.png" height="119" alt="Get it on F-Droid"></a>
</p>

<p align="center">
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/releases"><img src="https://get-it-on.codeberg.org/get-it-on-white-on-black.png" height="80" alt="Get it on Codeberg"></a>
</p>

### Verify Signature

APK signing fingerprint (SHA-256):

```
18c00c347ea1001afcdd87258881d24d684047bbeb22c47fbe7b51499516ab54
```

How to verify:

```bash
apksigner verify --print-certs app-release.apk
```

### Verify Checksum

SHA256 checksum: See `app-release.apk.sha256` on the release page.

```bash
sha256sum -c app-release.apk.sha256
```

## Permissions

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - Run timer in background
- `FLASHLIGHT` - Flash notification for rest intervals

See [IzzyOnDroid Permissions](https://android.izzysoft.de/applists/perms) for details.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Wiki

- [Getting Started](https://codeberg.org/Gonbei774/CalisthenicsMemory/wiki/Getting-Started)

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

---

**Last Updated**: March 2, 2026