package fr.flal.navipk.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.flal.navipk.api.Song
import fr.flal.navipk.data.CacheManager
import fr.flal.navipk.data.PreferencesManager
import fr.flal.navipk.player.PlayerManager

private data class CacheSizeOption(val label: String, val sizeMb: Int)

private val cacheSizeOptions = listOf(
    CacheSizeOption("512 Mo", 512),
    CacheSizeOption("1 Go", 1024),
    CacheSizeOption("2 Go", 2048),
    CacheSizeOption("5 Go", 5120),
    CacheSizeOption("10 Go", 10240),
    CacheSizeOption("Illimité", 0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    preferencesManager: PreferencesManager
) {
    val cachedIds by CacheManager.cachedSongIds.collectAsState()
    val entries = remember(cachedIds) { CacheManager.getCachedSongs() }
    val songs = remember(entries) { entries.map { CacheManager.toSong(it) } }
    val cacheSize = remember(cachedIds) { CacheManager.getCacheSizeBytes() }
    var currentMaxMb by remember { mutableIntStateOf(preferencesManager.getMaxCacheSizeMb()) }

    val limitText = if (currentMaxMb == 0) "Illimité"
                    else cacheSizeOptions.find { it.sizeMb == currentMaxMb }?.label ?: "${currentMaxMb} Mo"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Téléchargements") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Text(
                    text = "Cache : ${formatSize(cacheSize)} — ${songs.size} morceaux — Limite : $limitText",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (songs.isNotEmpty()) {
                    Button(
                        onClick = { onPlaySong(songs.first(), songs) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tout lire")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { PlayerManager.shufflePlay(songs) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lecture aléatoire")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = { CacheManager.clearCache() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vider le cache")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Taille max du cache",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                cacheSizeOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentMaxMb = option.sizeMb
                                preferencesManager.setMaxCacheSizeMb(option.sizeMb)
                                if (option.sizeMb > 0) {
                                    CacheManager.evictToLimit(option.sizeMb.toLong() * 1024 * 1024)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = currentMaxMb == option.sizeMb,
                            onClick = {
                                currentMaxMb = option.sizeMb
                                preferencesManager.setMaxCacheSizeMb(option.sizeMb)
                                if (option.sizeMb > 0) {
                                    CacheManager.evictToLimit(option.sizeMb.toLong() * 1024 * 1024)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
            }

            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun morceau téléchargé", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            itemsIndexed(songs) { index, song ->
                ListItem(
                    headlineContent = {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        Text(song.artist ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingContent = {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { CacheManager.removeSong(song.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.clickable { onPlaySong(song, songs) }
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes o"
        bytes < 1024 * 1024 -> "%.1f Ko".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f Mo".format(bytes / (1024.0 * 1024))
        else -> "%.2f Go".format(bytes / (1024.0 * 1024 * 1024))
    }
}
