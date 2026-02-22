package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import fr.flal.navipk.api.*
import fr.flal.navipk.player.PlayerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit
) {
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = SubsonicClient.getApi().getStarred2()
            val starred = response.subsonicResponse.starred2
            artists = starred?.artist ?: emptyList()
            albums = starred?.album ?: emptyList()
            songs = starred?.song ?: emptyList()
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favoris") },
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
        } else if (artists.isEmpty() && albums.isEmpty() && songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucun favori",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                if (songs.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    PlayerManager.playSong(songs.first(), songs)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tout lire")
                            }
                            OutlinedButton(
                                onClick = {
                                    PlayerManager.shufflePlay(songs)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aléatoire")
                            }
                        }
                    }

                    item {
                        Text(
                            "Morceaux favoris",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
                        )
                    }
                    itemsIndexed(songs) { index, song ->
                        SongItem(
                            song = song,
                            trackNumber = index + 1,
                            onClick = { PlayerManager.playSong(song, songs) }
                        )
                    }
                }

                if (albums.isNotEmpty()) {
                    item {
                        Text(
                            "Albums favoris",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                        )
                    }
                    items(albums) { album ->
                        ListItem(
                            headlineContent = {
                                Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                album.artist?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = album.coverArt?.let { SubsonicClient.getCoverArtUrl(it, 100) },
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            modifier = Modifier.clickable { onAlbumClick(album.id) }
                        )
                    }
                }

                if (artists.isNotEmpty()) {
                    item {
                        Text(
                            "Artistes favoris",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                        )
                    }
                    items(artists) { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name) },
                            supportingContent = {
                                artist.albumCount?.let { Text("$it albums") }
                            },
                            modifier = Modifier.clickable { onArtistClick(artist.id) }
                        )
                    }
                }
            }
        }
    }
}
