# Yakachercher

**Équipe 30**  
**URL projet :** [https://yakachercher.lesbroo.cloud](https://yakachercher.lesbroo.cloud)  
**GitHub :** [https://github.com/Tyvain/noelmusk/tree/equipe30](https://github.com/Tyvain/noelmusk/tree/equipe30)

---

## Objectifs initiaux

- Rechercher des tags via ligne de commande  
- Utiliser l’API de Mastodon et de Reddit
- Extraire et structurer les données de publication  
- Identifier les contenus explicites  

---

## Fonctionnalités développées

### V1

- Mise en place de l'interface
- Mise en forme sous tableau
- Fonction de tri des postes par date et like
- Ajout de la commande ET et OU
- Page d'accueil avec un Help
- Fonction Clear et Historique
- Navigation avec les fleches pour l'historique et possibilité d'entrer une ancienne commande

### V2

- Application sans scroll avec un affichage des posts 10 par 10
- Navigation avec les flèches entre les pages des postes
- Affichage du nombres de pages lors d'une recherche et des ULR des images des posts individuels
- Ajout de la commande "view" qui permet d'ouvrir un post dans un nouvel onglet
- Ajout de l'API de recherche Reddit
- Ajout des Icones pour distinguer les posts Mastodon et Reddit

### V3

- Ajout du nombre de commentaires des posts
- Filtre par commentaires et Likes
- Ajout d'un dockerfile pour la conteneurisation du projet
- Hebergement perso sur Serveur et configuration DNS 
- Ajout de commandes nextpage, previouspage pour changer de page de recherche.
---

### Ajout supplémentaire

- Filtre anti-NSFW
---

## Architecture

### `MastodonPost.java`

- Étendre `Post`  
- Convertir JSON en objet Java  
- Gérer les champs : id, date, auteur, contenu, médias, likes, replies, visibilité  
- Marquer les contenus explicites  

### `RedditPost.java`

- Étendre `Post`  
- Gérer les champs spécifiques à Reddit (titre, subreddit, etc.)  
- Convertir JSON en objet Java  
- Marquer les contenus explicites  

### `Post.java`

- Classe abstraite de base pour unifier les posts de différentes sources  

### `MainView.java`

- Interface utilisateur principale construite avec Vaadin  
- Gérer l'affichage des posts, la navigation, les commandes utilisateur (recherche, tri, navigation, etc.)  
- Intégrer la logique d'appel aux API Mastodon et Reddit  
- Implémenter le filtrage par mots-clés sensibles et la gestion du mode NSFW  
- Utiliser des expressions régulières pour extraire les options de recherche (`-c` pour commentaires minimum, `-l` pour likes minimum)  

### `Application.java`

- Point d'entrée de l'application Spring Boot  
- Configurer le thème Vaadin (Lumo Dark)  

## Docker

### À quoi sert le Dockerfile

Le `Dockerfile` sert à :

- Construire une image Docker contenant toute l'application Yakachercher  
- Standardiser l’environnement (Java 17, Maven) pour éviter les problèmes de configuration  
- Déployer facilement l’application sur tout système compatible Docker  
- Exécuter l’application sans installer Java ni Maven localement  

### Comment l’utiliser

```bash
# Construire l’image Docker depuis le Dockerfile
docker build -t yakachercher .

# Lancer l’application dans un conteneur
docker run -p 8080:8080 yakachercher
```

L'application sera accessible via : [http://localhost:8080](http://localhost:8080)
---

## Moteur de recherche

Permet de :

- Rechercher sur les réseaux (actuellement Mastodon et Reddit)  
- Extraire les hashtags ou mots-clés des requêtes  
- Classifier les contenus (Sûr, Sensible, NSFW) en se basant sur une liste de mots-clés explicites  
- Filtrer par mots-clés sensibles, likes minimum (`-l X`), et commentaires minimum (`-c Y`)  
- Gérer les opérateurs `ET (&)` et `OU` pour les recherches multi-tags  

---

## Membres et rôles

- **Olivier TRAM** : Gère le filtre de recherche.  
- **Olivier DINAN** : Gère le filtre anti-NSFW et la recherche sur Reddit.
- **Charles-Edouard QUERLIER** : Gère la partie interface de l'application.  
- **Léo VANHAECKE** : Gère l'hébergement de l'application et la section recherche avancée.
- **Warren KELEKELE** : Gère l'esthétique de la page 

---

## Lancer le projet

```bash
# Lancer localement
./mvnw spring-boot:run

# Accéder à l'application
http://localhost:8080
