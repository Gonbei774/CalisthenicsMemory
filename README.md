# Calisthenics Memory

[🇬🇧 English](README.md) | [🇯🇵 日本語](docs/i18n/README.ja.md) | [🇪🇸 Español](docs/i18n/README.es.md) | [🇩🇪 Deutsch](docs/i18n/README.de.md) | [🇨🇳 简体中文](docs/i18n/README.zh-CN.md) | [🇫🇷 Français](docs/i18n/README.fr.md)

A simple and customizable bodyweight training tracking app

---

## About This App

Calisthenics Memory is an Android app for tracking and managing bodyweight exercises (calisthenics) such as push-ups and squats. Create exercises freely, organize them into groups, and track your progress your way.

### Features

- **Fully Customizable** - Create exercises freely, organize by groups, manage with 10 levels, favorite registration
- **Simple** - Carefully selected essential features with an intuitive UI
- **Two Modes** - Speedy record mode and automatic guided workout mode with timer
- **Privacy-Focused** - Completely offline operation, data stays on your device only

---

## Screenshots

### Home Screen
<p align="center">
  <img src="screenshots/01_home.png" width="250" alt="Home Screen">
</p>

Simple and intuitive home screen. Quick access to 4 main features.

---

### ⚙️ Exercise Management

<p align="center">
  <img src="screenshots/02_create_favorites.png" width="250" alt="Exercise Management (Favorites)">
  <img src="screenshots/03_create_edit.png" width="250" alt="Exercise Editing">
</p>

**Left**: Favorite exercises are displayed at the top in a dedicated group. Easily identifiable with ★ marks.
**Right**: Exercise creation/editing screen. Flexible settings for type (reps/time), laterality, challenges, and levels.

- Organize hierarchically by groups
- Manage progressively with levels (1-10)
- Quick access to frequently used exercises with favorites

---

### 📝 Record Feature

<p align="center">
  <img src="screenshots/04_record_select.png" width="250" alt="Exercise Selection">
  <img src="screenshots/05_record_bilateral.png" width="250" alt="Bilateral Exercise Recording">
  <img src="screenshots/06_record_unilateral.png" width="250" alt="Unilateral Exercise Recording">
</p>

**Left**: Exercise selection screen. Organized clearly with favorites and hierarchical groups.
**Center**: Bilateral exercises (regular push-ups, squats, etc.) are recorded simply.
**Right**: Unilateral exercises (pistol squats, one-arm push-ups, etc.) are recorded separately for left and right.

- Freely adjustable number of sets
- Add date, time, and comments
- Minimize recording effort with speedy input

---

### 🏋️ Workout Feature

<p align="center">
  <img src="screenshots/07_workout_select.png" width="250" alt="Exercise Selection">
  <img src="screenshots/08_workout_config.png" width="250" alt="Workout Configuration">
  <img src="screenshots/09_workout_progress.png" width="250" alt="Workout in Progress">
  <img src="screenshots/10_workout_complete.png" width="250" alt="Workout Complete">
</p>

Automatic guided workout mode:

1. **Select Exercise** - Organized clearly with favorites and hierarchical display
2. **Adjust Settings** - Set target sets/reps, time per rep, countdown, and intervals
3. **Execute** - Automatic progression from countdown to execution, intervals
4. **Complete** - Review summary and save records

Manage your pace just by looking at the screen to focus on training. Skip or stop midway, and save records up to that point.

---

### 📊 View Feature - List Tab

<p align="center">
  <img src="screenshots/11_view_list.png" width="250" alt="Record List">
  <img src="screenshots/12_view_list_unilateral.png" width="250" alt="Unilateral Exercise Details">
</p>

**Left**: Check past training records chronologically.
**Right**: Unilateral exercises display left and right values color-coded (green=right, purple=left).

- Session details (date/time, set content, comments) at a glance
- Tap to edit, delete button to remove
- Filter by period (1 week/1 month/3 months/all time)

---

### 📈 View Feature - Graph Tab

<p align="center">
  <img src="screenshots/13_view_graph.png" width="250" alt="Graph (Average)">
  <img src="screenshots/14_view_graph_max.png" width="250" alt="Graph (Max)">
</p>

**Left**: Average statistics view - check training stability. Unilateral exercises display left and right as separate lines (green=right, purple=left).
**Right**: Max statistics view - check progress of personal best records.

- Switch statistics type (average/max/sum) for multifaceted analysis
- Period filter (1 week/1 month/3 months/all time)
- Statistics summary displays total sets, average, best, and lowest values

---

### 🎯 View Feature - Challenge Tab

