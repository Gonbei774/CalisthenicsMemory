<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

---

🌐 [English](../../README.md) | [日本語](README.ja.md) | [Español](README.es.md) | [Français](README.fr.md) | [Italiano](README.it.md) | [简体中文](README.zh-CN.md)

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

## Über die App

Ein Tracker für Eigengewichtstraining. Erstelle benutzerdefinierte Übungen, organisiere nach Gruppen und Stufen, verfolge deinen Fortschritt – komplett offline.

## Funktionen

- **Home-Dashboard** - Heutige Trainingseinträge auf einen Blick, lange drücken zum Kopieren
- **Vollständig anpassbar** - Übungen frei erstellen, nach Gruppen organisieren, mit 10 Stufen verwalten, Distanz und Gewicht pro Übung verfolgen
- **Favoriten** - Schneller Zugriff auf häufig verwendete Übungen
- **Zwei Aufzeichnungsmodi**
  - Aufzeichnungsmodus: Schnelle manuelle Eingabe mit "Übungseinstellungen anwenden"-Taste
  - Trainingsmodus: Automatisch geführtes Training mit übungsspezifischen Timer-Einstellungen (Pausenzeit, Wiederholungsdauer), LED-Blitz-Benachrichtigung bei Satzabschluss
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