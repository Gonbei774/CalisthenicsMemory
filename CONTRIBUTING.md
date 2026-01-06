# Contributing

Issues and suggestions are welcome!

- **Codeberg** (main): Issues and Pull Requests
- **GitHub** (mirror): Issues only

https://codeberg.org/Gonbei774/CalisthenicsMemory

## How to Build

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

Requires JDK 17 or higher.

## Project Structure

```
.
├── app/src/main/
│   ├── java/.../calisthenicsmemory/
│   │   ├── data/           # Data layer (Room, Repository)
│   │   ├── viewmodel/      # ViewModel
│   │   ├── ui/
│   │   │   ├── components/ # Reusable UI components
│   │   │   │   ├── common/
│   │   │   │   ├── program/
│   │   │   │   └── single/
│   │   │   ├── screens/    # Screen Composables
│   │   │   │   └── view/
│   │   │   └── theme/      # Theme definitions
│   │   ├── service/        # Foreground service
│   │   └── util/           # Utilities
│   └── res/
│       ├── values*/        # Multi-language resources
│       ├── drawable/       # Images
│       └── xml/            # Configuration XML
├── app/src/test/           # Unit tests
├── app/src/androidTest/    # UI tests
├── docs/readme/            # Multi-language READMEs
├── examples/               # Sample data
├── fastlane/               # Store metadata
└── screenshots/            # Screenshots
```

## Translations

Help translate Calisthenics Memory via [Weblate](https://translate.codeberg.org/projects/calisthenics-memory/):

<a href="https://translate.codeberg.org/engage/calisthenics-memory/">
<img src="https://translate.codeberg.org/widget/calisthenics-memory/multi-auto.svg" alt="Translation status" />
</a>

### Guidelines

- Weblate is intended for natural, native-speaker translations
- Some existing translations may still sound unnatural—corrections are welcome
- Please do not use machine translation on Weblate

If you'd like to add a new language, please open an [issue](https://codeberg.org/Gonbei774/CalisthenicsMemory/issues) before using machine translation. If you can provide a natural, native-speaker translation, you can proceed without opening an issue.

### Source Strings

Source strings (English) are managed in the repository, not Weblate.
Translators cannot add, delete, or edit source strings.

| Component | Source location |
|-----------|-----------------|
| App | [`app/src/main/res/values/strings.xml`](https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/app/src/main/res/values/strings.xml) |
| Fastlane | [`fastlane/metadata/android/en-US/`](https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/fastlane/metadata/android/en-US) |

### Fastlane Limits

| Field | Limit | Note |
|-------|-------|------|
| title | 30 chars | Read-only |
| short_description | 80 chars | |
| full_description | 4000 chars | |

Changelogs are excluded from translation on Weblate.

## License

By contributing, you agree that your contributions will be licensed under the [GNU General Public License v3.0](LICENSE).