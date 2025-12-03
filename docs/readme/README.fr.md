<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

Une application simple et respectueuse de la vie privée pour suivre les exercices au poids du corps sur Android.

---

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/">
    <img src="https://fdroid.org/badge/get-it-on.png" alt="Télécharger sur F-Droid" height="80">
  </a>
</p>
<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton.png" alt="Télécharger sur IzzyOnDroid" height="54">
  </a>
</p>

---

🌐 [English](../../README.md) | [日本語](README.ja.md) | [Deutsch](README.de.md) | [Español](README.es.md) | [Italiano](README.it.md) | [简体中文](README.zh-CN.md)

---

## À propos

Calisthenics Memory vous aide à suivre et gérer les exercices au poids du corps comme les pompes, les tractions et les squats. Créez des exercices personnalisés, organisez-les en niveaux progressifs et suivez vos progrès.

L'application fonctionne entièrement hors ligne — aucune connexion Internet requise, pas de publicités, pas de suivi. Vos données restent uniquement sur votre appareil.

## Points Clés

- **Personnalisation complète** - Pas de fonctionnalités verrouillées pour les exercices personnalisés. Répétitions/temps, unilatéral/bilatéral, objectifs, minuteries - tout disponible pour chaque exercice
- **Deux modes d'enregistrement** - Saisie manuelle rapide ou entraînement guidé avec minuteries
- **Hors ligne uniquement** - Vos données ne quittent jamais votre appareil

## Fonctionnalités

- **Tableau de bord** - Visualisez les enregistrements d'entraînement du jour en un coup d'œil, appui long pour copier
- **Entièrement personnalisable** - Créez des exercices librement, organisez par groupes, gérez avec 10 niveaux, réorganisez avec les boutons fléchés, suivez la distance et le poids par exercice
- **Favoris** - Accès rapide aux exercices fréquemment utilisés
- **Deux modes d'enregistrement**
  - Mode enregistrement : Saisie manuelle rapide avec le bouton "Appliquer les paramètres de l'exercice"
  - Mode entraînement : Entraînement guidé automatique avec paramètres de minuterie par exercice (intervalle de repos, durée de répétition), notification flash LED à la fin de la série
- **Suivi des progrès** - Visualisez les enregistrements sous forme de listes, graphiques ou barres de progression des défis
- **Support unilatéral/bilatéral** - Suivez les côtés gauche et droit séparément pour les exercices unilatéraux
- **Objectifs de défi** - Définissez des séries × répétitions cibles et suivez l'état d'accomplissement
- **Gestion des données** - Export/import au format JSON ou CSV (support de sauvegarde complet)
- **Multilingue** - Anglais, japonais, espagnol, allemand, chinois (simplifié), français, italien
- **Confidentialité d'abord** - Entièrement hors ligne, aucune permission d'exécution, pas d'accès Internet

## Captures d'écran

<p align="center">
  <img src="../../screenshots/1.png" width="250"><br>
  <b>Accueil</b> - L'entraînement du jour en un coup d'œil
</p>

<p align="center">
  <img src="../../screenshots/2.png" width="250"><br>
  <b>Exercices</b> - Organisez avec des groupes et favoris
</p>

<p align="center">
  <img src="../../screenshots/3.png" width="250"><br>
  <b>Enregistrement</b> - Saisie manuelle rapide
</p>

<p align="center">
  <img src="../../screenshots/4.png" width="250"><br>
  <b>Entraînement</b> - Entraînement guidé avec minuterie
</p>

<p align="center">
  <img src="../../screenshots/5.png" width="250"><br>
  <b>Graphique</b> - Suivez vos progrès
</p>

<p align="center">
  <img src="../../screenshots/6.png" width="250"><br>
  <b>Défi</b> - État d'accomplissement des objectifs
</p>

## Configuration requise

- **Android** 8.0 (API 26) ou supérieur
- **Stockage** ~10 Mo
- **Internet** Non requis

## Permissions

Cette application utilise uniquement des **permissions normales (au moment de l'installation)**, qui sont automatiquement accordées lors de l'installation sans demande à l'utilisateur.

À partir de v1.9.0, les permissions suivantes sont incluses :

| Permission | Objectif | Ajouté par | Source |
|------------|----------|------------|--------|
| `FOREGROUND_SERVICE` | Exécuter le minuteur d'entraînement en tant que service de premier plan | App (v1.9.0) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Type de service de premier plan pour minuteur d'entraînement | App (v1.9.0) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `WAKE_LOCK` | Maintenir le minuteur actif lorsque l'écran est éteint | App (v1.8.1) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FLASHLIGHT` | Notification flash LED pendant le mode entraînement | App (v1.8.0) | [FlashController.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/util/FlashController.kt) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Protection de sécurité des composants internes | Bibliothèque AndroidX (automatique) | - |

### Que sont les permissions normales ?

Android classe les permissions en deux types :
- **Permissions normales** : Permissions à faible risque accordées automatiquement lors de l'installation. Les utilisateurs ne peuvent pas les révoquer individuellement.
- **Permissions dangereuses** : Permissions à haut risque nécessitant une approbation explicite de l'utilisateur (ex : caméra, localisation, contacts).

Cette application ne demande aucune permission d'exécution.

Pour plus de détails :
- [Aperçu des types de permissions Android](https://developer.android.com/guide/topics/permissions/overview)
- [Liste complète des permissions normales](https://developer.android.com/reference/android/Manifest.permission)

### Note

Les permissions normales sont automatiquement accordées et peuvent ne pas apparaître dans les listes des magasins d'applications. Nous les documentons ici par souci de transparence.

## Compilation

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

JDK 17 ou supérieur requis.

## Licence

Ce projet est sous licence GNU General Public License v3.0. Voir [LICENSE](../../LICENSE) pour les détails.