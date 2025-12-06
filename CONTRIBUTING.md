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
│   │   │   ├── screens/    # Screen Composables
│   │   │   │   └── view/   # Shared UI components
│   │   │   └── theme/      # Theme definitions
│   │   ├── service/        # Foreground service
│   │   └── util/           # Utilities
│   └── res/
│       ├── values*/        # Multi-language resources (7 languages)
│       ├── drawable/       # Images
│       └── xml/            # Configuration XML
├── app/src/test/           # Unit tests
├── app/src/androidTest/    # UI tests
├── docs/readme/            # Multi-language READMEs
├── examples/               # Sample data
├── fastlane/               # F-Droid metadata
└── screenshots/            # Screenshots
```

## License

By contributing, you agree that your contributions will be licensed under the [GNU General Public License v3.0](LICENSE).