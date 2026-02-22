# NaviPK

**Android music player for Navidrome/Subsonic servers — with YouTube integration, offline caching, and auto-radio.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android Min SDK](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-green.svg)](https://developer.android.com/about/versions/oreo)
[![Latest Release](https://img.shields.io/github/v/release/Flalal/NaviPK)](https://github.com/Flalal/NaviPK/releases)

<!-- Screenshots coming soon -->

## Features

### Streaming & Playback
- Stream from any Navidrome/Subsonic server over HTTPS
- Play/pause, next, previous, seekable progress bar
- Repeat modes (off / repeat all / repeat one)
- Persistent shuffle toggle — shuffles/restores the queue without interrupting playback
- Lock screen & Bluetooth controls (car stereo, steering wheel)

### YouTube Integration
- Search and stream music from YouTube (toggle between Navidrome and YouTube)
- Automatic URL resolution via NewPipe Extractor
- Local YouTube favorites and playlists (stored as JSON)
- Import public YouTube playlists by URL (with full pagination)
- Full context menu on YouTube results (favorite, playlist, play next, enqueue, radio)

### Playlists & Radio
- Browse and play Navidrome playlists
- Mixed playlists — combine Navidrome and YouTube tracks in a single local playlist
- Create, delete, and manage playlists (Navidrome or local)
- Auto-radio from any track — similar songs via `getSimilarSongs2`, YouTube search, or random fallback

### Offline Cache
- Download tracks for offline playback (individual, full album, or full playlist)
- Automatic local playback when cached
- Configurable cache size (512 MB to unlimited) with LRU eviction
- Dedicated downloads management screen

### Library & Search
- Browse albums (infinite-scroll grid with cover art), artists, and playlists
- Full-width album headers with gradient overlays
- Global search across artists, albums, and tracks — grouped results
- Favorites screen (tracks, albums, artists, YouTube)

### Premium UI
- Dark-first theme with dynamic colors extracted from album art (Palette API)
- Animated color transitions (500 ms crossfade) on track change
- Full-screen player in a modal bottom sheet with blurred album art background
- Mini player with progress bar, marquee text, and crossfade animations
- 5-tab bottom navigation (Home, Search, Favorites, Playlists, Downloads)

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Audio | Media3 / ExoPlayer |
| HTTP | Retrofit + OkHttp |
| Images | Coil |
| Navigation | Navigation Compose |
| Dynamic Colors | AndroidX Palette |
| YouTube | NewPipe Extractor |
| Architecture | MVVM (ViewModel + StateFlow) |
| Local Storage | SharedPreferences + internal file cache |

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- Android SDK 26+ (Android 8.0 minimum)
- A running [Navidrome](https://www.navidrome.org/) server (or any Subsonic-compatible server)

### Build & Run

```bash
git clone https://github.com/Flalal/NaviPK.git
cd NaviPK
```

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Connect an Android device via USB (developer mode + USB debugging enabled)
4. Click **Run**

### Configuration

On first launch, the login screen asks for:

- **Server URL** — your Navidrome address (e.g. `https://music.example.com`)
- **Username** — your Navidrome login
- **Password** — your Navidrome password

Credentials are stored locally. Authentication uses the Subsonic API token method (MD5 salt + hash).

## Project Structure

```
fr.flal.navipk/
├── api/
│   ├── SubsonicApi.kt            # Retrofit interface (Subsonic endpoints)
│   ├── SubsonicClient.kt         # HTTP singleton (auth, stream/coverArt URLs)
│   ├── SubsonicModels.kt         # Data classes (Song, Album, Artist, Playlist…)
│   └── youtube/
│       ├── YoutubeClient.kt      # YouTube search & URL resolution
│       └── YoutubeDownloader.kt  # YouTube audio stream extraction
├── data/
│   ├── CacheManager.kt           # Offline cache (download, JSON index, LRU eviction)
│   ├── PreferencesManager.kt     # SharedPreferences (credentials, cache settings)
│   └── YouTubeLibraryManager.kt  # Local YouTube favorites & playlists (JSON + StateFlow)
├── player/
│   ├── PlaybackService.kt        # MediaSessionService (notification, Bluetooth)
│   ├── PlayerManager.kt          # Playback control (queue, repeat, shuffle, seek)
│   └── RadioManager.kt           # Auto-radio generation (similar songs / random fallback)
├── ui/
│   ├── login/
│   │   └── LoginScreen.kt
│   ├── library/
│   │   ├── LibraryScreen.kt           # Album grid + chips (Artists, Shuffle) + logout
│   │   ├── AlbumDetailScreen.kt       # Album header + songs + PlaylistPickerDialog
│   │   ├── ArtistsScreen.kt           # Artist list
│   │   ├── ArtistDetailScreen.kt      # Artist detail + albums
│   │   ├── PlaylistsScreen.kt         # Playlist list + create + delete + YouTube import
│   │   ├── PlaylistDetailScreen.kt    # Playlist tracks
│   │   ├── FavoritesScreen.kt         # Favorites screen
│   │   └── DownloadsScreen.kt         # Cache management + size settings
│   ├── search/
│   │   └── SearchScreen.kt            # Global search
│   ├── player/
│   │   ├── PlayerBar.kt               # Mini player (progress, marquee, crossfade)
│   │   ├── FullPlayerSheet.kt         # Full-screen player (bottom sheet, blur, controls)
│   │   └── QueueScreen.kt             # Queue view (standalone + overlay mode)
│   └── theme/
│       ├── Color.kt                    # Dark music palette
│       ├── Theme.kt                    # Dynamic animated theme + custom shapes
│       ├── Type.kt                     # Material 3 typography (15 styles)
│       └── DynamicColorExtractor.kt    # Dominant color extraction (Palette + Coil)
└── MainActivity.kt                     # Entry point, bottom nav, full player overlay
```

## Subsonic API Reference

NaviPK targets Subsonic API v1.16.1:

| Endpoint | Purpose |
|----------|---------|
| `ping.view` | Connection test |
| `getAlbumList2.view` | Album list (paginated) |
| `getAlbum.view` | Album detail + tracks |
| `getArtists.view` | Artist list |
| `getArtist.view` | Artist detail + albums |
| `getPlaylists.view` | Playlist list |
| `getPlaylist.view` | Playlist detail + tracks |
| `createPlaylist.view` | Create a playlist |
| `deletePlaylist.view` | Delete a playlist |
| `updatePlaylist.view` | Add tracks to a playlist |
| `search3.view` | Global search |
| `getRandomSongs.view` | Random tracks |
| `getSimilarSongs2.view` | Similar tracks (radio) |
| `star.view` / `unstar.view` | Add/remove favorites |
| `getStarred2.view` | Fetch favorites |
| `stream.view` | Audio streaming |
| `getCoverArt.view` | Album cover art |

## Contributing

Contributions are welcome! Please read the [Contributing Guide](CONTRIBUTING.md) before submitting a pull request.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Navidrome](https://www.navidrome.org/) — open-source music server
- [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) — YouTube stream extraction
- [Media3](https://developer.android.com/media/media3) — Android media playback library
