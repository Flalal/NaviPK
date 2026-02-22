package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.flal.navipk.api.Artist
import fr.flal.navipk.api.SubsonicClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = SubsonicClient.getApi().getArtists()
            artists = response.subsonicResponse.artists?.index
                ?.flatMap { it.artist ?: emptyList() } ?: emptyList()
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artistes") },
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
