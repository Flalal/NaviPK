package fr.flal.navipk.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.flal.navipk.player.PlayerManager
import fr.flal.navipk.player.PlayerState
import fr.flal.navipk.ui.theme.OnPlayerPrimary
import fr.flal.navipk.ui.theme.OnPlayerSecondary
import fr.flal.navipk.ui.theme.OnPlayerTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    playerState: PlayerState,
    onBack: () -> Unit,
    isOverlay: Boolean = false
) {
    val queue = playerState.queue
    val currentIndex = playerState.currentIndex

    val containerColor = if (isOverlay) Color.Transparent else MaterialTheme.colorScheme.surface
    val contentColor = if (isOverlay) OnPlayerPrimary else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (isOverlay) OnPlayerSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = if (isOverlay) OnPlayerPrimary else MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = containerColor,
        contentColor = contentColor,
        topBar = {
            TopAppBar(
                title = { Text("File d'attente (${queue.size})", color = contentColor) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = contentColor
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "File d'attente vide",
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryColor
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                itemsIndexed(queue) { index, song ->
                    val isCurrent = index == currentIndex
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = if (isCurrent) primaryColor else contentColor,
                            supportingColor = secondaryColor,
                            leadingIconColor = if (isCurrent) primaryColor else secondaryColor,
                            trailingIconColor = secondaryColor
                        ),
                        headlineContent = {
                            Text(
                                text = song.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
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
                            if (isCurrent) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = "En cours"
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = secondaryColor
                                )
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { PlayerManager.moveInQueue(index, index - 1) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Monter",
                                            modifier = Modifier.size(28.dp),
                                            tint = secondaryColor
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                                if (index < queue.size - 1) {
                                    IconButton(
                                        onClick = { PlayerManager.moveInQueue(index, index + 1) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Descendre",
                                            modifier = Modifier.size(28.dp),
                                            tint = secondaryColor
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                                if (!isCurrent) {
                                    IconButton(
                                        onClick = { PlayerManager.removeFromQueue(index) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Retirer",
                                            modifier = Modifier.size(20.dp),
                                            tint = secondaryColor
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        },
                        modifier = Modifier.clickable { PlayerManager.playAtIndex(index) }
                    )
                }
            }
        }
    }
}
