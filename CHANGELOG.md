# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[1.7.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.7.0
[1.6.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.6.0
[1.5.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.5.0
[1.4.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.4.0
[1.3.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.3.0
[1.2.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.2.0
[1.1.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.1.0
[1.0.0]: https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v1.0.0