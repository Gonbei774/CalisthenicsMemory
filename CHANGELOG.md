# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.19.0] - 2026-02-15

### Added
- **Community Share** - Export and import exercises, programs, and intervals as JSON files
  - Share your workout configurations with other users
  - Preview imported data before applying
  - Automatic backup prompt before importing
- **GROUP type in ToDo** - Manage tasks by exercise group
  - Add entire groups to your ToDo list
  - Group-level checkbox to select all exercises at once
- **Group reordering** with drag-and-drop in exercise management screen
- **Confirmation dialog for ToDo swipe-to-delete** to prevent accidental deletion
- **Per-exercise round completion** displayed in interval record view
- **Start button on interval workout confirm screen** for quicker access
- **Description text for Dynamic/Isometric exercise type selection**
- **50MB file size limit on import** to prevent out-of-memory crashes

### Improved
- **Settings screen reorganized** into separate sections (Backup, CSV Data Management, Share Hub)
- **SAF file picker** for pre-import backup instead of auto-saving to Downloads
- **Adaptive ScrollableTabRow** for better multi-language tab layout support

### Fixed
- Theme switch causing app to lose current screen state and return to home
- Navigation state not preserved across Activity recreation
- Interval screen briefly showing and beeping when interval is 0 seconds
- ToDo add dialog tab layout and content alignment

This release includes contributions from @SomeTr and @nautilusx.

## [1.18.0] - 2026-02-12

### Added
- **Interval mode** - Create timed work/rest cycles with customizable rounds
  - Batch exercise selection, prepare countdown, skip, sound notifications
  - Swipe-to-delete, round grouping, completion screen with comments
  - Interval records integrated into record list view
- **Calendar view tab** for browsing records
  - Sunday-start, chronological order, grid borders, weekly view
  - Period filter with out-of-range date graying and full record display
- **Programs and intervals in ToDo list**
  - Auto-remove on completion, mode selection dialog
- **Day-of-week repeat for ToDo tasks**
- **ToDo tasks in JSON backup and import** (v8)
- **Interval data in JSON backup and import** (v7)
- **Exercise description field** (max 60 chars)
- **Swipe navigation between record view tabs**

### Improved
- Calendar period filter: gray out out-of-range dates, show all records below
- Unified "Start" button translations and ToDo translations for all languages
- Various UI refinements (toggle buttons, timer layout, prepare timer, etc.)

### Fixed
- Long-press on drag handle triggering repeat dialog

This release includes contributions from @SomeTr.

## [1.17.1] - 2026-02-08

### Improved
- Graph type buttons now scroll horizontally
- Clearer auto mode description text

### Fixed
- Assistance tracking not included in backup/export
- Assistance graph Y-axis now starts from 0kg

This release includes contributions from @nautilusx and @SomeTr.

## [1.17.0] - 2026-02-02

### Added
- **Light mode with user-selectable theme option**
  - Choose between Dark, Light, or System default
