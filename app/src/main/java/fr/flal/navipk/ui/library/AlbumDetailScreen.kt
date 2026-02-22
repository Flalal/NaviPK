package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
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
import fr.flal.navipk.api.Song
import fr.flal.navipk.api.SubsonicClient
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
fun SongItem(song: Song, trackNumber: Int, onClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            Text(
                text = "$trackNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
}

fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}
