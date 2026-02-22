package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.flal.navipk.api.Album
import fr.flal.navipk.api.Song
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.player.PlayerManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    var artistName by remember { mutableStateOf("Artiste") }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(artistId) {
        try {
            val response = SubsonicClient.getApi().getArtist(artistId)
            response.subsonicResponse.artist?.let {
                artistName = it.name
                albums = it.album ?: emptyList()
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val allSongs = mutableListOf<Song>()
                                for (album in albums) {
                                    val resp = SubsonicClient.getApi().getAlbum(album.id)
                                    resp.subsonicResponse.album?.song?.let { allSongs.addAll(it) }
                                }
                                PlayerManager.shufflePlay(allSongs)
                            } catch (_: Exception) {}
                        }
                    }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Lecture aléatoire")
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
