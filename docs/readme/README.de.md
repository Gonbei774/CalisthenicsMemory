<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

Eine einfache, datenschutzorientierte App zur Verfolgung von Eigengewichtsübungen für Android.

---

<div align="center">
<table>
<tr><td align="center">
<h3>⚠️ Dieses Repository ist umgezogen ⚠️</h3>
<p>Die Entwicklung wurde zu <b><a href="https://codeberg.org/Gonbei774/CalisthenicsMemory">Codeberg</a></b> verlagert</p>
<p>Dieses GitHub-Repository ist ein <b>schreibgeschützter Spiegel</b>.<br>
Für den neuesten Code, Releases und Beiträge besuche bitte Codeberg.<br>
Issues werden hier zur Vereinfachung akzeptiert.</p>
</td></tr>
</table>
</div>

---

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/">
    <img src="https://fdroid.org/badge/get-it-on.png" alt="Jetzt bei F-Droid" height="80">
  </a>
</p>
<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton.png" alt="Jetzt bei IzzyOnDroid" height="54">
  </a>
</p>

---

🌐 [English](../../README.md) | [日本語](README.ja.md) | [Español](README.es.md) | [Français](README.fr.md) | [Italiano](README.it.md) | [简体中文](README.zh-CN.md)

---

## Über die App

Calisthenics Memory hilft dir, Eigengewichtsübungen wie Liegestütze, Klimmzüge und Kniebeugen zu verfolgen und zu verwalten. Erstelle benutzerdefinierte Übungen, organisiere sie in progressiven Stufen und verfolge deinen Fortschritt.

Die App funktioniert komplett offline – keine Internetverbindung erforderlich, keine Werbung, kein Tracking. Deine Daten bleiben nur auf deinem Gerät.

## Hauptmerkmale

- **Vollständige Anpassung** - Keine gesperrten Funktionen für benutzerdefinierte Übungen. Wiederholungen/Zeit, unilateral/bilateral, Ziele, Timer - alles verfügbar für jede Übung
- **Zwei Aufzeichnungsmodi** - Schnelle manuelle Eingabe oder geführtes Training mit Timern
- **Nur offline** - Deine Daten verlassen niemals dein Gerät

## Funktionen

- **Home-Dashboard** - Heutige Trainingseinträge auf einen Blick, lange drücken zum Kopieren
- **Vollständig anpassbar** - Übungen frei erstellen, nach Gruppen organisieren, mit 10 Stufen verwalten, mit Pfeiltasten umsortieren, Distanz und Gewicht pro Übung verfolgen
- **Favoriten** - Schneller Zugriff auf häufig verwendete Übungen
- **Zwei Aufzeichnungsmodi**
  - Aufzeichnungsmodus: Schnelle manuelle Eingabe mit "Übungseinstellungen anwenden"-Taste
  - Trainingsmodus: Automatisch geführtes Training mit übungsspezifischen Timer-Einstellungen (Pausenzeit, Wiederholungsdauer), LED-Blitz-Benachrichtigung bei Satzabschluss
- **Fortschrittsverfolgung** - Einträge als Listen, Grafiken oder Herausforderungs-Fortschrittsbalken anzeigen
- **Unilateral/Bilateral-Unterstützung** - Linke und rechte Seite separat für einseitige Übungen verfolgen
- **Herausforderungsziele** - Zielsätze × Wiederholungen festlegen und Erfüllungsstatus verfolgen
- **Datenverwaltung** - Export/Import im JSON- oder CSV-Format (vollständige Backup-Unterstützung)
- **Mehrsprachig** - Englisch, Japanisch, Spanisch, Deutsch, Chinesisch (vereinfacht), Französisch, Italienisch
- **Datenschutz-orientiert** - Komplett offline, keine Laufzeitberechtigungen, kein Internetzugriff

## Screenshots

<p align="center">
  <img src="../../screenshots/1.png" width="250"><br>
  <b>Home</b> - Heutiges Training auf einen Blick
</p>

<p align="center">
  <img src="../../screenshots/2.png" width="250"><br>
  <b>Übungen</b> - Mit Gruppen & Favoriten organisieren
</p>

<p align="center">
  <img src="../../screenshots/3.png" width="250"><br>
  <b>Aufzeichnen</b> - Schnelle manuelle Eingabe
</p>

<p align="center">
  <img src="../../screenshots/4.png" width="250"><br>
  <b>Training</b> - Geführtes Training mit Timer
</p>

<p align="center">
  <img src="../../screenshots/5.png" width="250"><br>
  <b>Grafik</b> - Fortschritt verfolgen
</p>

<p align="center">
  <img src="../../screenshots/6.png" width="250"><br>
  <b>Herausforderung</b> - Zielerreichungsstatus
</p>

## Anforderungen

- **Android** 8.0 (API 26) oder höher
- **Speicher** ~10MB
- **Internet** Nicht erforderlich

## Berechtigungen

Diese App verwendet nur **normale (Installationszeit-)Berechtigungen**, die bei der Installation automatisch gewährt werden, ohne Benutzeraufforderungen.

Ab v1.9.0 sind folgende Berechtigungen enthalten:

| Berechtigung | Zweck | Hinzugefügt von | Quelle |
|--------------|-------|-----------------|--------|
| `FOREGROUND_SERVICE` | Workout-Timer als Foreground-Service ausführen | App (v1.9.0) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Foreground-Service-Typ für Workout-Timer | App (v1.9.0) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `WAKE_LOCK` | Timer läuft weiter bei ausgeschaltetem Bildschirm | App (v1.8.1) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FLASHLIGHT` | LED-Blitz-Benachrichtigung im Trainingsmodus | App (v1.8.0) | [FlashController.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/util/FlashController.kt) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Sicherheitsschutz für interne Komponenten | AndroidX-Bibliothek (automatisch) | - |

### Was sind normale Berechtigungen?

Android klassifiziert Berechtigungen in zwei Typen:
- **Normale Berechtigungen**: Berechtigungen mit geringem Risiko, die bei der Installation automatisch gewährt werden. Benutzer können sie nicht einzeln widerrufen.
- **Gefährliche Berechtigungen**: Berechtigungen mit hohem Risiko, die eine ausdrückliche Benutzergenehmigung erfordern (z.B. Kamera, Standort, Kontakte).

Diese App fordert keine Laufzeitberechtigungen an.

Weitere Informationen:
- [Übersicht über Android-Berechtigungstypen](https://developer.android.com/guide/topics/permissions/overview)
- [Vollständige Liste der normalen Berechtigungen](https://developer.android.com/reference/android/Manifest.permission)

### Hinweis

Normale Berechtigungen werden automatisch gewährt und erscheinen möglicherweise nicht in App-Store-Auflistungen. Wir dokumentieren sie hier aus Transparenzgründen.

## Erstellen

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

JDK 17 oder höher erforderlich.

## Lizenz

Dieses Projekt ist unter der GNU General Public License v3.0 lizenziert. Siehe [LICENSE](../../LICENSE) für Details.