<p align="center">
  <img src="screenshots/15_view_challenge_complete.png" width="250" alt="Challenge Tab (Complete)">
  <img src="screenshots/16_view_challenge_progress.png" width="250" alt="Challenge Tab (In Progress)">
</p>

Visually check goal achievement status. Progress bars show progress at a glance:

- **100% or more**: Perfectly cleared (✓ achievement mark displayed)
- **75-99%**: Good condition
- **50-74%**: Almost there
- **0-49%**: Keep going

Hierarchical display of all groups including favorites. Filter by exercise to focus on specific training progress.

---

### ⚙️ Settings Screen

<p align="center">
  <img src="screenshots/17_settings.png" width="250" alt="Settings Screen">
  <img src="screenshots/18_settings_language.png" width="250" alt="Language Selection">
</p>

Data management and app configuration:

**Language Selection**
- Change app language directly from Settings
- Available languages: English, Japanese, Spanish, German, Chinese (Simplified), French
- Settings screen (left) and language selection dialog (right)

**Complete Backup (JSON)**
- Export/import all data (exercises, groups, records)
- Support data migration when changing devices
- ⚠️ Existing data is deleted on import

**Add Records (CSV)**
- Download template for bulk record addition
- Convenient for migrating from analog records or note apps
- Added to existing data (not deleted)

---

## Main Features

### Training Records
- Record date, time, sets, reps (or seconds)
- Support bilateral exercises (push-ups, etc.) and unilateral exercises (pistol squats, etc.)
- Comment feature for notes on form and insights

### Viewing Records
Check records in 3 tabs:

1. **List Tab** - Display past records in a list, edit and delete possible
2. **Graph Tab** - Visualize progress by period with graphs (average/max/sum)
3. **Challenge Tab** - Check goal achievement status, evaluated in 4 stages

### Challenge Settings
- Set target sets × target value (example: 3 sets × 50 reps)
- Flexible achievement evaluation by judging with sum of top N sets
- Progress bar color changes according to achievement rate

### Group Management and Favorites
- Group exercises (e.g., Push-ups, Squats, Pull-ups, etc.)
- Organize clearly with hierarchical display
- Manage progressive progress with levels (1-10)
- Display frequently used exercises in a dedicated group with favorite registration

### Backup
- Export data in JSON format
- Support data migration to another device or backup
- Import records in CSV format (convenient for migrating from analog records or note apps)

### Multi-Language Support
- Support English, Japanese, Spanish, German, Chinese (Simplified), and French
- In-app language selection available from Settings screen
- Also automatically switches according to device language settings

---

## How to Use

### 1. Create Exercises
Add new exercises from the "Create" screen:

1. Enter exercise name (example: "Wall Push-up")
2. Select type (Dynamic: rep-based / Isometric: time-based)
3. Select laterality (Bilateral: both sides / Unilateral: one side)
4. Select group (optional)
5. Set level (1-10, optional)
6. Set challenge (target sets × target value, optional)
7. Favorite registration with ★ mark (optional)

### 2. Record Training
Add records from the "Record" screen:

1. Select exercise (from favorites or hierarchical display)
2. Set number of sets
3. Enter values for each set
4. Add comment (optional)
5. Check date and time (change if necessary)
6. Tap "Record"

### 3. Use Workout Mode
Automatic guided training from the "Workout" screen:

1. Select exercise
2. Set target sets and reps
3. Timer settings (time per rep, countdown, interval)
4. Tap "Start"
5. Automatic countdown → execution → interval → next set
6. Save record after completion

### 4. View Records
Check past records on the "View Records" screen:

- **List Tab**: Display by session, edit and delete possible
- **Graph Tab**: Visualize progress with line graphs
- **Challenge Tab**: Display goal achievement status with progress bars

Filter by exercise and period.

### 5. Backup Data
Export and import data from the "Settings" screen:

**Complete Backup (JSON)**
- **Export**: "Export Data" → Select save destination
- **Import**: "Import Data" → Select JSON file
  - ⚠️ Existing data is deleted on import

**Add Records (CSV)**
- **Download Template**: Get record CSV template with "Export Input Template"
- **Import Records**: Add records from CSV file with "Import Records"
  - Added to existing data (not deleted)
  - Convenient for migrating from analog records or note apps

---

## Challenge Judgment Mechanism

### Basic Rules
Goal achievement is judged by the **sum of the top N sets**.

**Example: When the goal is "2 sets × 20 reps"**

**Clear Pattern**:
- 20 reps + 20 reps + 5 reps → Sum of top 2 = 40 reps (100%)
- 25 reps + 16 reps + 10 reps → Sum of top 2 = 41 reps (102%)

**Not Clear Pattern**:
- 15 reps + 15 reps + 15 reps → Sum of top 2 = 30 reps (75%)

