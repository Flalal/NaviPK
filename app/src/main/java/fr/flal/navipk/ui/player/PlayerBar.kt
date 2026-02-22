package fr.flal.navipk.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.flal.navipk.api.coverArtUrl
import fr.flal.navipk.player.PlayerManager
import fr.flal.navipk.player.PlayerState
import kotlinx.coroutines.delay

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onExpand: () -> Unit
) {
    val song = playerState.currentSong ?: return
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playerState.isPlaying, playerState.currentSong) {
        while (true) {
            val pos = PlayerManager.getCurrentPosition()
            val dur = PlayerManager.getDuration().coerceAtLeast(1L)
            progress = pos.toFloat() / dur.toFloat()
            delay(500)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .height(61.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.coverArtUrl(100),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = song.artist ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Crossfade(
                    targetState = playerState.isPlaying,
                    label = "playPause"
                ) { isPlaying ->
                    IconButton(onClick = { PlayerManager.togglePlayPause() }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Lecture"
                        )
                    }
                }

                IconButton(onClick = { PlayerManager.next() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Suivant")
                }
            }
        }
    }
}
