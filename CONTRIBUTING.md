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

Weblate is intended for natural, native-speaker translations.
Some existing translations may still sound unnatural, and corrections are always welcome.

We kindly ask that machine translation not be used on Weblate.

If you'd like to add a new language (machine translation is fine as a starting point),
please open an [issue](https://codeberg.org/Gonbei774/CalisthenicsMemory/issues) and I'll handle it.

Translation files: [app/src/main/res/](https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/app/src/main/res)

## License

By contributing, you agree that your contributions will be licensed under the [GNU General Public License v3.0](LICENSE).