### For Unilateral Exercises
Calculate top N for left and right respectively, and evaluate by average.

**Example: When the goal is "2 sets × 20 reps (per side)"**

**Both Sides Clear**:
- Right: 20 reps + 20 reps = 40 reps (100%)
- Left: 19 reps + 21 reps = 40 reps (100%)
- **Average: 100%** → Clear

**Only One Side Clear**:
- Right: 20 reps + 20 reps = 40 reps (100%)
- Left: 15 reps + 15 reps = 30 reps (75%)
- **Average: 87.5%** → Not Clear

### Evaluation Criteria

Progress bar color changes according to achievement rate:
- **100% or more**: Perfectly cleared (✓ achievement mark displayed)
- **75-99%**: Good condition
- **50-74%**: Almost there
- **0-49%**: Keep going

---

## Screen Structure

### Home Screen
Access each feature from 4 buttons:
- **Create** - Manage exercises and groups
- **Record** - Input training records
- **Workout** - Automatic guided training
- **View** - Check past records

Access the settings screen from the ⚙️ button in the bottom right.

### Exercise Management Screen (Create)
Manage exercises and groups, set challenges. Favorite exercises are displayed with ★ marks and placed in a dedicated group.

### Record Screen (Record)
Select exercise → Enter sets and values → Record

### Workout Screen (Workout)
Select exercise → Settings → Preparation → Execution → Interval → Complete → Record

### View Screen (View Records)
Check records in 3 tabs: List / Graph / Challenge

---

## Tips

### Effective Usage
- **Utilize Favorites**: Register frequently used exercises for faster access
- **Utilize Levels**: Manage progressive progress with levels 1-10
- **Set Challenges**: Clear goals are effective for maintaining motivation
- **Comment Feature**: Note insights on form and physical condition
- **Regular Backups**: Export and save data

### How to Read Graphs
- **Average**: Check training stability
- **Max**: Check progress of personal best
- **Sum**: Check training volume
- Unilateral exercises display left and right as separate lines (green=right, purple=left)

---

## System Requirements

- **Supported OS**: Android 8.0 (API 26) or higher
- **Storage**: Approximately 10MB
- **Internet**: Not required (completely offline operation)

---

## License

This app is released under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

---

## Installation

### 📥 APK Download

The latest version can be downloaded from [Releases](https://github.com/Gonbei774/CalisthenicsMemory/releases).

**[📦 Download v1.4.0](https://github.com/Gonbei774/CalisthenicsMemory/releases/download/v1.4.0/app-release.apk)**

If you need a previous version, you can download it from the [Releases](https://github.com/Gonbei774/CalisthenicsMemory/releases) page.

### ⚠️ Disclaimer

This app is provided without warranty. Distributed under the GPL-3.0 license on an "AS IS" basis, with no warranty of merchantability or fitness for a particular purpose. We are not responsible for any damages arising from use.

### Installation Steps

1. Download APK file from the link above
2. Tap the downloaded file
3. Allow "Install from unknown sources" if prompted
4. Installation complete

### Security Verification

If you want to verify that the APK has not been tampered with, please verify the SHA256 checksum:

```bash
# Calculate SHA256 of downloaded APK
sha256sum app-release.apk

# Compare with official SHA256
# https://github.com/Gonbei774/CalisthenicsMemory/releases/download/v1.4.0/app-release.apk.sha256
```

---

## FAQ

### Q: What training methods are supported?
A: Any bodyweight training method is supported, including Convict Conditioning, StartBodyweight, or your own custom programs. You can create exercises freely, so you can use it according to your training philosophy.

### Q: Where is data stored?
A: It is stored in a local database (SQLite) inside the app. It is never sent to the internet and operates completely offline.

### Q: Are backups taken automatically?
A: No, you need to export manually. We recommend regular backups.

### Q: Will data be deleted if I uninstall the app?
A: Yes, it will be deleted. Be sure to export before uninstalling.

### Q: Can I use it without setting challenges?
A: Yes, the record feature can be used without challenge settings. It will not be displayed in the challenge tab, but the list and graphs work fine.

### Q: Is multi-language supported?
A: English, Japanese, Spanish, German, Chinese (Simplified), and French are supported. You can change the language directly from the Settings screen, or it will automatically switch according to device language settings.

### Q: Where are favorite exercises displayed?
A: Favorite exercises are automatically displayed in the "Favorites" group. They continue to be displayed in their original group, so you can access from either.

---

## Development

### Build Instructions

```bash
git clone https://github.com/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

### Requirements
- JDK 17 or higher
- Android SDK (API 26 or higher)
- Gradle (included in the project)

---

**Last Updated**: November 15, 2025