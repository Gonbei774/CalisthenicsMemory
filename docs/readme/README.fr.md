<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

<h1 align="center">Calisthenics Memory</h1>

<p align="center">
  <a href="../../README.md">English</a> |
  <a href="README.ja.md">日本語</a> |
  <a href="README.de.md">Deutsch</a> |
  <a href="README.es.md">Español</a> |
  <a href="README.it.md">Italiano</a> |
  <a href="README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <a href="https://ci.codeberg.org/repos/15682"><img src="https://ci.codeberg.org/api/badges/15682/status.svg" alt="Build Status"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-green.svg" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License: GPL-3.0">
  <a href="https://apt.izzysoft.de/packages/io.github.gonbei774.calisthenicsmemory"><img src="https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/monthly/rolling.json&query=$.['io.github.gonbei774.calisthenicsmemory']&label=IzzyOnDroid%20monthly%20downloads" alt="IzzyOnDroid Downloads"></a>
  <a href="https://shields.rbtlog.dev/io.github.gonbei774.calisthenicsmemory"><img src="https://shields.rbtlog.dev/simple/io.github.gonbei774.calisthenicsmemory" alt="Reproducible Builds"></a>
</p>

<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="80" alt="Télécharger sur IzzyOnDroid"></a>
</p>

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/"><img src="https://fdroid.org/badge/get-it-on.png" height="119" alt="Télécharger sur F-Droid"></a>
</p>

## À propos

Un tracker d'entraînement au poids du corps. Créez des exercices personnalisés, organisez par groupes et niveaux, suivez vos progrès – entièrement hors ligne.

## Fonctionnalités

- **Tableau de bord** - Visualisez les enregistrements d'entraînement du jour en un coup d'œil, appui long pour copier
- **Entièrement personnalisable** - Créez des exercices librement, organisez par groupes, gérez avec 10 niveaux, suivez la distance et le poids par exercice
- **Favoris** - Accès rapide aux exercices fréquemment utilisés
- **To Do** - Planifiez votre entraînement en ajoutant des exercices à une liste de tâches quotidienne, appuyez pour accéder directement aux écrans Enregistrement ou Entraînement
- **Deux modes d'enregistrement**
  - Mode enregistrement : Saisie manuelle rapide avec le bouton "Appliquer les paramètres de l'exercice"
  - Mode entraînement : Entraînement guidé automatique avec paramètres de minuterie par exercice (intervalle de repos, durée de répétition), notification flash LED à la fin de la série
- **Programme** - Créez des routines multi-exercices avec les modes Minuteur ON (compte à rebours) ou Minuteur OFF (à votre rythme), intervalles de repos configurables entre les séries
- **Suivi des progrès** - Visualisez les enregistrements sous forme de listes, graphiques ou barres de progression des défis
- **Support unilatéral/bilatéral** - Suivez les côtés gauche et droit séparément pour les exercices unilatéraux
- **Objectifs de défi** - Définissez des séries × répétitions cibles et suivez l'état d'accomplissement
- **Gestion des données** - Export/import au format JSON ou CSV (support de sauvegarde complet)
- **Multilingue** - Anglais, japonais, espagnol, allemand, chinois (simplifié), français, italien
- **Confidentialité d'abord** - Entièrement hors ligne, aucune permission dangereuse, pas d'accès Internet

## Captures d'écran

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

## Configuration requise

- **Android** 8.0 (API 26) ou supérieur
- **Stockage** ~10 Mo
- **Internet** Non requis

## Permissions

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - Exécuter la minuterie en arrière-plan
- `FLASHLIGHT` - Notification flash pour les intervalles de repos

## Compilation

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

JDK 17 ou supérieur requis.

## Licence

Ce projet est sous licence GNU General Public License v3.0. Voir [LICENSE](../../LICENSE) pour les détails.

---

**Dernière mise à jour** : 6 décembre 2025