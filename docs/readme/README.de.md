<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

<h1 align="center">Calisthenics Memory</h1>

<p align="center">
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/README.md">English</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.ja.md">日本語</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.es.md">Español</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.fr.md">Français</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.it.md">Italiano</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <a href="https://ci.codeberg.org/repos/15682"><img src="https://ci.codeberg.org/api/badges/15682/status.svg" alt="Build Status"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-green.svg" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License: GPL-3.0">
  <img src="https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/yearly/rolling.json&query=$.['io.github.gonbei774.calisthenicsmemory']&label=Downloads" alt="Downloads">
  <a href="https://shields.rbtlog.dev/io.github.gonbei774.calisthenicsmemory"><img src="https://shields.rbtlog.dev/simple/io.github.gonbei774.calisthenicsmemory" alt="Reproducible Builds"></a>
  <a href="https://translate.codeberg.org/engage/calisthenics-memory/"><img src="https://translate.codeberg.org/widget/calisthenics-memory/app/svg-badge.svg" alt="translated"></a>
</p>

<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="80" alt="Jetzt bei IzzyOnDroid"></a>
</p>

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/"><img src="https://fdroid.org/badge/get-it-on.png" height="119" alt="Jetzt bei F-Droid"></a>
</p>

## Über die App

Ein Tracker für Eigengewichtstraining. Erstelle benutzerdefinierte Übungen, organisiere nach Gruppen und Stufen, verfolge deinen Fortschritt – komplett offline.

## Funktionen

- **Home-Dashboard** - Heutige Trainingseinträge auf einen Blick, lange drücken zum Kopieren
- **Vollständig anpassbar** - Übungen frei erstellen, nach Gruppen organisieren, mit 10 Stufen verwalten, Distanz und Gewicht pro Übung verfolgen
- **Favoriten** - Schneller Zugriff auf häufig verwendete Übungen
- **To Do** - Plane dein Training, indem du Übungen zu einer täglichen Aufgabenliste hinzufügst, tippe, um direkt zu den Aufzeichnungs- oder Trainingsbildschirmen zu springen
- **Zwei Aufzeichnungsmodi**
  - Aufzeichnungsmodus: Schnelle manuelle Eingabe mit "Übungseinstellungen anwenden"-Taste
  - Trainingsmodus: Automatisch geführtes Training mit übungsspezifischen Timer-Einstellungen (Pausenzeit, Wiederholungsdauer), LED-Blitz-Benachrichtigung bei Satzabschluss
- **Programm** - Erstelle Routinen mit mehreren Übungen, Timer-AN (countdown-basiert) oder Timer-AUS (eigenes Tempo) Modi, konfigurierbare Pausen zwischen den Sätzen
  - Jump/Redo/Finish-Navigation für flexible Fortschrittskontrolle
  - Speichern & Beenden zum Pausieren und späteren Fortsetzen
  - Vorherige Werte auf Übungskarten als Referenz angezeigt
  - Sätze und Ziele während der Ausführung anpassen
- **Fortschrittsverfolgung** - Einträge als Listen, Grafiken oder Herausforderungs-Fortschrittsbalken anzeigen
- **Unilateral/Bilateral-Unterstützung** - Linke und rechte Seite separat für einseitige Übungen verfolgen
- **Herausforderungsziele** - Zielsätze × Wiederholungen festlegen und Erfüllungsstatus verfolgen
- **Datenverwaltung** - Export/Import im JSON- oder CSV-Format (vollständige Backup-Unterstützung)
- **Mehrsprachig** - Englisch, Japanisch, Spanisch, Deutsch, Chinesisch (vereinfacht), Französisch, Italienisch
- **Datenschutz-orientiert** - Komplett offline, keine gefährlichen Berechtigungen, kein Internetzugriff

## Screenshots

<p align="center">
  <img src="../../screenshots/1.png" width="250">
  <img src="../../screenshots/2.png" width="250">
  <img src="../../screenshots/3.png" width="250">
</p>
<p align="center">
  <img src="../../screenshots/4.png" width="250">
  <img src="../../screenshots/5.png" width="250">
  <img src="../../screenshots/6.png" width="250">
</p>

## Anforderungen

- **Android** 8.0 (API 26) oder höher
- **Speicher** ~10MB
- **Internet** Nicht erforderlich

## Berechtigungen

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - Timer im Hintergrund ausführen
- `FLASHLIGHT` - Blitz-Benachrichtigung für Pausenzeiten

## Erstellen

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

JDK 17 oder höher erforderlich.

## Lizenz

Dieses Projekt ist unter der GNU General Public License v3.0 lizenziert. Siehe [LICENSE](../../LICENSE) für Details.

---

**Zuletzt aktualisiert**: 6. Dezember 2025