package fr.flal.navipk.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import fr.flal.navipk.api.Song
import fr.flal.navipk.api.coverArtUrl
import fr.flal.navipk.api.isYoutube
import fr.flal.navipk.api.youtubeId
import fr.flal.navipk.api.youtube.YoutubeClient
import fr.flal.navipk.data.CacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isShuffled: Boolean = false,
    val isLoadingYoutube: Boolean = false
)

object PlayerManager {

    private var mediaController: MediaController? = null
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    private var originalQueue: List<Song> = emptyList()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun connect(context: Context) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            setupListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = mediaController?.currentMediaItemIndex ?: 0
                val queue = _state.value.queue
                val song = if (index in queue.indices) queue[index] else null
                _state.value = _state.value.copy(
                    currentSong = song,
                    currentIndex = index,
                    duration = mediaController?.duration ?: 0L
                )
                // Pre-resolve next YouTube songs
                preResolveUpcoming(index)
            }
        })
    }

    private fun preResolveUpcoming(currentIndex: Int) {
        val queue = _state.value.queue
        val upcoming = queue.drop(currentIndex + 1).take(3)
        for (song in upcoming) {
            if (song.isYoutube && YoutubeClient.getCachedStreamUrl(song.youtubeId) == null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        YoutubeClient.getStreamUrl(song.youtubeId)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun buildMediaItem(s: Song): MediaItem {
        val uri = if (s.isYoutube) {
            val cached = YoutubeClient.getCachedStreamUrl(s.youtubeId)
            if (cached.isNullOrBlank()) {
                Log.w("PlayerManager", "No cached stream URL for: ${s.title}")
            }
            cached ?: ""
        } else {
            CacheManager.getPlaybackUri(s.id)
        }
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(s.title)
                    .setArtist(s.artist)
                    .setAlbumTitle(s.album)
                    .setArtworkUri(s.coverArtUrl(500)?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }

    fun shufflePlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        originalQueue = songs
        val shuffled = songs.shuffled()
        playSong(shuffled.first(), shuffled, isShuffled = true)
    }

    fun playSong(song: Song, queue: List<Song>, isShuffled: Boolean = false) {
        val controller = mediaController ?: return
        if (!isShuffled) {
            originalQueue = queue
        }

        val hasYoutube = queue.any { it.isYoutube }
        if (hasYoutube) {
            _state.value = _state.value.copy(isLoadingYoutube = true)
            scope.launch {
                try {
                    // Resolve clicked song + next 2-3 in parallel
                    val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                    val toResolve = queue.filterIndexed { i, s ->
                        s.isYoutube && i >= startIndex && i < startIndex + 4
                    }
                    toResolve.map { s ->
                        async(Dispatchers.IO) {
                            try {
                                YoutubeClient.getStreamUrl(s.youtubeId)
                            } catch (e: Exception) {
                                Log.e("PlayerManager", "Failed to resolve YouTube URL for: ${s.title}", e)
                            }
                        }
                    }.awaitAll()

                    val mediaItems = queue.map { buildMediaItem(it) }
                    controller.setMediaItems(mediaItems, startIndex, 0L)
                    controller.prepare()
                    controller.play()

                    _state.value = _state.value.copy(
                        currentSong = song,
                        queue = queue,
                        currentIndex = startIndex,
                        isPlaying = true,
                        isShuffled = isShuffled,
                        isLoadingYoutube = false
                    )

                    // Resolve remaining YouTube songs in background
                    val remaining = queue.filterIndexed { i, s ->
                        s.isYoutube && (i < startIndex || i >= startIndex + 4)
                    }
                    for (s in remaining) {
                        scope.launch(Dispatchers.IO) {
                            try { YoutubeClient.getStreamUrl(s.youtubeId) } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {
                    _state.value = _state.value.copy(isLoadingYoutube = false)
                }
            }
        } else {
            val mediaItems = queue.map { buildMediaItem(it) }
            val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()

            _state.value = _state.value.copy(
                currentSong = song,
                queue = queue,
                currentIndex = startIndex,
                isPlaying = true,
                isShuffled = isShuffled
            )
        }
    }

    fun playNext(song: Song) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()
        val currentIndex = controller.currentMediaItemIndex
        val insertIndex = currentIndex + 1

        if (song.isYoutube) {
            scope.launch {
                try {
                    YoutubeClient.getStreamUrl(song.youtubeId)
                } catch (_: Exception) {}
                val mediaItem = buildMediaItem(song)
                controller.addMediaItem(insertIndex, mediaItem)
                currentQueue.add(insertIndex, song)
                _state.value = _state.value.copy(queue = currentQueue)
            }
        } else {
            val mediaItem = buildMediaItem(song)
            controller.addMediaItem(insertIndex, mediaItem)
            currentQueue.add(insertIndex, song)
            _state.value = _state.value.copy(queue = currentQueue)
        }
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()

        if (song.isYoutube) {
            scope.launch {
                try {
                    YoutubeClient.getStreamUrl(song.youtubeId)
                } catch (_: Exception) {}
                val mediaItem = buildMediaItem(song)
                controller.addMediaItem(mediaItem)
                currentQueue.add(song)
                _state.value = _state.value.copy(queue = currentQueue)
            }
        } else {
            val mediaItem = buildMediaItem(song)
            controller.addMediaItem(mediaItem)
            currentQueue.add(song)
            _state.value = _state.value.copy(queue = currentQueue)
        }
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()
        if (index !in currentQueue.indices) return
        controller.removeMediaItem(index)
        currentQueue.removeAt(index)
        val newIndex = controller.currentMediaItemIndex
        val newSong = if (newIndex in currentQueue.indices) currentQueue[newIndex] else null
        _state.value = _state.value.copy(
            queue = currentQueue,
            currentIndex = newIndex,
            currentSong = newSong
        )
    }

    fun playAtIndex(index: Int) {
        val controller = mediaController ?: return
        val queue = _state.value.queue
        if (index !in queue.indices) return
        controller.seekTo(index, 0L)
        controller.play()
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val currentState = _state.value
        val currentSong = currentState.currentSong ?: return

        if (currentState.isShuffled) {
            val restoredQueue = originalQueue
            val newIndex = restoredQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
            val mediaItems = restoredQueue.map { buildMediaItem(it) }
            val position = controller.currentPosition
            controller.setMediaItems(mediaItems, newIndex, position)
            controller.prepare()
            controller.play()
            _state.value = currentState.copy(
                queue = restoredQueue,
                currentIndex = newIndex,
                isShuffled = false
            )
        } else {
            originalQueue = currentState.queue
            val others = currentState.queue.filterIndexed { i, _ -> i != currentState.currentIndex }.shuffled()
            val shuffledQueue = mutableListOf<Song>()
            shuffledQueue.add(currentSong)
            shuffledQueue.addAll(others)
            val mediaItems = shuffledQueue.map { buildMediaItem(it) }
            val position = controller.currentPosition
            controller.setMediaItems(mediaItems, 0, position)
            controller.prepare()
            controller.play()
            _state.value = currentState.copy(
                queue = shuffledQueue,
                currentIndex = 0,
                isShuffled = true
            )
        }
    }

    fun moveInQueue(from: Int, to: Int) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()
        if (from !in currentQueue.indices || to !in currentQueue.indices) return
        controller.moveMediaItem(from, to)
        val song = currentQueue.removeAt(from)
        currentQueue.add(to, song)
        val newIndex = controller.currentMediaItemIndex
        _state.value = _state.value.copy(
            queue = currentQueue,
            currentIndex = newIndex
        )
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun next() {
        mediaController?.seekToNextMediaItem()
    }

    fun previous() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val newMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = newMode
        _state.value = _state.value.copy(repeatMode = newMode)
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return mediaController?.duration ?: 0L
    }
}
