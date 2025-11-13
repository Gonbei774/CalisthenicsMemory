# Calisthenics Memory

[🇬🇧 English](README.md) | [🇯🇵 日本語](README.ja.md) | [🇪🇸 Español](README.es.md) | [🇩🇪 Deutsch](README.de.md) | [🇨🇳 简体中文](README.zh-CN.md) | [🇫🇷 Français](README.fr.md)

Une application simple et personnalisable pour le suivi d'entraînement au poids du corps

---

## À Propos de Cette Application

Calisthenics Memory est une application Android pour enregistrer et gérer les exercices au poids du corps (calisthénie) tels que les pompes et les squats. Créez des exercices librement, organisez-les en groupes et suivez vos progrès à votre manière.

### Caractéristiques

- **Entièrement Personnalisable** - Créez des exercices librement, organisez par groupes, gérez avec 10 niveaux, enregistrement des favoris
- **Simple** - Fonctionnalités essentielles soigneusement sélectionnées avec une interface intuitive
- **Deux Modes** - Mode d'enregistrement rapide et mode d'entraînement automatique guidé avec minuteur
- **Axé sur la Confidentialité** - Fonctionnement entièrement hors ligne, les données restent uniquement sur votre appareil

---

## Captures d'Écran

### Écran d'Accueil
<p align="center">
  <img src="screenshots/01_home.png" width="250" alt="Écran d'Accueil">
</p>

Écran d'accueil simple et intuitif. Accès rapide à 4 fonctions principales.

---

### ⚙️ Gestion des Exercices

<p align="center">
  <img src="screenshots/02_create_favorites.png" width="250" alt="Gestion des Exercices (Favoris)">
  <img src="screenshots/03_create_edit.png" width="250" alt="Édition d'Exercices">
</p>

**Gauche** : Les exercices favoris sont affichés en haut dans un groupe dédié. Facilement identifiables avec des marques ★.
**Droite** : Écran de création/édition d'exercices. Paramètres flexibles pour le type (répétitions/temps), la latéralité, les défis et les niveaux.

- Organisez hiérarchiquement par groupes
- Gérez progressivement avec les niveaux (1-10)
- Accès rapide aux exercices fréquents avec les favoris

---

### 📝 Fonction d'Enregistrement

<p align="center">
  <img src="screenshots/04_record_select.png" width="250" alt="Sélection d'Exercice">
  <img src="screenshots/05_record_bilateral.png" width="250" alt="Enregistrement d'Exercice Bilatéral">
  <img src="screenshots/06_record_unilateral.png" width="250" alt="Enregistrement d'Exercice Unilatéral">
</p>

**Gauche** : Écran de sélection d'exercice. Organisé clairement avec favoris et groupes hiérarchiques.
**Centre** : Les exercices bilatéraux (pompes régulières, squats, etc.) sont enregistrés simplement.
**Droite** : Les exercices unilatéraux (pistol squats, pompes à un bras, etc.) sont enregistrés séparément pour gauche et droite.

- Nombre de séries librement ajustable
- Ajoutez date, heure et commentaires
- Minimisez l'effort d'enregistrement avec une saisie rapide

---

### 🏋️ Fonction d'Entraînement

<p align="center">
  <img src="screenshots/07_workout_select.png" width="250" alt="Sélection d'Exercice">
  <img src="screenshots/08_workout_config.png" width="250" alt="Configuration d'Entraînement">
  <img src="screenshots/09_workout_progress.png" width="250" alt="Entraînement en Cours">
</p>

Mode d'entraînement automatique guidé :

1. **Sélectionner l'Exercice** - Organisé clairement avec favoris et affichage hiérarchique
2. **Ajuster les Paramètres** - Définissez séries/répétitions cibles, temps par répétition, compte à rebours et repos
3. **Exécuter** - Progression automatique du compte à rebours à l'exécution, repos

Gérez votre rythme simplement en regardant l'écran pour vous concentrer sur l'entraînement. Passez ou arrêtez à mi-chemin, et sauvegardez les enregistrements jusqu'à ce point.

---

### 📊 Fonction de Consultation - Onglet Liste

<p align="center">
  <img src="screenshots/10_view_list.png" width="250" alt="Liste d'Enregistrements">
  <img src="screenshots/11_view_list_unilateral.png" width="250" alt="Détails d'Exercice Unilatéral">
</p>

**Gauche** : Vérifiez les enregistrements d'entraînement passés chronologiquement.
**Droite** : Les exercices unilatéraux affichent les valeurs gauche et droite codées par couleur (vert=droit, violet=gauche).

- Détails de séance (date/heure, contenu des séries, commentaires) en un coup d'œil
- Appuyez pour modifier, bouton supprimer pour effacer
- Filtrer par période (1 semaine/1 mois/3 mois/tout le temps)

