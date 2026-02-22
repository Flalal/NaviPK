package fr.flal.navipk.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.flal.navipk.api.*
import fr.flal.navipk.player.PlayerManager
import fr.flal.navipk.ui.library.formatDuration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun performSearch() {
        if (query.isBlank()) return
        isLoading = true
        hasSearched = true
        keyboardController?.hide()
        scope.launch {
            try {
                val response = SubsonicClient.getApi().search3(query)
                val result = response.subsonicResponse.searchResult3
                artists = result?.artist ?: emptyList()
                albums = result?.album ?: emptyList()
                songs = result?.song ?: emptyList()
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Rechercher...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; artists = emptyList(); albums = emptyList(); songs = emptyList(); hasSearched = false }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { performSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Rechercher")
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
            !hasSearched -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Recherchez des artistes, albums ou morceaux",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            artists.isEmpty() && albums.isEmpty() && songs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aucun résultat pour \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    if (artists.isNotEmpty()) {
                        item {
                            Text(
                                "Artistes",
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

                    if (albums.isNotEmpty()) {
                        item {
                            Text(
                                "Albums",
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

                    if (songs.isNotEmpty()) {
                        item {
                            Text(
                                "Morceaux",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                            )
                        }
                        items(songs) { song ->
                            ListItem(
                                headlineContent = {
                                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(
                                        "${song.artist ?: ""} - ${song.album ?: ""}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    AsyncImage(
                                        model = song.coverArt?.let { SubsonicClient.getCoverArtUrl(it, 100) },
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                trailingContent = {
                                    song.duration?.let {
                                        Text(
                                            formatDuration(it),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    PlayerManager.playSong(song, songs)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
