<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

---

🌐 [English](../../README.md) | [日本語](README.ja.md) | [Deutsch](README.de.md) | [Español](README.es.md) | [Italiano](README.it.md) | [简体中文](README.zh-CN.md)

---

<div align="center">
<table>
<tr><td align="center">
<h3>⚠️ Ce dépôt a déménagé ⚠️</h3>
<p>Le développement a été déplacé vers <b><a href="https://codeberg.org/Gonbei774/CalisthenicsMemory">Codeberg</a></b></p>
<p>Ce dépôt GitHub est un <b>miroir en lecture seule</b>.<br>
Pour le code le plus récent, les releases et les contributions, visitez Codeberg.<br>
Les issues sont acceptées ici par commodité.</p>
</td></tr>
</table>
</div>

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

## À propos

Un tracker d'entraînement au poids du corps. Créez des exercices personnalisés, organisez par groupes et niveaux, suivez vos progrès – entièrement hors ligne.

## Fonctionnalités

- **Tableau de bord** - Visualisez les enregistrements d'entraînement du jour en un coup d'œil, appui long pour copier
- **Entièrement personnalisable** - Créez des exercices librement, organisez par groupes, gérez avec 10 niveaux, suivez la distance et le poids par exercice
- **Favoris** - Accès rapide aux exercices fréquemment utilisés
- **Deux modes d'enregistrement**
  - Mode enregistrement : Saisie manuelle rapide avec le bouton "Appliquer les paramètres de l'exercice"
  - Mode entraînement : Entraînement guidé automatique avec paramètres de minuterie par exercice (intervalle de repos, durée de répétition), notification flash LED à la fin de la série
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