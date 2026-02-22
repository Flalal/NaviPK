package fr.flal.navipk.player

import android.util.Log
import fr.flal.navipk.api.Song
import fr.flal.navipk.api.SubsonicClient
import fr.flal.navipk.api.isYoutube
import fr.flal.navipk.api.youtube.YoutubeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object RadioManager {

    private const val TAG = "RadioManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun startRadio(song: Song) {
        if (_isLoading.value) return
        _isLoading.value = true
        scope.launch {
            try {
                val similar = if (song.isYoutube) {
                    getYouTubeSimilar(song)
                } else {
                    getNavidromeSimilar(song)
                }
                val queue = listOf(song) + similar
                PlayerManager.playSong(song, queue)
            } catch (e: Exception) {
                Log.e(TAG, "startRadio failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getYouTubeSimilar(song: Song): List<Song> {
        val query = buildString {
            append(song.title)
            song.artist?.let { append(" $it") }
        }
        return YoutubeClient.search(query).filter { it.id != song.id }
    }

    private suspend fun getNavidromeSimilar(song: Song): List<Song> {
        return try {
            val response = SubsonicClient.getApi().getSimilarSongs2(song.id)
            val songs = response.subsonicResponse.similarSongs2?.song
            if (!songs.isNullOrEmpty()) {
                songs
            } else {
                // Fallback to random songs
                val fallback = SubsonicClient.getApi().getRandomSongs()
                fallback.subsonicResponse.randomSongs?.song?.filter { it.id != song.id } ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getNavidromeSimilar failed, fallback to random", e)
            try {
                val fallback = SubsonicClient.getApi().getRandomSongs()
                fallback.subsonicResponse.randomSongs?.song?.filter { it.id != song.id } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
