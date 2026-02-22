package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.flal.navipk.api.Album
import fr.flal.navipk.api.Song
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.player.PlayerManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAlbumClick: (String) -> Unit,
    onArtistsClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onLogout: () -> Unit
) {
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = SubsonicClient.getApi().getAlbumList2()
            albums = response.subsonicResponse.albumList2?.album ?: emptyList()
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NaviPK") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Recherche")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val response = SubsonicClient.getApi().getRandomSongs(50)
                                val songs = response.subsonicResponse.randomSongs?.song ?: emptyList()
                                PlayerManager.shufflePlay(songs)
                            } catch (_: Exception) {}
                        }
                    }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Lecture aléatoire")
                    }
                    IconButton(onClick = onFavoritesClick) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favoris")
                    }
                    IconButton(onClick = onArtistsClick) {
                        Icon(Icons.Default.Person, contentDescription = "Artistes")
                    }
                    IconButton(onClick = onPlaylistsClick) {
                        Icon(Icons.Default.QueueMusic, contentDescription = "Playlists")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Déconnexion")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Erreur : $errorMessage")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val response = SubsonicClient.getApi().getAlbumList2()
                                    albums = response.subsonicResponse.albumList2?.album ?: emptyList()
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage
                                } finally {
                                    isLoading = false
                                }
                            }
                        }) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    items(albums) { album ->
                        AlbumCard(album = album, onClick = { onAlbumClick(album.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            AsyncImage(
                model = album.coverArt?.let { SubsonicClient.getCoverArtUrl(it) },
                contentDescription = album.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                album.artist?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
