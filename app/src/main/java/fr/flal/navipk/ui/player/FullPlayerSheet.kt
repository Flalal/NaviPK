package fr.flal.navipk.ui.player

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.api.coverArtUrl
import fr.flal.navipk.api.isYoutube
import fr.flal.navipk.player.PlayerManager
import fr.flal.navipk.player.PlayerState
import fr.flal.navipk.ui.theme.OnPlayerPrimary
import fr.flal.navipk.ui.theme.OnPlayerSecondary
import fr.flal.navipk.ui.theme.OnPlayerTertiary
import fr.flal.navipk.ui.theme.PlayerOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    playerState: PlayerState,
    onDismiss: () -> Unit
) {
    var showQueue by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        contentColor = OnPlayerPrimary,
        dragHandle = null
    ) {
        AnimatedContent(
            targetState = showQueue,
            label = "playerQueueSwitch"
        ) { isQueue ->
            if (isQueue) {
                QueueScreen(
                    playerState = playerState,
                    onBack = { showQueue = false },
                    isOverlay = true
                )
            } else {
                NowPlayingContent(
                    playerState = playerState,
                    onDismiss = onDismiss,
                    onQueueClick = { showQueue = true }
                )
            }
        }
    }
}

@Composable
private fun NowPlayingContent(
    playerState: PlayerState,
    onDismiss: () -> Unit,
    onQueueClick: () -> Unit
) {
    val song = playerState.currentSong
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isFavorite by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(playerState.isPlaying, playerState.currentSong) {
        while (true) {
            currentPosition = PlayerManager.getCurrentPosition()
            duration = PlayerManager.getDuration().coerceAtLeast(1L)
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Layer 1: Blurred background
        val coverUrl = song?.coverArtUrl(500)
        if (coverUrl != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Layer 2: Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerOverlay)
        )

        // Layer 3: Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Collapse button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Réduire",
                    tint = OnPlayerPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Album art with crossfade
            Crossfade(
                targetState = coverUrl,
                label = "albumArt"
            ) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = song?.title,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            AnimatedContent(
                targetState = song?.title ?: "",
                label = "title"
            ) { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnPlayerPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Artist
            AnimatedContent(
                targetState = song?.artist ?: "",
                label = "artist"
            ) { artist ->
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnPlayerSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // Album
            Text(
                text = song?.album ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = OnPlayerTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Seek slider
            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                onValueChange = { fraction ->
                    PlayerManager.seekTo((fraction * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = OnPlayerPrimary,
                    activeTrackColor = OnPlayerPrimary,
                    inactiveTrackColor = OnPlayerTertiary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnPlayerSecondary
                )
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnPlayerSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transport controls
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val repeatMode = playerState.repeatMode
                IconButton(
                    onClick = { PlayerManager.toggleRepeatMode() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne
                        else Icons.Default.Repeat,
                        contentDescription = "Répéter",
                        modifier = Modifier.size(28.dp),
                        tint = if (repeatMode == Player.REPEAT_MODE_OFF) OnPlayerTertiary
                        else OnPlayerPrimary
                    )
                }

                IconButton(
                    onClick = { PlayerManager.previous() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Précédent",
                        modifier = Modifier.size(40.dp),
                        tint = OnPlayerPrimary
                    )
                }

                FilledIconButton(
                    onClick = { PlayerManager.togglePlayPause() },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = OnPlayerPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Crossfade(
                        targetState = playerState.isPlaying,
                        label = "playPauseBtn"
                    ) { isPlaying ->
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Lecture",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { PlayerManager.next() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Suivant",
                        modifier = Modifier.size(40.dp),
                        tint = OnPlayerPrimary
                    )
                }

                IconButton(
                    onClick = { PlayerManager.toggleShuffle() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aléatoire",
                        modifier = Modifier.size(28.dp),
                        tint = if (playerState.isShuffled) OnPlayerPrimary
                        else OnPlayerTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row: favorite + queue
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (song?.isYoutube != true) {
                    IconButton(onClick = {
                        val songId = song?.id ?: return@IconButton
                        scope.launch {
                            try {
                                if (isFavorite) {
                                    SubsonicClient.getApi().unstar(songId)
                                } else {
                                    SubsonicClient.getApi().star(songId)
                                }
                                isFavorite = !isFavorite
                            } catch (_: Exception) {}
                        }
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else OnPlayerSecondary
                        )
                    }
                }

                IconButton(onClick = onQueueClick) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "File d'attente",
                        tint = OnPlayerSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
