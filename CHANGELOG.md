# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[1.4.0]: https://github.com/Gonbei774/CalisthenicsMemory/releases/tag/v1.4.0
[1.3.0]: https://github.com/Gonbei774/CalisthenicsMemory/releases/tag/v1.3.0
[1.2.0]: https://github.com/Gonbei774/CalisthenicsMemory/releases/tag/v1.2.0
[1.1.0]: https://github.com/Gonbei774/CalisthenicsMemory/releases/tag/v1.1.0
[1.0.0]: https://github.com/Gonbei774/CalisthenicsMemory/releases/tag/v1.0.0