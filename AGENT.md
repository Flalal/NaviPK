# AGENT.md - NaviPK

## Projet

NaviPK est un lecteur musical Android pour serveurs Navidrome/Subsonic, optimise pour l'utilisation en voiture.

## Architecture

- **Package** : `fr.flal.navipk`
- **Langage** : Kotlin
- **UI** : Jetpack Compose + Material 3
- **Audio** : Media3 / ExoPlayer
- **API** : Retrofit + OkHttp + Gson
- **Navigation** : Navigation Compose (routes string dans `MainActivity.kt`) + Bottom NavigationBar 5 onglets
- **State** : `StateFlow` dans les singletons (`PlayerManager`, `CacheManager`, `YouTubeLibraryManager`)
- **Theme** : Dark-first avec couleurs dynamiques extraites de la pochette via Palette API

## Structure des fichiers

```
fr.flal.navipk/
├── api/
│   ├── SubsonicApi.kt          # Interface Retrofit (endpoints Subsonic)
│   ├── SubsonicClient.kt       # Singleton HTTP (auth token MD5, URLs stream/coverArt)
│   └── SubsonicModels.kt       # Data classes (Song, Album, Artist, Playlist...)
├── data/
│   ├── CacheManager.kt         # Singleton cache offline (download OkHttp, index JSON, StateFlow)
│   ├── PreferencesManager.kt   # SharedPreferences (credentials, taille max cache)
│   └── YouTubeLibraryManager.kt # Singleton favoris + playlists YouTube locaux (JSON, Gson, StateFlow)
├── player/
│   ├── PlaybackService.kt      # MediaSessionService (notification, Bluetooth)
│   ├── PlayerManager.kt        # Singleton lecture (queue, repeat, shuffle toggle, play/pause, seek, moveInQueue)
│   └── RadioManager.kt         # Singleton radio (morceaux similaires via getSimilarSongs2 ou YouTube search)
├── ui/
│   ├── login/
│   │   └── LoginScreen.kt
│   ├── library/
│   │   ├── LibraryScreen.kt         # Grille albums (pagination infinie) + chips (Artistes, Aleatoire) + logout
│   │   ├── AlbumDetailScreen.kt     # Header album pleine largeur + gradient + SongItem + PlaylistPickerDialog
│   │   ├── ArtistsScreen.kt
│   │   ├── ArtistDetailScreen.kt
│   │   ├── PlaylistsScreen.kt       # Liste playlists + creation (FAB) + suppression + import YouTube par URL
│   │   ├── PlaylistDetailScreen.kt
│   │   ├── FavoritesScreen.kt
│   │   └── DownloadsScreen.kt       # Gestion du cache offline + reglage taille max cache
│   ├── search/
│   │   └── SearchScreen.kt
│   ├── player/
│   │   ├── PlayerBar.kt             # MiniPlayer (progress bar, marquee, crossfade play/pause)
│   │   ├── FullPlayerSheet.kt       # Lecteur plein ecran en ModalBottomSheet (fond flou, animations, controles)
│   │   └── QueueScreen.kt           # File d'attente (mode normal + mode overlay transparent dans le player)
│   └── theme/
│       ├── Color.kt                  # Palette dark musicale (near-black, surfaces sombres, couleurs player)
│       ├── Theme.kt                  # NaviPKTheme(seedColor) : dynamic color anime, shapes custom, always dark
│       ├── Type.kt                   # 15 styles typographiques Material 3
│       └── DynamicColorExtractor.kt  # rememberDominantColor() : extraction couleur via Palette API + Coil
└── MainActivity.kt                   # Entry point, Bottom NavigationBar 5 onglets, FullPlayerSheet overlay
```

## Conventions

- **Langue UI** : francais (labels, boutons, messages)
- **Singletons** : `PlayerManager`, `CacheManager`, `SubsonicClient`, `YouTubeLibraryManager`, `RadioManager` sont des `object` Kotlin
- **Navigation** : Bottom NavigationBar 5 onglets (library, search, favorites, playlists, downloads) avec `saveState`/`restoreState`. Les sous-routes (album/*, artist/*, playlist/*) utilisent `navController.popBackStack()` pour revenir
- **Lecteur** : pas de route Navigation, le lecteur est un `ModalBottomSheet` overlay (`showFullPlayer` state dans `NaviPKApp`). Le mini player est affiche au-dessus de la NavigationBar
- **Theme** : toujours dark. `NaviPKTheme(seedColor)` accepte une couleur optionnelle extraite de la pochette. Toutes les couleurs du scheme sont animees avec `animateColorAsState(tween(500ms))`
- **TopAppBar** : chaque screen a son propre `TopAppBar` qui gere les status bar insets par defaut (pas de windowInsets custom)
- **Ecrans tab** (library, search, favorites, playlists, downloads) : pas de `onBack`, pas de `navigationIcon` retour
- **Ecrans detail** (album, artist, playlist) : ont un `onBack` et une fleche retour
- **SongItem** : composable reutilisable dans `AlbumDetailScreen.kt`, accepte un `modifier` optionnel (pour `animateItem()`). Parametre `initialIsFavorite` pour l'etat favori initial. Parametre `showThumbnail` pour afficher la pochette au lieu du numero de piste. Parametre `onRemove` optionnel pour retirer un morceau d'une playlist locale. YouTube-aware : favoris via `YouTubeLibraryManager`, masque les options download/cache pour les morceaux YouTube
- **PlaylistPickerDialog** : dialog de selection de playlist Navidrome, dans `AlbumDetailScreen.kt`
- **YouTubePlaylistPickerDialog** : dialog de selection/creation de playlist locale, dans `AlbumDetailScreen.kt`
- **Playlists mixtes** : une playlist locale peut contenir des morceaux Navidrome et YouTube. La resolution d'URL ne cible que les morceaux YouTube. Le telechargement ne cible que les morceaux Navidrome
- **RadioManager** : singleton dans `player/RadioManager.kt`. `startRadio(song)` genere une file de morceaux similaires (Navidrome via `getSimilarSongs2`, YouTube via search). Fallback sur `getRandomSongs` si vide
- **QueueScreen** : parametre `isOverlay` pour affichage transparent avec textes blancs quand integre dans le FullPlayerSheet

## Points d'attention

- `CacheManager.init(context)` et `YouTubeLibraryManager.init(context)` doivent etre appeles dans `MainActivity.onCreate()` avant toute utilisation
- `PlayerManager` resout les URI via `CacheManager.getPlaybackUri()` : fichier local si cache, URL distante sinon
- Le cache utilise le stockage interne (`context.filesDir/audio_cache`) - pas de permission necessaire
- Les favoris et playlists YouTube sont stockes dans `filesDir/youtube_library.json` serialise avec Gson
- L'index du cache est un fichier JSON (`audio_cache/index.json`) serialise avec Gson
- Le fond flou du FullPlayerSheet (`Modifier.blur(60.dp)`) necessite API 31+ ; fallback fond sombre uni pour API 26-30
- La couleur dominante est extraite d'une image 128px via Coil + Palette API (priorite : vibrant > lightVibrant > darkVibrant > dominant)

## API Subsonic utilisee

Endpoints dans `SubsonicApi.kt` : `ping`, `getAlbumList2` (avec pagination offset/size), `getAlbum`, `getArtists`, `getArtist`, `getPlaylists`, `getPlaylist`, `createPlaylist`, `deletePlaylist`, `updatePlaylist` (ajout de morceaux), `search3`, `getRandomSongs`, `getSimilarSongs2` (radio), `star`, `unstar`, `getStarred2`, `stream.view`, `getCoverArt.view`.