---

### 📈 Fonction de Consultation - Onglet Graphique

<p align="center">
  <img src="screenshots/12_view_graph.png" width="250" alt="Graphique (Exercice Unilatéral)">
  <img src="screenshots/13_view_graph_isometric.png" width="250" alt="Graphique (Exercice Isométrique)">
</p>

**Gauche** : Les exercices unilatéraux affichent gauche et droite comme des lignes séparées (vert=droit, violet=gauche).
**Droite** : Les exercices isométriques (Planche, etc.) sont également graphiés. Vérifiez le temps total d'entraînement avec l'affichage de la somme.

- Changez le type de statistique (moyenne/max/total) pour une analyse multifacette
- Filtre de période (1 semaine/1 mois/3 mois/tout le temps)
- Le résumé des statistiques affiche les séries totales, la moyenne, le meilleur et les valeurs les plus basses

---

### 🎯 Fonction de Consultation - Onglet Défi

<p align="center">
  <img src="screenshots/14_view_challenge_complete.png" width="250" alt="Onglet Défi (Complet)">
  <img src="screenshots/15_view_challenge_progress.png" width="250" alt="Onglet Défi (En Cours)">
</p>

Vérifiez visuellement l'état d'atteinte des objectifs. Les barres de progression montrent le progrès en un coup d'œil :

- **100% ou plus** : Parfaitement accompli (✓ marque de réussite affichée)
- **75-99%** : Bonne condition
- **50-74%** : Presque là
- **0-49%** : Continuez

Affichage hiérarchique de tous les groupes y compris les favoris. Filtrez par exercice pour vous concentrer sur la progression d'entraînement spécifique.

---

### ⚙️ Écran des Paramètres

<p align="center">
  <img src="screenshots/16_settings.png" width="250" alt="Écran des Paramètres">
</p>

Fonctions de gestion des données :

**Sauvegarde Complète (JSON)**
- Exportez/importez toutes les données (exercices, groupes, enregistrements)
- Supporte la migration des données lors du changement d'appareil
- ⚠️ Les données existantes sont supprimées lors de l'importation

**Ajouter des Enregistrements (CSV)**
- Téléchargez le modèle pour l'ajout en masse d'enregistrements
- Pratique pour migrer depuis des enregistrements analogiques ou des applications de notes
- Ajouté aux données existantes (non supprimées)

---

## Fonctions Principales

### Enregistrements d'Entraînement
- Enregistrez date, heure, séries, répétitions (ou secondes)
- Supporte les exercices bilatéraux (pompes, etc.) et les exercices unilatéraux (pistol squats, etc.)
- Fonction de commentaires pour des notes sur la forme et les perceptions

### Consultation des Enregistrements
Vérifiez les enregistrements en 3 onglets :

1. **Onglet Liste** - Affichez les enregistrements passés en liste, édition et suppression possible
2. **Onglet Graphique** - Visualisez les progrès par période avec des graphiques (moyenne/max/total)
3. **Onglet Défi** - Vérifiez l'état d'atteinte des objectifs, évalué en 4 étapes

### Paramètres de Défi
- Définissez séries cibles × valeur cible (exemple : 3 séries × 50 répétitions)
- Évaluation flexible de l'atteinte en jugeant avec la somme des N meilleures séries
- La couleur de la barre de progression change selon le taux d'atteinte

### Gestion des Groupes et Favoris
- Groupez les exercices (ex. Pompes, Squats, Tractions, etc.)
- Organisez clairement avec affichage hiérarchique
- Gérez les progrès progressifs avec les niveaux (1-10)
- Affichez les exercices fréquents dans un groupe dédié avec enregistrement des favoris

### Sauvegarde
- Exportez les données au format JSON
- Supporte la migration des données vers un autre appareil ou sauvegarde
- Importez les enregistrements au format CSV (pratique pour migrer depuis des enregistrements analogiques ou des applications de notes)

### Support Multilingue
- Supporte français, anglais, japonais, allemand, espagnol et chinois (simplifié)
- Bascule automatiquement selon les paramètres de langue de l'appareil

---

## Comment Utiliser

### 1. Créer des Exercices
Ajoutez de nouveaux exercices depuis l'écran "Créer" :

1. Entrez le nom de l'exercice (exemple : "Wall Push-up")
2. Sélectionnez le type (Dynamique : basé sur les répétitions / Isométrique : basé sur le temps)
3. Sélectionnez la latéralité (Bilatéral : les deux côtés / Unilatéral : un côté)
4. Sélectionnez le groupe (optionnel)
5. Définissez le niveau (1-10, optionnel)
6. Définissez le défi (séries cibles × valeur cible, optionnel)
7. Enregistrement des favoris avec marque ★ (optionnel)

