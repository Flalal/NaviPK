# AGENT.md — NaviPK

## Project

NaviPK is an Android music player for Navidrome/Subsonic servers, optimized for car use.

## Architecture

- **Package**: `fr.flal.navipk`
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Audio**: Media3 / ExoPlayer
- **API**: Retrofit + OkHttp + Gson
- **Navigation**: Navigation Compose (string routes in `MainActivity.kt`) + 5-tab Bottom NavigationBar
- **State**: `StateFlow` in singletons (`PlayerManager`, `CacheManager`, `YouTubeLibraryManager`, `RadioManager`)
- **Theme**: Dark-first with dynamic colors extracted from album art via Palette API

## File Structure

```
fr.flal.navipk/
├── api/
│   ├── SubsonicApi.kt            # Retrofit interface (Subsonic endpoints)
│   ├── SubsonicClient.kt         # HTTP singleton (MD5 token auth, stream/coverArt URLs)
│   ├── SubsonicModels.kt         # Data classes (Song, Album, Artist, Playlist…)
│   └── youtube/
│       ├── YoutubeClient.kt      # YouTube search & URL resolution
│       └── YoutubeDownloader.kt  # YouTube audio stream extraction
├── data/
│   ├── CacheManager.kt           # Offline cache singleton (OkHttp download, JSON index, StateFlow)
│   ├── PreferencesManager.kt     # SharedPreferences (credentials, max cache size)
│   └── YouTubeLibraryManager.kt  # YouTube favorites + local playlists singleton (JSON, Gson, StateFlow)
├── player/
│   ├── PlaybackService.kt        # MediaSessionService (notification, Bluetooth)
│   ├── PlayerManager.kt          # Playback singleton (queue, repeat, shuffle toggle, play/pause, seek, moveInQueue)
│   └── RadioManager.kt           # Radio singleton (similar songs via getSimilarSongs2 or YouTube search)
├── ui/
│   ├── login/
│   │   └── LoginScreen.kt
│   ├── library/
│   │   ├── LibraryScreen.kt           # Album grid (infinite pagination) + chips (Artists, Shuffle) + logout
│   │   ├── AlbumDetailScreen.kt       # Full-width album header + gradient + SongItem + PlaylistPickerDialog
│   │   ├── ArtistsScreen.kt
│   │   ├── ArtistDetailScreen.kt
│   │   ├── PlaylistsScreen.kt         # Playlist list + create (FAB) + delete + YouTube import by URL
│   │   ├── PlaylistDetailScreen.kt
│   │   ├── FavoritesScreen.kt
│   │   └── DownloadsScreen.kt         # Offline cache management + max cache size setting
│   ├── search/
│   │   └── SearchScreen.kt
│   ├── player/
│   │   ├── PlayerBar.kt               # MiniPlayer (progress bar, marquee, crossfade play/pause)
│   │   ├── FullPlayerSheet.kt         # Full-screen player in ModalBottomSheet (blurred background, animations, controls)
│   │   └── QueueScreen.kt             # Queue view (normal mode + transparent overlay in player)
│   └── theme/
│       ├── Color.kt                    # Dark music palette (near-black, dark surfaces, player colors)
│       ├── Theme.kt                    # NaviPKTheme(seedColor): dynamic animated color, custom shapes, always dark
│       ├── Type.kt                     # 15 Material 3 typographic styles
│       └── DynamicColorExtractor.kt    # rememberDominantColor(): color extraction via Palette API + Coil
└── MainActivity.kt                     # Entry point, 5-tab Bottom NavigationBar, FullPlayerSheet overlay
```

## Conventions

- **UI language**: French (labels, buttons, messages)
- **Singletons**: `PlayerManager`, `CacheManager`, `SubsonicClient`, `YouTubeLibraryManager`, `RadioManager` are Kotlin `object`s
- **Navigation**: 5-tab Bottom NavigationBar (library, search, favorites, playlists, downloads) with `saveState`/`restoreState`. Sub-routes (album/*, artist/*, playlist/*) use `navController.popBackStack()` to go back
- **Player**: not a Navigation route — the player is a `ModalBottomSheet` overlay (`showFullPlayer` state in `NaviPKApp`). The mini player is displayed above the NavigationBar
- **Theme**: always dark. `NaviPKTheme(seedColor)` accepts an optional color extracted from album art. All color scheme values are animated with `animateColorAsState(tween(500ms))`
- **TopAppBar**: each screen has its own `TopAppBar` handling status bar insets by default (no custom windowInsets)
- **Tab screens** (library, search, favorites, playlists, downloads): no `onBack`, no back arrow `navigationIcon`
- **Detail screens** (album, artist, playlist): have `onBack` and a back arrow
- **SongItem**: reusable composable in `AlbumDetailScreen.kt`, accepts optional `modifier` (for `animateItem()`). `initialIsFavorite` for initial favorite state. `showThumbnail` to show cover art instead of track number. Optional `onRemove` to remove a track from a local playlist. YouTube-aware: favorites via `YouTubeLibraryManager`, hides download/cache options for YouTube tracks
- **PlaylistPickerDialog**: Navidrome playlist picker dialog, in `AlbumDetailScreen.kt`
- **YouTubePlaylistPickerDialog**: local playlist picker/creation dialog, in `AlbumDetailScreen.kt`
- **Mixed playlists**: a local playlist can contain both Navidrome and YouTube tracks. URL resolution targets only YouTube tracks. Download targets only Navidrome tracks
- **RadioManager**: singleton in `player/RadioManager.kt`. `startRadio(song)` generates a queue of similar tracks (Navidrome via `getSimilarSongs2`, YouTube via search). Falls back to `getRandomSongs` if empty
- **QueueScreen**: `isOverlay` parameter for transparent display with white text when embedded in FullPlayerSheet

## Important Notes

- `CacheManager.init(context)` and `YouTubeLibraryManager.init(context)` must be called in `MainActivity.onCreate()` before any usage
- `PlayerManager` resolves URIs via `CacheManager.getPlaybackUri()`: local file if cached, remote URL otherwise
- Cache uses internal storage (`context.filesDir/audio_cache`) — no permissions required
- YouTube favorites and playlists are stored in `filesDir/youtube_library.json`, serialized with Gson
- Cache index is a JSON file (`audio_cache/index.json`), serialized with Gson
- FullPlayerSheet blurred background (`Modifier.blur(60.dp)`) requires API 31+; falls back to solid dark background for API 26–30
- Dominant color is extracted from a 128px image via Coil + Palette API (priority: vibrant > lightVibrant > darkVibrant > dominant)

## Subsonic API Usage

Endpoints in `SubsonicApi.kt`: `ping`, `getAlbumList2` (with offset/size pagination), `getAlbum`, `getArtists`, `getArtist`, `getPlaylists`, `getPlaylist`, `createPlaylist`, `deletePlaylist`, `updatePlaylist` (add tracks), `search3`, `getRandomSongs`, `getSimilarSongs2` (radio), `star`, `unstar`, `getStarred2`, `stream.view`, `getCoverArt.view`.
