# AGENT.md - NaviPK

## Projet

NaviPK est un lecteur musical Android pour serveurs Navidrome/Subsonic, optimise pour l'utilisation en voiture.

## Architecture

- **Package** : `fr.flal.navipk`
- **Langage** : Kotlin
- **UI** : Jetpack Compose + Material 3
- **Audio** : Media3 / ExoPlayer
- **API** : Retrofit + OkHttp + Gson
- **Navigation** : Navigation Compose (routes string dans `MainActivity.kt`)
- **State** : `StateFlow` dans les singletons (`PlayerManager`, `CacheManager`)

## Structure des fichiers

```
fr.flal.navipk/
├── api/
│   ├── SubsonicApi.kt          # Interface Retrofit (endpoints Subsonic)
│   ├── SubsonicClient.kt       # Singleton HTTP (auth token MD5, URLs stream/coverArt)
│   └── SubsonicModels.kt       # Data classes (Song, Album, Artist, Playlist...)
├── data/
│   ├── CacheManager.kt         # Singleton cache offline (download OkHttp, index JSON, StateFlow)
│   └── PreferencesManager.kt   # SharedPreferences (credentials, taille max cache)
├── player/
│   ├── PlaybackService.kt      # MediaSessionService (notification, Bluetooth)
│   └── PlayerManager.kt        # Singleton lecture (queue, repeat, play/pause, seek)
├── ui/
│   ├── login/
│   │   └── LoginScreen.kt
│   ├── library/
│   │   ├── LibraryScreen.kt         # Grille albums avec pagination infinie
│   │   ├── AlbumDetailScreen.kt     # Detail album + SongItem + PlaylistPickerDialog
│   │   ├── ArtistsScreen.kt
│   │   ├── ArtistDetailScreen.kt
│   │   ├── PlaylistsScreen.kt
│   │   ├── PlaylistDetailScreen.kt
│   │   ├── FavoritesScreen.kt
│   │   └── DownloadsScreen.kt       # Gestion du cache offline
│   ├── search/
│   │   └── SearchScreen.kt
│   ├── player/
│   │   ├── PlayerBar.kt             # Mini lecteur bas d'ecran
│   │   ├── PlayerScreen.kt          # Lecteur plein ecran (repeat, favori, queue)
│   │   └── QueueScreen.kt           # File d'attente (retirer, sauter a un morceau)
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt                  # Entry point, NavBar globale, routes Navigation
```

## Conventions

- **Langue UI** : francais (labels, boutons, messages)
- **Singletons** : `PlayerManager`, `CacheManager`, `SubsonicClient` sont des `object` Kotlin
- **Navigation** : routes string simples (`"library"`, `"album/{albumId}"`, `"player"`, etc.)
- **NavBar globale** : definie dans `MainActivity.kt`, visible sur toutes les pages sauf login
- **TopAppBar** : chaque screen a son propre `TopAppBar` avec `windowInsets = WindowInsets(0, 0, 0, 0)` car la NavBar gere deja les status bar insets
- **SongItem** : composable reutilisable dans `AlbumDetailScreen.kt`, utilise par tous les ecrans de liste de morceaux
- **PlaylistPickerDialog** : dialog de selection de playlist, aussi dans `AlbumDetailScreen.kt`

## Points d'attention

- `CacheManager.init(context)` doit etre appele dans `MainActivity.onCreate()` avant toute utilisation
- `PlayerManager` resout les URI via `CacheManager.getPlaybackUri()` : fichier local si cache, URL distante sinon
- Le cache utilise le stockage interne (`context.filesDir/audio_cache`) - pas de permission necessaire
- L'index du cache est un fichier JSON (`audio_cache/index.json`) serialise avec Gson
- Les `windowInsets` sont geres par la NavBar (status bar) - ne pas ajouter de padding status bar dans les screens

## API Subsonic utilisee

Endpoints dans `SubsonicApi.kt` : `ping`, `getAlbumList2` (avec pagination offset/size), `getAlbum`, `getArtists`, `getArtist`, `getPlaylists`, `getPlaylist`, `updatePlaylist` (ajout de morceaux), `search3`, `getRandomSongs`, `star`, `unstar`, `getStarred2`, `stream.view`, `getCoverArt.view`.
