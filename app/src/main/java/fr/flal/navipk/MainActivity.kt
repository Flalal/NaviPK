package fr.flal.navipk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.api.coverArtUrl
import fr.flal.navipk.api.youtube.YoutubeClient
import fr.flal.navipk.data.CacheManager
import fr.flal.navipk.data.PreferencesManager
import fr.flal.navipk.data.UpdateManager
import fr.flal.navipk.player.PlayerManager
import fr.flal.navipk.ui.library.*
import fr.flal.navipk.ui.login.LoginScreen
import fr.flal.navipk.ui.player.FullPlayerSheet
import fr.flal.navipk.ui.player.MiniPlayer
import fr.flal.navipk.ui.search.SearchScreen
import fr.flal.navipk.ui.theme.NaviPKTheme
import fr.flal.navipk.ui.theme.rememberDominantColor
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager(this)
        CacheManager.init(this)
        PlayerManager.connect(this)
        YoutubeClient.init()

        if (preferencesManager.isLoggedIn()) {
            SubsonicClient.configure(
                preferencesManager.getServerUrl(),
                preferencesManager.getUsername(),
                preferencesManager.getPassword()
            )
        }

        lifecycleScope.launch {
            UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)
        }

        enableEdgeToEdge()
        setContent {
            NaviPKApp(preferencesManager)
        }
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val navTabs = listOf(
    NavTab("library", "Accueil", Icons.Default.Home),
    NavTab("search", "Recherche", Icons.Default.Search),
    NavTab("favorites", "Favoris", Icons.Default.Favorite),
    NavTab("playlists", "Playlists", Icons.Default.QueueMusic),
    NavTab("downloads", "Downloads", Icons.Default.Download)
)

@Composable
fun NaviPKApp(preferencesManager: PreferencesManager) {
    val navController = rememberNavController()
    val playerState by PlayerManager.state.collectAsState()
    val startDestination = if (preferencesManager.isLoggedIn()) "library" else "login"
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showFullPlayer by rememberSaveable { mutableStateOf(false) }

    // Update state
    val updateState by UpdateManager.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dynamic color from current song's cover art
    val coverArtUrl = remember(playerState.currentSong) {
        playerState.currentSong?.coverArtUrl(128)
    }
    val dominantColor = rememberDominantColor(coverArtUrl)

    NaviPKTheme(seedColor = dominantColor) {
        // Update dialog
        val release = updateState.release
        if (release != null && release.tagName != preferencesManager.getDismissedVersion()) {
            UpdateDialog(
                releaseName = release.name.ifBlank { release.tagName },
                releaseNotes = release.body,
                downloadProgress = updateState.downloadProgress,
                isDownloading = updateState.isDownloading,
                onUpdate = { scope.launch { UpdateManager.downloadAndInstall(context) } },
                onLater = { UpdateManager.dismiss() },
                onIgnore = {
                    preferencesManager.setDismissedVersion(release.tagName)
                    UpdateManager.dismiss()
                }
            )
        }
        val isLoggedIn = currentRoute != null && currentRoute != "login"

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isLoggedIn) {
                    Column {
                        if (playerState.currentSong != null) {
                            MiniPlayer(
                                playerState = playerState,
                                onExpand = { showFullPlayer = true }
                            )
                        }
                        NavigationBar {
                            val tabRoutes = navTabs.map { it.route }
                            navTabs.forEach { tab ->
                                val selected = currentRoute == tab.route ||
                                    (tab.route == "library" && currentRoute in listOf("album/{albumId}", "artists", "artist/{artistId}")) ||
                                    (tab.route == "playlists" && currentRoute in listOf("playlist/{playlistId}"))
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                            showFullPlayer = true
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
                            showFullPlayer = true
                        }
                    )
                }

                composable("search") {
                    SearchScreen(
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        },
                        onArtistClick = { artistId ->
                            navController.navigate("artist/$artistId")
                        },
                        onPlaySong = { song, queue ->
                            PlayerManager.playSong(song, queue)
                            showFullPlayer = true
                        }
                    )
                }

                composable("favorites") {
                    FavoritesScreen(
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        },
                        onArtistClick = { artistId ->
                            navController.navigate("artist/$artistId")
                        },
                        onPlaySong = { song, queue ->
                            PlayerManager.playSong(song, queue)
                            showFullPlayer = true
                        }
                    )
                }

                composable("downloads") {
                    DownloadsScreen(
                        onPlaySong = { song, queue ->
                            PlayerManager.playSong(song, queue)
                            showFullPlayer = true
                        },
                        preferencesManager = preferencesManager
                    )
                }
            }
        }

        // Full player overlay
        if (showFullPlayer) {
            FullPlayerSheet(
                playerState = playerState,
                onDismiss = { showFullPlayer = false }
            )
        }
    }
}

@Composable
private fun UpdateDialog(
    releaseName: String,
    releaseNotes: String,
    downloadProgress: Float,
    isDownloading: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onIgnore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onLater() },
        title = { Text("Mise à jour disponible") },
        text = {
            Column {
                Text("Version : $releaseName")
                if (releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(releaseNotes)
                }
                if (isDownloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                TextButton(onClick = onUpdate) { Text("Mettre à jour") }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                Column {
                    TextButton(onClick = onLater) { Text("Plus tard") }
                    TextButton(onClick = onIgnore) { Text("Ignorer cette version") }
                }
            }
        }
    )
}
