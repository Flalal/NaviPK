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
- Lecteur plein écran en bottom sheet avec fond flou de la pochette
- Mini lecteur avec barre de progression, texte défilant et animations

### Interface premium
- Thème dark-first (fond near-black, surfaces sombres)
- Couleurs dynamiques extraites de la pochette en cours (Palette API)
- Transitions de couleurs animées (500ms) lors du changement de morceau
- Bottom NavigationBar 5 onglets (Accueil, Recherche, Favoris, Playlists, Downloads)
- Fond flou album art sur le lecteur plein écran (API 31+, fallback sombre)
- Animations Crossfade sur play/pause et pochette, AnimatedContent sur titre/artiste

### Bibliothèque
- Parcourir les albums (grille avec pochettes, pagination infinie)
- Parcourir les artistes (accessible via chip dans l'accueil)
- Parcourir les playlists
- Détail album avec header pleine largeur et gradient overlay
- Détail artiste avec ses albums
- Détail playlist avec ses morceaux

### YouTube
- Recherche de musique sur YouTube (bascule Navidrome/YouTube)
- Lecture en streaming avec résolution d'URL automatique
- Favoris YouTube stockés localement (JSON)
- Playlists YouTube locales (création, ajout de morceaux, suppression, retrait individuel)
- Import de playlist YouTube publique par URL (avec pagination complète)
- Menu contextuel complet sur les résultats YouTube (favoris, playlist, lire ensuite, ajouter à la file, radio)
- Ajout à une playlist depuis le lecteur plein écran (Navidrome ou YouTube)

### Recherche
- Recherche globale (artistes, albums, morceaux)
- Résultats groupés par catégorie
- Lecture directe depuis les résultats

### Lecture aléatoire (Shuffle)
- Shuffle global (morceaux aléatoires du serveur, accessible via chip dans l'accueil)
- Shuffle par album
- Shuffle par artiste (tous les morceaux de tous ses albums)
- Shuffle par playlist
- Shuffle persistant : bouton toggle dans le lecteur, mélange/restaure la queue sans interrompre la lecture

### Favoris
- Marquer/démarquer un morceau en favori (star/unstar Subsonic)
- Favoris YouTube locaux (stockage JSON, toggle depuis le player ou le menu contextuel)
- Écran dédié listant tous les favoris (morceaux, albums, artistes, YouTube)
- Lecture directe ou aléatoire des favoris

### File d'attente
- "Lire ensuite" : insérer un morceau après le morceau en cours
- "Ajouter à la file" : ajouter un morceau en fin de queue
- File d'attente intégrée dans le lecteur plein écran (slide animé)
- Réorganisation de la queue : flèches haut/bas pour déplacer les morceaux
- Morceau en cours mis en surbrillance

### Cache offline
- Téléchargement de morceaux pour écoute hors-ligne
- Téléchargement par morceau, album entier ou playlist entière
- Indicateur hors-ligne sur chaque morceau caché
- Écran de gestion des téléchargements (taille du cache, suppression)
- Réglage de la taille max du cache dans l'UI (512 Mo à illimité)
- Lecture automatique depuis le cache local si disponible
- Éviction LRU par timestamp

### Playlists
- Parcourir et lire les playlists (Navidrome + locales)
- Playlists mixtes : mélanger morceaux Navidrome et YouTube dans une même playlist locale
- Créer une playlist vide (Navidrome ou locale)
- Supprimer une playlist (avec confirmation)
- Supprimer un morceau individuel d'une playlist locale
- Ajouter un morceau à une playlist existante (depuis le menu contextuel ou le lecteur)
- Import de playlist YouTube publique par URL
- Playlists locales stockées en JSON

### Radio / Mix automatique
- Lancer une radio à partir de n'importe quel morceau (Navidrome ou YouTube)
- Navidrome : morceaux similaires via `getSimilarSongs2`, fallback sur morceaux aléatoires
- YouTube : recherche automatique de morceaux similaires
- Accessible depuis le lecteur plein écran (bouton Radio) et le menu contextuel de chaque morceau

### Navigation
- Bottom NavigationBar 5 onglets avec préservation d'état (saveState/restoreState)
- Accueil avec chips rapides (Artistes, Aléatoire)
- Déconnexion accessible depuis le TopAppBar de l'accueil

## Stack technique

| Composant | Technologie |
|---|---|
| Langage | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Lecture audio | Media3 / ExoPlayer |
| API HTTP | Retrofit + OkHttp |
| Chargement d'images | Coil |
| Navigation | Navigation Compose |
| Couleurs dynamiques | AndroidX Palette |
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
│   ├── PreferencesManager.kt   # Sauvegarde des credentials + settings cache
│   └── YouTubeLibraryManager.kt # Favoris + playlists YouTube locaux (JSON + StateFlow)
├── player/
│   ├── PlaybackService.kt      # Service Media3 (notification, Bluetooth)
│   ├── PlayerManager.kt        # Contrôle de la lecture (play, queue, shuffle, repeat)
│   └── RadioManager.kt         # Génération de file radio (morceaux similaires)
├── ui/
│   ├── login/
│   │   └── LoginScreen.kt      # Écran de connexion
│   ├── library/
│   │   ├── LibraryScreen.kt         # Grille d'albums + chips (Artistes, Aléatoire) + logout
│   │   ├── AlbumDetailScreen.kt     # Header album pleine largeur + gradient + SongItem + PlaylistPickerDialog
│   │   ├── ArtistsScreen.kt         # Liste des artistes
│   │   ├── ArtistDetailScreen.kt    # Albums d'un artiste
│   │   ├── PlaylistsScreen.kt       # Liste des playlists
│   │   ├── PlaylistDetailScreen.kt  # Morceaux d'une playlist
│   │   ├── FavoritesScreen.kt       # Écran des favoris
│   │   └── DownloadsScreen.kt       # Gestion du cache offline
│   ├── search/
│   │   └── SearchScreen.kt     # Recherche globale
│   ├── player/
│   │   ├── PlayerBar.kt        # MiniPlayer (progress bar, marquee, crossfade)
│   │   ├── FullPlayerSheet.kt  # Lecteur plein écran (bottom sheet, fond flou, contrôles)
│   │   └── QueueScreen.kt      # File d'attente (mode normal + overlay dans le player)
│   └── theme/
│       ├── Color.kt                  # Palette dark musicale
│       ├── Theme.kt                  # Thème dynamique animé + shapes custom
│       ├── Type.kt                   # 15 styles typographiques M3
│       └── DynamicColorExtractor.kt  # Extraction couleur dominante via Palette API
└── MainActivity.kt              # Entry point, Bottom NavigationBar, FullPlayerSheet overlay
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
| `createPlaylist.view` | Créer une playlist |
| `deletePlaylist.view` | Supprimer une playlist |
| `search3.view` | Recherche globale |
| `getRandomSongs.view` | Morceaux aléatoires |
| `getSimilarSongs2.view` | Morceaux similaires (radio) |
| `star.view` / `unstar.view` | Ajouter/retirer des favoris |
| `getStarred2.view` | Récupérer les favoris |
| `stream.view` | Streaming audio |
| `getCoverArt.view` | Pochettes d'album |

## Roadmap

*Toutes les features de la v1.5.0 sont livrées.*

## Licence

Projet privé.
