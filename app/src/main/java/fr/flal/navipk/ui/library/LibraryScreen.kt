package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
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
import fr.flal.navipk.api.Album
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.player.PlayerManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAlbumClick: (String) -> Unit,
    onArtistsClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val pageSize = 50

    LaunchedEffect(Unit) {
        try {
            val response = SubsonicClient.getApi().getAlbumList2(size = pageSize, offset = 0)
            val loaded = response.subsonicResponse.albumList2?.album ?: emptyList()
            albums = loaded
            hasMore = loaded.size >= pageSize
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && !isLoadingMore && lastVisibleIndex >= albums.size - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && albums.isNotEmpty()) {
            isLoadingMore = true
            try {
                val response = SubsonicClient.getApi().getAlbumList2(size = pageSize, offset = albums.size)
                val loaded = response.subsonicResponse.albumList2?.album ?: emptyList()
                albums = albums + loaded
                hasMore = loaded.size >= pageSize
            } catch (_: Exception) {}
            isLoadingMore = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NaviPK") },
                actions = {
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
                                    val response = SubsonicClient.getApi().getAlbumList2(size = pageSize, offset = 0)
                                    val loaded = response.subsonicResponse.albumList2?.album ?: emptyList()
                                    albums = loaded
                                    hasMore = loaded.size >= pageSize
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
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    // Header chips
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = onArtistsClick,
                                label = { Text("Artistes") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            AssistChip(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val response = SubsonicClient.getApi().getRandomSongs(50)
                                            val songs = response.subsonicResponse.randomSongs?.song ?: emptyList()
                                            PlayerManager.shufflePlay(songs)
                                        } catch (_: Exception) {}
                                    }
                                },
                                label = { Text("Aléatoire") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }

                    items(albums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
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