### 2. Enregistrer l'Entraînement
Ajoutez des enregistrements depuis l'écran "Enregistrer" :

1. Sélectionnez l'exercice (depuis les favoris ou l'affichage hiérarchique)
2. Définissez le nombre de séries
3. Entrez les valeurs pour chaque série
4. Ajoutez un commentaire (optionnel)
5. Vérifiez la date et l'heure (changez si nécessaire)
6. Appuyez sur "Enregistrer"

### 3. Utiliser le Mode Entraînement
Entraînement automatique guidé depuis l'écran "S'entraîner" :

1. Sélectionnez l'exercice
2. Définissez les séries et répétitions cibles
3. Paramètres du minuteur (temps par répétition, compte à rebours, repos)
4. Appuyez sur "Démarrer"
5. Compte à rebours automatique → exécution → repos → série suivante
6. Sauvegardez l'enregistrement après l'achèvement

### 4. Consulter les Enregistrements
Vérifiez les enregistrements passés sur l'écran "Consulter les Enregistrements" :

- **Onglet Liste** : Affichez par séance, édition et suppression possible
- **Onglet Graphique** : Visualisez les progrès avec des graphiques en lignes
- **Onglet Défi** : Affichez l'état d'atteinte des objectifs avec des barres de progression

Filtrez par exercice et période.

### 5. Sauvegarder les Données
Exportez et importez les données depuis l'écran "Paramètres" :

**Sauvegarde Complète (JSON)**
- **Exporter** : "Exporter les Données" → Sélectionnez la destination de sauvegarde
- **Importer** : "Importer les Données" → Sélectionnez le fichier JSON
  - ⚠️ Les données existantes sont supprimées lors de l'importation

**Ajouter des Enregistrements (CSV)**
- **Télécharger le Modèle** : Obtenez le modèle CSV d'enregistrement avec "Exporter le Modèle de Saisie"
- **Importer les Enregistrements** : Ajoutez des enregistrements depuis le fichier CSV avec "Importer les Enregistrements"
  - Ajouté aux données existantes (non supprimées)
  - Pratique pour migrer depuis des enregistrements analogiques ou des applications de notes

---

## Mécanisme de Jugement du Défi

### Règles de Base
L'atteinte de l'objectif est jugée par la **somme des N meilleures séries**.

**Exemple : Lorsque l'objectif est "2 séries × 20 répétitions"**

**Modèle Accompli** :
- 20 rép. + 20 rép. + 5 rép. → Somme des 2 meilleures = 40 rép. (100%)
- 25 rép. + 16 rép. + 10 rép. → Somme des 2 meilleures = 41 rép. (102%)

**Modèle Non Accompli** :
- 15 rép. + 15 rép. + 15 rép. → Somme des 2 meilleures = 30 rép. (75%)

### Pour les Exercices Unilatéraux
Calculez les N meilleures pour gauche et droite respectivement, et évaluez par moyenne.

**Exemple : Lorsque l'objectif est "2 séries × 20 répétitions (par côté)"**

**Les Deux Côtés Accomplis** :
- Droit : 20 rép. + 20 rép. = 40 rép. (100%)
- Gauche : 19 rép. + 21 rép. = 40 rép. (100%)
- **Moyenne : 100%** → Accompli

**Seulement Un Côté Accompli** :
- Droit : 20 rép. + 20 rép. = 40 rép. (100%)
- Gauche : 15 rép. + 15 rép. = 30 rép. (75%)
- **Moyenne : 87.5%** → Non Accompli

### Critères d'Évaluation

La couleur de la barre de progression change selon le taux d'atteinte :
- **100% ou plus** : Parfaitement accompli (✓ marque de réussite affichée)
- **75-99%** : Bonne condition
- **50-74%** : Presque là
- **0-49%** : Continuez

---

## Structure des Écrans

### Écran d'Accueil
Accédez à chaque fonction depuis 4 boutons :
- **Créer** - Gérez les exercices et les groupes
- **Enregistrer** - Entrez les enregistrements d'entraînement
- **S'entraîner** - Entraînement automatique guidé
- **Consulter** - Vérifiez les enregistrements passés

Accédez à l'écran des paramètres depuis le bouton ⚙️ en bas à droite.

### Écran de Gestion des Exercices (Créer)
Gérez les exercices et les groupes, définissez les défis. Les exercices favoris sont affichés avec des marques ★ et placés dans un groupe dédié.

### Écran d'Enregistrement (Enregistrer)
Sélectionnez l'exercice → Entrez les séries et les valeurs → Enregistrez

### Écran d'Entraînement (S'entraîner)
Sélectionnez l'exercice → Paramètres → Préparation → Exécution → Repos → Achèvement → Enregistrer

