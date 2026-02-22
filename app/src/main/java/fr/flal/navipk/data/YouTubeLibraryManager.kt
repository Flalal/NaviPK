package fr.flal.navipk.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.flal.navipk.api.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

data class YouTubePlaylist(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val createdAt: Long
)

private data class YouTubeLibraryData(
    val favorites: List<Song> = emptyList(),
    val playlists: List<YouTubePlaylist> = emptyList()
)

object YouTubeLibraryManager {

    private lateinit var dataFile: File
    private val gson = Gson()
    private var data = YouTubeLibraryData()

    private val _favorites = MutableStateFlow<List<Song>>(emptyList())
    val favorites: StateFlow<List<Song>> = _favorites.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSongIds: StateFlow<Set<String>> = _favoriteSongIds.asStateFlow()

    private val _playlists = MutableStateFlow<List<YouTubePlaylist>>(emptyList())
    val playlists: StateFlow<List<YouTubePlaylist>> = _playlists.asStateFlow()

    fun init(context: Context) {
        dataFile = File(context.filesDir, "youtube_library.json")
        load()
    }

    private fun load() {
        if (dataFile.exists()) {
            try {
                val type = object : TypeToken<YouTubeLibraryData>() {}.type
                data = gson.fromJson(dataFile.readText(), type)
            } catch (_: Exception) {
                data = YouTubeLibraryData()
            }
        }
        publish()
    }

    private fun save() {
        dataFile.writeText(gson.toJson(data))
        publish()
    }

    private fun publish() {
        _favorites.value = data.favorites
        _favoriteSongIds.value = data.favorites.map { it.id }.toSet()
        _playlists.value = data.playlists
    }

    fun toggleFavorite(song: Song) {
        val current = data.favorites.toMutableList()
        val existing = current.indexOfFirst { it.id == song.id }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            current.add(song)
        }
        data = data.copy(favorites = current)
        save()
    }

    fun isFavorite(songId: String): Boolean {
        return songId in _favoriteSongIds.value
    }

    fun createPlaylist(name: String): YouTubePlaylist {
        val playlist = YouTubePlaylist(
            id = "ytpl:${UUID.randomUUID()}",
            name = name,
            songs = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        data = data.copy(playlists = data.playlists + playlist)
        save()
        return playlist
    }

    fun deletePlaylist(id: String) {
        data = data.copy(playlists = data.playlists.filter { it.id != id })
        save()
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        data = data.copy(
            playlists = data.playlists.map { pl ->
                if (pl.id == playlistId && pl.songs.none { it.id == song.id }) {
                    pl.copy(songs = pl.songs + song)
                } else {
                    pl
                }
            }
        )
        save()
    }

    fun getPlaylist(id: String): YouTubePlaylist? {
        return data.playlists.find { it.id == id }
    }
}
