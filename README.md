# NaviPK

Application Android de lecture musicale compatible avec les serveurs [Navidrome](https://www.navidrome.org/) / Subsonic.

## Fonctionnalités

### Lecture audio
- Streaming depuis un serveur Navidrome/Subsonic via HTTPS
- Contrôles play/pause, suivant, précédent
- Barre de progression avec seek
- Mode repeat (off / tout répéter / répéter un)
- Notification media (écran de verrouillage)
- Contrôles Bluetooth (volant, autoradio)
- Lecteur plein écran avec pochette d'album

### Bibliothèque
- Parcourir les albums (grille avec pochettes, pagination infinie)
- Parcourir les artistes
- Parcourir les playlists
- Détail album avec liste des morceaux
- Détail artiste avec ses albums
- Détail playlist avec ses morceaux

### Recherche
- Recherche globale (artistes, albums, morceaux)
- Résultats groupés par catégorie
- Lecture directe depuis les résultats

### Lecture aléatoire (Shuffle)
- Shuffle global (morceaux aléatoires du serveur)
- Shuffle par album
- Shuffle par artiste (tous les morceaux de tous ses albums)
- Shuffle par playlist

### Favoris
- Marquer/démarquer un morceau en favori (star/unstar Subsonic)
- Écran dédié listant tous les favoris (morceaux, albums, artistes)
- Lecture directe ou aléatoire des favoris

### File d'attente
- "Lire ensuite" : insérer un morceau après le morceau en cours
- "Ajouter à la file" : ajouter un morceau en fin de queue
- Écran file d'attente : voir, retirer ou sauter à un morceau
- Morceau en cours mis en surbrillance

### Cache offline
- Téléchargement de morceaux pour écoute hors-ligne
- Téléchargement par morceau, album entier ou playlist entière
- Indicateur hors-ligne sur chaque morceau caché
- Écran de gestion des téléchargements (taille du cache, suppression)
- Lecture automatique depuis le cache local si disponible
- Éviction LRU par timestamp

### Playlists
- Parcourir et lire les playlists
- Ajouter un morceau à une playlist existante (depuis le menu contextuel)

### Navigation
- Barre de navigation globale avec icônes (visible sur toutes les pages)
- Accès rapide : Accueil, Recherche, Shuffle, Favoris, Artistes, Playlists, Téléchargements

## Stack technique

| Composant | Technologie |
|---|---|
| Langage | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Lecture audio | Media3 / ExoPlayer |
| API HTTP | Retrofit + OkHttp |
| Chargement d'images | Coil |
| Navigation | Navigation Compose |
| Architecture | MVVM (ViewModel + StateFlow) |
| Stockage local | SharedPreferences + cache fichiers interne |

## Prérequis

- Android Studio (dernière version)
- Android SDK 26+ (Android 8.0 minimum)
- Un serveur Navidrome ou compatible Subsonic

## Installation

```bash
git clone https://github.com/Flalal/NaviPK.git
```

1. Ouvrir le projet dans Android Studio
2. Attendre le Gradle Sync
3. Brancher un appareil Android en USB (mode développeur + débogage USB activés)
4. Cliquer sur Run

## Configuration

Au premier lancement, l'écran de connexion demande :

- **URL du serveur** : l'adresse de votre Navidrome (ex: `https://music.example.com`)
- **Nom d'utilisateur** : votre login Navidrome
- **Mot de passe** : votre mot de passe Navidrome

Les identifiants sont sauvegardés localement. La connexion utilise l'authentification par token MD5 (salt + hash) de l'API Subsonic.

## Structure du projet

```
fr.flal.navipk/
├── api/
│   ├── SubsonicApi.kt          # Interface Retrofit (endpoints)
│   ├── SubsonicClient.kt       # Client HTTP singleton (auth, URLs)
│   └── SubsonicModels.kt       # Modèles de données (Album, Song, Artist...)
├── data/
│   ├── CacheManager.kt         # Cache offline (téléchargement, index JSON, éviction)
│   └── PreferencesManager.kt   # Sauvegarde des credentials + settings cache
├── player/
│   ├── PlaybackService.kt      # Service Media3 (notification, Bluetooth)
│   └── PlayerManager.kt        # Contrôle de la lecture (play, queue, shuffle, repeat)
├── ui/
│   ├── login/
│   │   └── LoginScreen.kt      # Écran de connexion
│   ├── library/
│   │   ├── LibraryScreen.kt         # Grille d'albums (pagination infinie)
│   │   ├── AlbumDetailScreen.kt     # Détail album + SongItem + PlaylistPickerDialog
│   │   ├── ArtistsScreen.kt         # Liste des artistes
│   │   ├── ArtistDetailScreen.kt    # Albums d'un artiste
│   │   ├── PlaylistsScreen.kt       # Liste des playlists
│   │   ├── PlaylistDetailScreen.kt  # Morceaux d'une playlist
│   │   ├── FavoritesScreen.kt       # Écran des favoris
│   │   └── DownloadsScreen.kt       # Gestion du cache offline
│   ├── search/
│   │   └── SearchScreen.kt     # Recherche globale
│   ├── player/
│   │   ├── PlayerBar.kt        # Mini lecteur (bas de l'écran)
│   │   ├── PlayerScreen.kt     # Lecteur plein écran
│   │   └── QueueScreen.kt      # File d'attente
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt              # Point d'entrée + NavBar globale + Navigation
```

## API Subsonic

L'application utilise les endpoints suivants de l'API Subsonic (v1.16.1) :

| Endpoint | Usage |
|---|---|
| `ping.view` | Test de connexion |
| `getAlbumList2.view` | Liste des albums (avec pagination) |
| `getAlbum.view` | Détail d'un album + morceaux |
| `getArtists.view` | Liste des artistes |
| `getArtist.view` | Détail d'un artiste + albums |
| `getPlaylists.view` | Liste des playlists |
| `getPlaylist.view` | Détail d'une playlist + morceaux |
| `updatePlaylist.view` | Ajouter un morceau à une playlist |
| `search3.view` | Recherche globale |
| `getRandomSongs.view` | Morceaux aléatoires |
| `star.view` / `unstar.view` | Ajouter/retirer des favoris |
| `getStarred2.view` | Récupérer les favoris |
| `stream.view` | Streaming audio |
| `getCoverArt.view` | Pochettes d'album |

## Licence

Projet privé.
