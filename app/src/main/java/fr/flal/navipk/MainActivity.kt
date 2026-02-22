package fr.flal.navipk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.data.CacheManager
import fr.flal.navipk.data.PreferencesManager
import fr.flal.navipk.player.PlayerManager
import fr.flal.navipk.ui.library.*
import fr.flal.navipk.ui.login.LoginScreen
import fr.flal.navipk.ui.player.PlayerBar
import fr.flal.navipk.ui.player.PlayerScreen
import fr.flal.navipk.ui.player.QueueScreen
import fr.flal.navipk.ui.search.SearchScreen
import fr.flal.navipk.ui.theme.NaviPKTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager(this)
        CacheManager.init(this)
        PlayerManager.connect(this)

        // Restore session if already logged in
        if (preferencesManager.isLoggedIn()) {
            SubsonicClient.configure(
                preferencesManager.getServerUrl(),
                preferencesManager.getUsername(),
                preferencesManager.getPassword()
            )
        }

        enableEdgeToEdge()
        setContent {
            NaviPKTheme {
                NaviPKApp(preferencesManager)
            }
        }
    }
}

@Composable
fun NaviPKApp(preferencesManager: PreferencesManager) {
    val navController = rememberNavController()
    val playerState by PlayerManager.state.collectAsState()
    val startDestination = if (preferencesManager.isLoggedIn()) "library" else "login"
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(modifier = Modifier.fillMaxSize()) {
            // Global navigation bar (hidden on login)
            if (currentRoute != null && currentRoute != "login") {
                NavBar(
                    currentRoute = currentRoute,
                    navController = navController,
                    preferencesManager = preferencesManager
                )
            }

            // Main navigation area
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.weight(1f)
            ) {
                composable("login") {
                    LoginScreen(
                        initialServerUrl = preferencesManager.getServerUrl().ifBlank { "https://" },
                        initialUsername = preferencesManager.getUsername(),
                        onLoginSuccess = { serverUrl, username, password ->
                            preferencesManager.save(serverUrl, username, password)
                            navController.navigate("library") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("library") {
                    LibraryScreen(
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        }
                    )
                }

                composable(
                    "album/{albumId}",
                    arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
                    AlbumDetailScreen(
                        albumId = albumId,
                        onBack = { navController.popBackStack() },
                        onPlaySong = { song, queue ->
                            PlayerManager.playSong(song, queue)
                            navController.navigate("player")
                        }
                    )
                }

                composable("artists") {
                    ArtistsScreen(
                        onBack = { navController.popBackStack() },
                        onArtistClick = { artistId ->
                            navController.navigate("artist/$artistId")
                        }
                    )
                }

                composable(
                    "artist/{artistId}",
                    arguments = listOf(navArgument("artistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
                    ArtistDetailScreen(
                        artistId = artistId,
                        onBack = { navController.popBackStack() },
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        }
                    )
                }

                composable("playlists") {
                    PlaylistsScreen(
                        onBack = { navController.popBackStack() },
                        onPlaylistClick = { playlistId ->
                            navController.navigate("playlist/$playlistId")
                        }
                    )
                }

                composable(
                    "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        onBack = { navController.popBackStack() },
                        onPlaySong = { song, queue ->
                            PlayerManager.playSong(song, queue)
                            navController.navigate("player")
                        }
                    )
                }

                composable("search") {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        },
                        onArtistClick = { artistId ->
                            navController.navigate("artist/$artistId")
                        },
                        onPlaySong = { song, queue ->
                            PlayerManager.playSong(song, queue)
                            navController.navigate("player")
                        }
                    )
                }

                composable("favorites") {
                    FavoritesScreen(
                        onBack = { navController.popBackStack() },
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        },
                        onArtistClick = { artistId ->
                            navController.navigate("artist/$artistId")
                        },
                        onPlaySong = { song, queue ->
                            PlayerManager.playSong(song, queue)
                            navController.navigate("player")
                        }
                    )
                }

                composable("downloads") {
                    DownloadsScreen(
                        onBack = { navController.popBackStack() },
                        onPlaySong = { song, queue ->
                            PlayerManager.playSong(song, queue)
                            navController.navigate("player")
                        },
                        preferencesManager = preferencesManager
                    )
                }

                composable("queue") {
                    QueueScreen(
                        playerState = playerState,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("player") {
                    PlayerScreen(
                        playerState = playerState,
                        onBack = { navController.popBackStack() },
                        onQueueClick = { navController.navigate("queue") }
                    )
                }
            }

            // Player bar at the bottom (visible when a song is playing)
            if (playerState.currentSong != null) {
                PlayerBar(
                    playerState = playerState,
                    onClick = { navController.navigate("player") }
                )
            }
    }
}

@Composable
fun NavBar(
    currentRoute: String,
    navController: NavController,
    preferencesManager: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Default.Home,
                    label = "Accueil",
                    isActive = currentRoute == "library",
                    onClick = {
                        navController.navigate("library") {
                            popUpTo("library") { inclusive = true }
                        }
                    }
                )
                NavBarItem(
                    icon = Icons.Default.Search,
                    label = "Recherche",
                    isActive = currentRoute == "search",
                    onClick = { navController.navigate("search") }
                )
                NavBarItem(
                    icon = Icons.Default.Shuffle,
                    label = "Aléatoire",
                    isActive = false,
                    onClick = {
                        scope.launch {
                            try {
                                val response = SubsonicClient.getApi().getRandomSongs(50)
                                val songs = response.subsonicResponse.randomSongs?.song ?: emptyList()
                                PlayerManager.shufflePlay(songs)
                            } catch (_: Exception) {}
                        }
                    }
                )
                NavBarItem(
                    icon = Icons.Default.Favorite,
                    label = "Favoris",
                    isActive = currentRoute == "favorites",
                    onClick = { navController.navigate("favorites") }
                )
                NavBarItem(
                    icon = Icons.Default.Person,
                    label = "Artistes",
                    isActive = currentRoute == "artists" || currentRoute.startsWith("artist/"),
                    onClick = { navController.navigate("artists") }
                )
                NavBarItem(
                    icon = Icons.Default.QueueMusic,
                    label = "Playlists",
                    isActive = currentRoute == "playlists" || currentRoute.startsWith("playlist/"),
                    onClick = { navController.navigate("playlists") }
                )
                NavBarItem(
                    icon = Icons.Default.Download,
                    label = "Downloads",
                    isActive = currentRoute == "downloads",
                    onClick = { navController.navigate("downloads") }
                )
                NavBarItem(
                    icon = Icons.Default.Logout,
                    label = "Déco",
                    isActive = false,
                    onClick = {
                        preferencesManager.clear()
                        navController.navigate("login") {
                            popUpTo("library") { inclusive = true }
                        }
                    }
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isActive) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.onSurfaceVariant

    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = tint)
    }
}
