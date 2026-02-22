package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.flal.navipk.api.Playlist
import fr.flal.navipk.api.SubsonicClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onBack: () -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            val response = SubsonicClient.getApi().getPlaylists()
            playlists = response.subsonicResponse.playlists?.playlist ?: emptyList()
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    // Create playlist dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newPlaylistName = "" },
            title = { Text("Nouvelle playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newPlaylistName.trim()
                        if (name.isNotEmpty()) {
                            showCreateDialog = false
                            newPlaylistName = ""
                            scope.launch {
                                try {
                                    val response = SubsonicClient.getApi().createPlaylist(name)
                                    if (response.subsonicResponse.status == "ok") {
                                        snackbarHostState.showSnackbar("Playlist \"$name\" créée")
                                        refreshTrigger++
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            "Erreur : ${response.subsonicResponse.error?.message ?: "inconnue"}"
                                        )
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Erreur : ${e.localizedMessage}")
                                }
                            }
                        }
                    }
                ) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newPlaylistName = "" }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Delete confirmation dialog
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Supprimer la playlist") },
            text = { Text("Supprimer \"${playlist.name}\" ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlistToDelete = null
                        scope.launch {
                            try {
                                val response = SubsonicClient.getApi().deletePlaylist(playlist.id)
                                if (response.subsonicResponse.status == "ok") {
                                    snackbarHostState.showSnackbar("Playlist supprimée")
                                    refreshTrigger++
                                } else {
                                    snackbarHostState.showSnackbar(
                                        "Erreur : ${response.subsonicResponse.error?.message ?: "inconnue"}"
                                    )
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Erreur : ${e.localizedMessage}")
                            }
                        }
                    }
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Créer une playlist")
            }
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
                items(playlists) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = {
                            Text("${playlist.songCount ?: 0} morceaux")
                        },
                        trailingContent = {
                            IconButton(onClick = { playlistToDelete = playlist }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.clickable { onPlaylistClick(playlist.id) }
                    )
                }
            }
        }
    }
}
