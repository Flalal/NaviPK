package fr.flal.navipk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.data.PreferencesManager
import fr.flal.navipk.player.PlayerManager
import fr.flal.navipk.ui.library.*
import fr.flal.navipk.ui.login.LoginScreen
import fr.flal.navipk.ui.player.PlayerBar
import fr.flal.navipk.ui.player.PlayerScreen
import fr.flal.navipk.ui.search.SearchScreen
import fr.flal.navipk.ui.theme.NaviPKTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager(this)
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

    Column(modifier = Modifier.fillMaxSize()) {
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
                        },
                        onArtistsClick = {
                            navController.navigate("artists")
                        },
                        onPlaylistsClick = {
                            navController.navigate("playlists")
                        },
                        onSearchClick = {
                            navController.navigate("search")
                        },
                        onFavoritesClick = {
                            navController.navigate("favorites")
                        },
                        onLogout = {
                            preferencesManager.clear()
                            navController.navigate("login") {
                                popUpTo("library") { inclusive = true }
                            }
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
                        }
                    )
                }

                composable("player") {
                    PlayerScreen(
                        playerState = playerState,
                        onBack = { navController.popBackStack() }
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