- **Assistance tracking for band-assisted exercises** (Issue #15)
  - Track band assistance level for exercises like pull-ups
  - Assistance chart displayed alongside main progress chart

### Fixed
- Light mode UI visibility issues (text, buttons, dialogs)
- Assistance chart Y-axis label overlapping with X-axis
- Assistance chart X-axis now aligned with main chart date range
- Exercise type labels now translatable on Add/Edit screen (Issue #28)
- Auto Mode description not updating based on switch state (Issue #25)

This release includes contributions from @nautilusx, @SomeTr, and @balaraz.

## [1.16.1] - 2026-01-20

### Fixed
- App state resetting when rotating the device

This release includes contributions from @SomeTr.

## [1.16.0] - 2026-01-19

### Added
- **Loop feature for Program mode** (Issue #21)
  - Group exercises to repeat multiple rounds
  - Drag-and-drop reordering for loops and exercises within loops
  - Round information displayed during execution
  - Loop support in JSON import/export
- **Ukrainian language support**
- **Exercise search on all screens**
  - Relevance-based ranking for better results

### Improved
- Navigation UX with discard confirmation dialog
- Overall workout progress display (replaces per-exercise set numbers)
- Loop display in navigation sheet and edit screen

### Fixed
- Crash when saving loop workouts in result screen
- Loops now copied when duplicating programs
- Exercise display numbers after reordering
- Loop info preserved when using autofill in preview screen
- Pause function now correctly stops timers in training exercises

This release includes contributions from @unsealed211, @SomeTr, and @balaraz.

## [1.15.0] - 2025-12-31

### Added
- **Program Mode Navigation**
  - Save & Exit option
  - Jump to exercise
  - Redo set
  - Finish early
- **Program Confirmation Screen improvements**
  - Previous values display
  - Collapsible UI
  - Bulk adjustments

## [1.14.1] - 2025-12-14

### Fixed
- **JSON import now preserves program data** (Issue #8)
  - Program configurations are now included in backup/restore
  - Importing backups no longer deletes program data

## [1.14.0] - 2025-12-14

### Added
- **Program duplication feature**
  - Long-press program to access context menu
  - Duplicate creates a copy with "(Copy)" suffix
- **Pre-start editing in Program mode**
  - Edit sets and interval for each exercise in confirm screen
  - Adjustments apply to current session only
- **"Previous Record" button in Program mode**
  - Apply previous training record values with one tap
- **UI improvements for Program edit screen**
  - Auto-scroll to newly added exercise
  - "Add exercise" button fixed at bottom of screen
  - Delete confirmation dialog for programs

### Improved
- Swipe-to-delete animation in program edit screen
- Program/Challenge buttons now update set count along with target values

### Fixed
- Save process stability (race condition resolved)
- Interval settings now saved correctly (was defaulting to 60s)
- Start countdown now respects program settings (ignores global setting)
- Program screen accent colors unified with workout theme

## [1.13.0] - 2025-12-13

### Added
- **Program feature for multi-exercise routines**
  - Create and save programs with multiple exercises
  - Execute exercises in sequence with automatic progression
  - Timer ON mode: Auto-timed workouts with countdown
  - Timer OFF mode: Self-paced workouts with manual completion
  - Support for Pairs and Triplets via duplicate exercise registration
  - Configurable rest intervals between sets
  - Complete result screen with editable values

## [1.12.0] - 2025-12-11

### Added
- **Auto-fill previous record feature**
  - Automatically fills the previous record value when recording sets
  - Reduces manual input for consistent training

### Fixed
- **Today's Workout section scroll**
  - Section now scrolls when content is long
  - "View all records" button always accessible

## [1.11.0] - 2025-12-07

### Added
- **To Do feature for planning workouts**
  - Add exercises to your list and manage your workout plan
  - Reorder tasks by drag-and-drop
  - Swipe to delete tasks
  - Jump directly to Record or Workout screen from each task

### Improved
- Simplified record list UI

## [1.10.0] - 2025-12-05

### Added
- **Distance graph with dual Y-axis display**
  - View distance data alongside reps/time on the graph
  - Invert toggle for exercises where greater distance means higher difficulty
  - Distance scale fixed across all time periods for consistent comparison
- **Volume graph for weight tracking exercises**
  - Daily training volume calculated as reps × weight (kg)
  - Separate left/right display for unilateral exercises
- **Weight statistics in statistics summary**
  - Total volume, max/avg daily volume, max/avg weight
  - Left/right breakdown for unilateral exercises

### Improved
- Training record editing now allows distance and weight value changes
- Workout settings screen now scrollable for better accessibility
- Statistics summary UI simplified for better internationalization support

## [1.9.0] - 2025-12-04

### Added
- **Distance and weight tracking for exercises**
  - Record distance (cm) and weight (kg) per exercise
  - Enable tracking per exercise in exercise settings
- **"Keep Screen On" option for workout mode**
  - Prevents screen from turning off during workouts
  - Toggle available in Settings

### Improved
- **Timer reliability with Foreground Service**
  - Timer continues accurately even when screen is off
  - More reliable background operation

## [1.8.1] - 2025-11-29

### Fixed
- **Timer stops when screen is off**
  - Added WakeLock to keep timer running in background
  - Ensures workout timer continues even with screen off

## [1.8.0] - 2025-11-29

### Added
- **LED flash notification for workout mode**
  - Camera flash blinks when workout sets complete
  - Visual notification even when phone is silent

### Changed
- **Redesigned app info section in Settings**
  - Improved layout with cleaner organization
  - Better visual hierarchy

### Technical
- Updated license screen with JetBrains KMP and Reorderable library entries

## [1.7.1] - 2025-11-26

### Fixed
- **Reproducible build for F-Droid**
  - Excluded timestamp from AboutLibraries metadata
  - Fixes build verification failure on F-Droid and IzzyOnDroid

## [1.7.0] - 2025-11-26

### Added
- **Exercise reordering feature**
  - Reorder exercises within groups using up/down arrow buttons
  - Display order persisted in database (`displayOrder` field)
  - Reordering disabled in Favorites group (to preserve original group order)

- **Per-exercise timer settings**
  - Configure rest interval per exercise (overrides global setting)
  - Configure rep duration per exercise for Dynamic exercises
  - Timer settings shown in full-screen exercise edit dialog

- **Open-source licenses display**
  - View third-party library licenses from Settings > App Info
  - Uses AboutLibraries for accurate license information

### Changed
- **Manual "Apply Exercise Settings" button**
  - Replaced auto-fill feature with explicit button
  - Available in both Record and Workout screens
  - More predictable user experience

- **Full-screen exercise edit dialog**
  - Exercise add/edit now uses full-screen dialog
  - Better keyboard handling with `imePadding`
  - Form sections organized in cards

- **Set interval toggle in Settings**
  - ON/OFF toggle for set interval feature
  - Maximum value limited to 600 seconds (10 minutes)
  - Note about per-exercise settings taking priority

### Technical
- Database schema version updated to 10 (added `displayOrder`, `restInterval`, `repDuration` fields)
- CSV export format updated to 11 columns (backward compatible import)
- Removed `RecordPreferences.kt` (replaced by manual apply button)

## [1.6.0] - 2025-11-24

### Added
- **Home screen dashboard**
  - Display today's training records on home screen
  - Long-press to copy records to clipboard (format: "Exercise: reps/reps/reps")
- **Auto-fill target value feature**
  - Automatically fill target value in record input screen
  - New toggle in Settings to enable/disable this feature
- **Italian language support**
  - Added Italian translation
  - Now supports 7 languages: English, Japanese, German, Spanish, French, Chinese, Italian

### Improved
- **Home screen redesign**
  - Reordered buttons (Record button moved to top)
  - Added "View all records" link
  - Increased title size and spacing
  - Centered content layout
- **Workout settings enhancement**
  - Added enable/disable toggles for preparation time and rep duration
  - Unified dialog colors to orange theme

## [1.5.0] - 2025-11-19

### Added
- **Customizable workout timer settings**
  - Configure preparation time duration
  - Configure rep duration for isometric exercises
  - Adjustable settings in Settings screen

### Improved
- Enhanced app icon quality (converted from WebP to PNG format)
- Simplified README and documentation structure
- Removed redundant multi-language README files
- Streamlined screenshot organization
- UI/UX refinements across the app

## [1.4.0] - 2025-11-15

### Improved
- **Comprehensive CSV import/export enhancement**
  - Previously: Only training records could be exported/imported
  - Now: Full data backup including exercises, groups, and training records
  - Automatic format detection (supports Japanese/English/other language headers)
  - Data preview before import
  - Confirmation dialog before overwriting existing data
  - Export with data verification

- **Enhanced backup/import functionality**
  - Added confirmation dialog before import (warns about data overwrite)
  - Display result messages for backup and import operations
  - Improved user experience with clearer feedback

- **Complete multi-language support**
  - Full support for 6 languages (English, Japanese, German, Spanish, French, Chinese)
  - Added CSV-related strings for all languages
  - Fixed ViewModel messages to follow language changes

- **Workout alarm enhancement**
  - Triple beep pattern for set completion notifications

### Fixed
- Fixed language setting not applied to ViewModel snackbar messages

## [1.3.0] - 2025-11-13

### Added
- In-app language selection feature - change language directly from Settings
- Extended existing period filter to Challenge tab
- Training days count display for each period

### Improved
- Fixed favorite group name to support language switching
- Enhanced challenge tab UI consistency and selection colors
- Fixed achievement rate calculation logic

## [1.2.0] - 2025-11-09

### Added
- Favorite exercise feature with badge display
  - Mark exercises as favorites with star icon
  - Favorites displayed in dedicated group at top of exercise list
  - Star prefix (★) shown in exercise selection screens

### Improved
- Graph data point visibility with smaller and more transparent markers
- UI consistency across workout and exercise setup screens
- Badge styling for better visual consistency
- Overall UI polish and refinement

### Technical
- Database schema version updated to 9 (added `isFavorite` field to Exercise table)

## [1.1.0] - 2025-01-XX

### Added
- CSV import/export feature for training records
- Settings screen with categorized sections

### Improved
- UI compactness for Japanese text
- Graph layout and display area balance
- Settings screen organization

### Fixed
- Export data initialization bug
- String format warnings

## [1.0.0] - 2025-01-XX

### Added
- Initial release
- Training record management (date, time, sets, reps/seconds)
- Unilateral/Bilateral exercise support
- View records with 3 tabs (List, Graph, Challenge)
- Workout feature with automatic guidance
- Challenge achievement evaluation system
- Data export/import (JSON format)
- Multi-language support (Japanese, English)
- Completely offline, privacy-focused design

[1.19.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.19.0
[1.18.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.18.0
[1.17.1]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.17.1
[1.17.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.17.0
[1.16.1]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.16.1
[1.16.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.16.0
[1.15.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.15.0
[1.14.1]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.14.1
[1.14.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.14.0
[1.13.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.13.0
[1.12.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.12.0
[1.11.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.11.0
[1.10.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.10.0
[1.9.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.9.0
[1.8.1]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.8.1
[1.8.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.8.0
[1.7.1]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.7.1
[1.7.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.7.0
[1.6.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.6.0
[1.5.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.5.0
[1.4.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.4.0
[1.3.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.3.0
[1.2.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.2.0
[1.1.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.1.0
[1.0.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.0.0