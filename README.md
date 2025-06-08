# 🌐 SEARC#

## 👥 Équipe 40
🔗 [Fichiers sources](https://github.com/Tyvain/noelmusk/tree/equipe40)

## 🧑‍💻 Membres
- **John WAIA**
- **Jo-Michel UTO**
- **Keys DIANOU**
- **Malcolm BERTAINA**
- **Maxime TONNELIER**
- **Morgan CARRE**
- **Romain CASTELAIN**

## 🎯 Objectifs initiaux
- Permettre la recherche de publications sur différents réseaux sociaux à partir de hashtags ou de mots-clés.  
- Intégrer une nouvelle API sociale en complément de celle déjà existante pour Mastodon.  
- Repenser l'interface utilisateur et l'expérience utilisateur afin de rendre l'application entièrement navigable au clavier.

## 🚀 Fonctionnalités développées

### ✅ Version 1
- Structuration du code de base du projet.
- Ajout des premières commandes de navigation : `Goto`, `Top`, `Bottom`, `Next`, `Previous`, `Play`.
- Implémentation de fonctionnalités diverses : `Link`, `RepliesGreater`, `SortReplies`, `Help`, etc.
- Optimisation des appels à l'API Mastodon.
- Mise en place d'un système de notifications.

### 🚧 Version 2
- Amélioration des commandes `Top` et `Bottom` pour une navigation plus fluide.
- Enrichissement de la commande `Help` pour une meilleure lisibilité.
- Refonte partielle de l'interface utilisateur.
- Affichage de données supplémentaires pour chaque post (likes, réponses, reposts, dates, etc.).
- Ajout de la commande `History` pour naviguer dans l'historique de consultation.

### 🔬 Version 3
- Ajout du scroll automatique lors de la sélection d'un post en dehors de la zone visible.
- Correction de l'affichage de la grille des résultats.
- Intégration de l'API Reddit en complément de Mastodon.
- Affichage d'un badge indiquant la plateforme d'origine du post (Mastodon ou Reddit).
- Refactorisation de la commande `RepliesGreater` en `Filter`, plus générique.
- Ajout de la commande `Open` permettant d'ouvrir un post dans un nouvel onglet.
- Navigation dans l'historique désormais possible via le clavier.
- Ajout d'un écran d'accueil listant l'ensemble des commandes disponibles à l'ouverture du site.
- **⌨️ Navigation Clavier Avancée** :
  - **🔄 Historique des commandes** : Navigation complète dans l'historique avec les flèches haut/bas (comme un terminal)
  - **🎮 Navigation rapide** : 
    - `Flèche gauche` : Post précédent (équivalent à la commande `p`)
    - `Flèche droite` : Post suivant (équivalent à la commande `n`)
  - **⚡ Exécution optimisée** : Touche `Enter` pour exécuter directement les commandes depuis l'historique
  - **🛡️ Prévention auto-exécution** : Système de flag empêchant l'exécution automatique lors de la navigation dans l'historique
  - **💻 Expérience terminal** : Interface entièrement navigable au clavier sans souris

## ⌨️ Raccourcis Clavier

| Touche | Action |
|--------|--------|
| `⬆️ Flèche Haut` | Remonte dans l'historique des commandes |
| `⬇️ Flèche Bas` | Descend dans l'historique des commandes |
| `⬅️ Flèche Gauche` | Post précédent |
| `➡️ Flèche Droite` | Post suivant |
| `Enter` | Exécute la commande |

## 🗂️ Structure du projet
- `components` : composants UI réutilisables (ex : cartes de commandes, badges de plateforme).
- `factory` : classes de fabrique pour instancier les services et commandes sociales.
- `model` : classes de données représentant les entités du projet (posts, comptes, tags, etc.).
- `service` : services dédiés à la récupération et au traitement des données depuis les réseaux sociaux.
- `utils` : fonctions utilitaires pour la gestion de la Grid et des notifications.
- `views.main` : vues principales de l'application côté serveur.
- `views.main.commands` : commandes spécifiques accessibles via l'interface (ex : navigation, tri, recherche, etc.).

## ⚙️ Lancer le projet
Pour démarrer l'application en local :
```bash
mvn spring-boot:run
```

L'application sera accessible à l'adresse : `http://localhost:8080`

## 🎯 Commandes Disponibles

- `search <hashtag>` : Recherche des posts par hashtag
- `n` / `next` : Post suivant
- `p` / `previous` : Post précédent  
- `help` : Affiche l'aide
- `hist` / `history` : Affiche l'historique des commandes
- `clear` : Efface l'affichage
- `open` : Ouvre le post dans un nouvel onglet
- `top` : Va au premier post
- `bottom` : Va au dernier post
- `play` : Lance la lecture automatique
- `stop` : Arrête la lecture automatique

## 🌟 Dernières Améliorations Version 3

La version 3 enrichit considérablement l'expérience utilisateur avec une **navigation entièrement optimisée au clavier**. Plus besoin de taper les commandes répétitives - utilisez simplement les flèches pour naviguer rapidement entre les posts et dans l'historique, transformant l'application en un véritable terminal social interactif !