### Écran de Consultation (Consulter les Enregistrements)
Vérifiez les enregistrements en 3 onglets : Liste / Graphique / Défi

---

## Conseils

### Utilisation Efficace
- **Utilisez les Favoris** : Enregistrez les exercices fréquents pour un accès plus rapide
- **Utilisez les Niveaux** : Gérez les progrès progressifs avec les niveaux 1-10
- **Définissez des Défis** : Des objectifs clairs sont efficaces pour maintenir la motivation
- **Fonction de Commentaires** : Notez les perceptions sur la forme et la condition physique
- **Sauvegardes Régulières** : Exportez et sauvegardez les données

### Comment Lire les Graphiques
- **Moyenne** : Vérifiez la stabilité de l'entraînement
- **Max** : Vérifiez les progrès du record personnel
- **Total** : Vérifiez le volume d'entraînement
- Les exercices unilatéraux affichent gauche et droite comme des lignes séparées (vert=droit, violet=gauche)

---

## Configuration Système Requise

- **OS Supporté** : Android 8.0 (API 26) ou supérieur
- **Stockage** : Environ 10MB
- **Internet** : Non requis (fonctionnement entièrement hors ligne)

---

## Licence

Cette application est publiée sous la Licence Publique Générale GNU v3.0. Consultez le fichier [LICENSE](LICENSE) pour plus de détails.

---

## Installation

### 📥 Téléchargement APK

La dernière version peut être téléchargée depuis [Releases](https://github.com/Gonbei774/CalisthenicsMemory/releases).

**[📦 Télécharger v1.3.0](https://github.com/Gonbei774/CalisthenicsMemory/releases/download/v1.3.0/app-release.apk)**

Si vous avez besoin d'une version précédente, vous pouvez la télécharger depuis la page [Releases](https://github.com/Gonbei774/CalisthenicsMemory/releases).

### ⚠️ Avertissement

Cette application est fournie sans garantie. Distribuée sous la licence GPL-3.0 sur une base "TEL QUEL", sans garantie de commerciabilité ou d'adéquation à un usage particulier. Nous ne sommes pas responsables des dommages résultant de l'utilisation.

### Étapes d'Installation

1. Téléchargez le fichier APK depuis le lien ci-dessus
2. Appuyez sur le fichier téléchargé
3. Autorisez "Installer depuis des sources inconnues" si demandé
4. Installation terminée

### Vérification de Sécurité

Si vous souhaitez vérifier que l'APK n'a pas été modifié, vérifiez la somme de contrôle SHA256 :

```bash
# Calculez SHA256 de l'APK téléchargé
sha256sum app-release.apk

# Comparez avec le SHA256 officiel
# https://github.com/Gonbei774/CalisthenicsMemory/releases/download/v1.3.0/app-release.apk.sha256
```

---

## FAQ

### Q : Quelles méthodes d'entraînement sont supportées ?
R : Toute méthode d'entraînement au poids du corps est supportée, y compris Convict Conditioning, StartBodyweight, ou vos propres programmes personnalisés. Vous pouvez créer des exercices librement, vous pouvez donc l'utiliser selon votre philosophie d'entraînement.

### Q : Où sont stockées les données ?
R : Elles sont stockées dans une base de données locale (SQLite) à l'intérieur de l'application. Elles ne sont jamais envoyées sur Internet et fonctionnent entièrement hors ligne.

### Q : Les sauvegardes sont-elles automatiques ?
R : Non, vous devez exporter manuellement. Nous recommandons des sauvegardes régulières.

### Q : Les données seront-elles supprimées si je désinstalle l'application ?
R : Oui, elles seront supprimées. Assurez-vous d'exporter avant de désinstaller.

### Q : Puis-je l'utiliser sans définir de défis ?
R : Oui, la fonction d'enregistrement peut être utilisée sans paramètres de défi. Elle ne sera pas affichée dans l'onglet défi, mais la liste et les graphiques fonctionnent bien.

### Q : Est-elle multilingue ?
R : Le français, l'anglais, le japonais, l'allemand, l'espagnol et le chinois (simplifié) sont supportés. Bascule automatiquement selon les paramètres de langue de l'appareil.

### Q : Où sont affichés les exercices favoris ?
R : Les exercices favoris sont automatiquement affichés dans le groupe "Favoris". Ils continuent d'être affichés dans leur groupe d'origine, vous pouvez donc y accéder depuis l'un ou l'autre.

---

## Développement

### Instructions de Compilation

```bash
git clone https://github.com/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

### Exigences
- JDK 17 ou supérieur
- Android SDK (API 26 ou supérieur)
- Gradle (inclus dans le projet)

---

**Dernière Mise à Jour** : 13 novembre 2025