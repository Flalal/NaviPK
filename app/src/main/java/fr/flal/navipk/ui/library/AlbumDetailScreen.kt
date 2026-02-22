package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.flal.navipk.api.AlbumWithSongs
import fr.flal.navipk.api.Playlist
import fr.flal.navipk.api.Song
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.data.CacheManager
import fr.flal.navipk.data.DownloadState
import fr.flal.navipk.player.PlayerManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    var album by remember { mutableStateOf<AlbumWithSongs?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val cachedIds by CacheManager.cachedSongIds.collectAsState()

    LaunchedEffect(albumId) {
        try {
            val response = SubsonicClient.getApi().getAlbum(albumId)
            album = response.subsonicResponse.album
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(album?.name ?: "Album") },
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
            album?.let { alb ->
                val songs = alb.song ?: emptyList()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    item {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = alb.coverArt?.let { SubsonicClient.getCoverArtUrl(it, 500) },
                                contentDescription = alb.name,
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(alb.name, style = MaterialTheme.typography.headlineSmall)
                                alb.artist?.let {
                                    Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${songs.size} morceaux", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Play all button
                        Button(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    onPlaySong(songs.first(), songs)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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
                        Spacer(modifier = Modifier.height(8.dp))
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
}

@Composable
fun SongItem(song: Song, trackNumber: Int, onClick: () -> Unit, initialIsFavorite: Boolean = false) {
    var showMenu by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(initialIsFavorite) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cachedIds by CacheManager.cachedSongIds.collectAsState()
    val downloadStates by CacheManager.downloadStates.collectAsState()
    val isCached = song.id in cachedIds
    val dlState = downloadStates[song.id]

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = song.artist ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$trackNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isCached) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Hors-ligne",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                song.duration?.let {
                    Text(
                        text = formatDuration(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Plus",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Lire ensuite") },
                            onClick = {
                                PlayerManager.playNext(song)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ajouter à la file") },
                            onClick = {
                                PlayerManager.addToQueue(song)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ajouter à une playlist") },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                showPlaylistDialog = true
                            }
                        )
                        if (isCached) {
                            DropdownMenuItem(
                                text = { Text("Supprimer du cache") },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                },
                                onClick = {
                                    CacheManager.removeSong(song.id)
                                    showMenu = false
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (dlState) {
                                            DownloadState.DOWNLOADING -> "Téléchargement..."
                                            DownloadState.ERROR -> "Réessayer le téléchargement"
                                            else -> "Télécharger"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    if (dlState == DownloadState.DOWNLOADING) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                    }
                                },
                                enabled = dlState != DownloadState.DOWNLOADING,
                                onClick = {
                                    scope.launch { CacheManager.downloadSong(song) }
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris")
                            },
                            leadingIcon = {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                scope.launch {
                                    try {
                                        if (isFavorite) {
                                            SubsonicClient.getApi().unstar(song.id)
                                        } else {
                                            SubsonicClient.getApi().star(song.id)
                                        }
                                        isFavorite = !isFavorite
                                    } catch (_: Exception) {}
                                }
                                showMenu = false
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )

    if (showPlaylistDialog) {
        PlaylistPickerDialog(
            songId = song.id,
            onDismiss = { showPlaylistDialog = false }
        )
    }
}

@Composable
fun PlaylistPickerDialog(songId: String, onDismiss: () -> Unit) {
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = SubsonicClient.getApi().getPlaylists()
            playlists = response.subsonicResponse.playlists?.playlist ?: emptyList()
        } catch (_: Exception) {}
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter à une playlist") },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (playlists.isEmpty()) {
                Text("Aucune playlist")
            } else {
                LazyColumn {
                    itemsIndexed(playlists) { _, playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            supportingContent = {
                                Text("${playlist.songCount ?: 0} morceaux")
                            },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    try {
                                        SubsonicClient.getApi().updatePlaylist(playlist.id, songId)
                                    } catch (_: Exception) {}
                                }
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}
