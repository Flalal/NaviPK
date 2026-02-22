package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.flal.navipk.api.Song
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.data.CacheManager
import fr.flal.navipk.player.PlayerManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    var playlistName by remember { mutableStateOf("Playlist") }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val cachedIds by CacheManager.cachedSongIds.collectAsState()

    LaunchedEffect(playlistId) {
        try {
            val response = SubsonicClient.getApi().getPlaylist(playlistId)
            response.subsonicResponse.playlist?.let {
                playlistName = it.name
                songs = it.entry ?: emptyList()
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item {
                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) onPlaySong(songs.first(), songs)
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tout lire")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                PlayerManager.shufflePlay(songs)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lecture aléatoire")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val allCached = songs.isNotEmpty() && songs.all { it.id in cachedIds }
                    OutlinedButton(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                scope.launch { CacheManager.downloadSongs(songs) }
                            }
                        },
                        enabled = !allCached,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            if (allCached) Icons.Default.CloudDone else Icons.Default.Download,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (allCached) "Téléchargé" else "Télécharger")
                    }
                    HorizontalDivider()
                }

                itemsIndexed(songs) { index, song ->
                    SongItem(
                        song = song,
                        trackNumber = index + 1,
                        onClick = { onPlaySong(song, songs) }
                    )
                }
            }
        }
    }
}
