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
import fr.flal.navipk.api.isYoutube
import fr.flal.navipk.api.youtubeId
import fr.flal.navipk.api.youtube.YoutubeClient
import fr.flal.navipk.data.CacheManager
import fr.flal.navipk.data.YouTubeLibraryManager
import fr.flal.navipk.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    val isYtPlaylist = playlistId.startsWith("ytpl:")
    var playlistName by remember { mutableStateOf("Playlist") }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isResolvingUrls by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cachedIds by CacheManager.cachedSongIds.collectAsState()

    LaunchedEffect(playlistId) {
        if (isYtPlaylist) {
            val pl = YouTubeLibraryManager.getPlaylist(playlistId)
            if (pl != null) {
                playlistName = pl.name
                songs = pl.songs
            }
            isLoading = false
        } else {
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
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
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Button(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    val ytSongs = songs.filter { it.isYoutube }
                                    if (ytSongs.isNotEmpty()) {
                                        scope.launch {
                                            isResolvingUrls = true
                                            try {
                                                ytSongs.map { s ->
                                                    async(Dispatchers.IO) {
                                                        try { YoutubeClient.getStreamUrl(s.youtubeId) } catch (_: Exception) {}
                                                    }
                                                }.awaitAll()
                                                onPlaySong(songs.first(), songs)
                                            } finally {
                                                isResolvingUrls = false
                                            }
                                        }
                                    } else {
                                        onPlaySong(songs.first(), songs)
                                    }
                                }
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
                                    val ytSongs = songs.filter { it.isYoutube }
                                    if (ytSongs.isNotEmpty()) {
                                        scope.launch {
                                            isResolvingUrls = true
                                            try {
                                                ytSongs.map { s ->
                                                    async(Dispatchers.IO) {
                                                        try { YoutubeClient.getStreamUrl(s.youtubeId) } catch (_: Exception) {}
                                                    }
                                                }.awaitAll()
                                                PlayerManager.shufflePlay(songs)
                                            } finally {
                                                isResolvingUrls = false
                                            }
                                        }
                                    } else {
                                        PlayerManager.shufflePlay(songs)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lecture aléatoire")
                        }
                        run {
                            val navSongs = songs.filter { !it.isYoutube }
                            if (navSongs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val allCached = navSongs.all { it.id in cachedIds }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch { CacheManager.downloadSongs(navSongs) }
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
                            }
                        }
                        HorizontalDivider()
                    }

                    itemsIndexed(songs) { index, song ->
                        SongItem(
                            song = song,
                            trackNumber = index + 1,
                            showThumbnail = isYtPlaylist,
                            onRemove = if (isYtPlaylist) ({
                                YouTubeLibraryManager.removeSongFromPlaylist(playlistId, song.id)
                                songs = songs.filter { it.id != song.id }
                            }) else null,
                            onClick = {
                                val ytSongs = songs.filter { it.isYoutube }
                                if (ytSongs.isNotEmpty()) {
                                    scope.launch {
                                        isResolvingUrls = true
                                        try {
                                            ytSongs.map { s ->
                                                async(Dispatchers.IO) {
                                                    try { YoutubeClient.getStreamUrl(s.youtubeId) } catch (_: Exception) {}
                                                }
                                            }.awaitAll()
                                            onPlaySong(song, songs)
                                        } finally {
                                            isResolvingUrls = false
                                        }
                                    }
                                } else {
                                    onPlaySong(song, songs)
                                }
                            }
                        )
                    }
                }

                if (isResolvingUrls) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
