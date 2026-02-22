package fr.flal.navipk.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.flal.navipk.api.Song
import fr.flal.navipk.api.SubsonicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

enum class DownloadState { DOWNLOADING, DONE, ERROR }

data class CacheEntry(
    val songId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumId: String?,
    val coverArt: String?,
    val duration: Int?,
    val suffix: String?,
    val fileName: String,
    val cachedAt: Long
)

object CacheManager {

    private lateinit var cacheDir: File
    private lateinit var indexFile: File
    private val gson = Gson()
    private val index = mutableMapOf<String, CacheEntry>()

    private val _cachedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val cachedSongIds: StateFlow<Set<String>> = _cachedSongIds.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        cacheDir = File(context.filesDir, "audio_cache")
        cacheDir.mkdirs()
        indexFile = File(cacheDir, "index.json")
        loadIndex()
    }

    private fun loadIndex() {
        if (indexFile.exists()) {
            try {
                val type = object : TypeToken<Map<String, CacheEntry>>() {}.type
                val loaded: Map<String, CacheEntry> = gson.fromJson(indexFile.readText(), type)
                index.clear()
                index.putAll(loaded)
            } catch (_: Exception) {
                index.clear()
            }
        }
        _cachedSongIds.value = index.keys.toSet()
    }

    private fun saveIndex() {
        indexFile.writeText(gson.toJson(index))
        _cachedSongIds.value = index.keys.toSet()
    }

    fun getPlaybackUri(songId: String): String {
        val entry = index[songId]
        if (entry != null) {
            val file = File(cacheDir, entry.fileName)
            if (file.exists()) return file.toURI().toString()
        }
        return SubsonicClient.getStreamUrl(songId)
    }

    suspend fun downloadSong(song: Song) {
        if (index.containsKey(song.id)) return

        _downloadStates.value = _downloadStates.value + (song.id to DownloadState.DOWNLOADING)

        try {
            val url = SubsonicClient.getStreamUrl(song.id)
            val suffix = song.suffix ?: "mp3"
            val fileName = "${song.id}.$suffix"
            val targetFile = File(cacheDir, fileName)

            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                    resp.body?.byteStream()?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw Exception("Empty body")
                }
            }

            val entry = CacheEntry(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumId = song.albumId,
                coverArt = song.coverArt,
                duration = song.duration,
                suffix = suffix,
                fileName = fileName,
                cachedAt = System.currentTimeMillis()
            )
            index[song.id] = entry
            saveIndex()
            _downloadStates.value = _downloadStates.value + (song.id to DownloadState.DONE)
        } catch (_: Exception) {
            _downloadStates.value = _downloadStates.value + (song.id to DownloadState.ERROR)
        }
    }

    suspend fun downloadSongs(songs: List<Song>) {
        for (song in songs) {
            downloadSong(song)
        }
    }

    fun removeSong(songId: String) {
        val entry = index.remove(songId) ?: return
        File(cacheDir, entry.fileName).delete()
        saveIndex()
        _downloadStates.value = _downloadStates.value - songId
    }

    fun clearCache() {
        index.keys.toList().forEach { removeSong(it) }
    }

    fun getCacheSizeBytes(): Long {
        return index.values.sumOf { entry ->
            File(cacheDir, entry.fileName).let { if (it.exists()) it.length() else 0L }
        }
    }

    fun evictToLimit(maxBytes: Long) {
        if (getCacheSizeBytes() <= maxBytes) return
        val sorted = index.values.sortedBy { it.cachedAt }
        for (entry in sorted) {
            removeSong(entry.songId)
            if (getCacheSizeBytes() <= maxBytes) break
        }
    }

    fun getCachedSongs(): List<CacheEntry> {
        return index.values.sortedByDescending { it.cachedAt }
    }

    fun toSong(entry: CacheEntry): Song {
        return Song(
            id = entry.songId,
            title = entry.title,
            artist = entry.artist,
            album = entry.album,
            albumId = entry.albumId,
            coverArt = entry.coverArt,
            duration = entry.duration,
            suffix = entry.suffix
        )
    }
}
