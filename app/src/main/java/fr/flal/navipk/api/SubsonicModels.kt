package fr.flal.navipk.api

import com.google.gson.annotations.SerializedName

// Envelope de réponse Subsonic
data class SubsonicResponse(
    @SerializedName("subsonic-response") val subsonicResponse: SubsonicResponseBody
)

data class SubsonicResponseBody(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val albumList2: AlbumList2? = null,
    val album: AlbumWithSongs? = null,
    val artists: ArtistsContainer? = null,
    val artist: ArtistWithAlbums? = null,
    val searchResult3: SearchResult3? = null,
    val randomSongs: RandomSongs? = null,
    val starred2: Starred2? = null,
    val playlists: PlaylistsContainer? = null,
    val playlist: PlaylistWithSongs? = null,
    val similarSongs2: SimilarSongs2? = null
)

data class SubsonicError(
    val code: Int,
    val message: String
)

// Albums
data class AlbumList2(
    val album: List<Album>? = null
)

data class Album(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
    val year: Int? = null,
    val genre: String? = null
)

data class AlbumWithSongs(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val song: List<Song>? = null
)

// Songs
data class Song(
    val id: String,
    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val albumId: String? = null,
    val coverArt: String? = null,
    val duration: Int? = null,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val size: Long? = null,
    val suffix: String? = null,
    val bitRate: Int? = null
)

// Artists
data class ArtistsContainer(
    val index: List<ArtistIndex>? = null
)

data class ArtistIndex(
    val name: String,
    val artist: List<Artist>? = null
)

data class Artist(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val albumCount: Int? = null
)

data class ArtistWithAlbums(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val album: List<Album>? = null
)

// Search
data class SearchResult3(
    val artist: List<Artist>? = null,
    val album: List<Album>? = null,
    val song: List<Song>? = null
)

// Random Songs
data class RandomSongs(
    val song: List<Song>? = null
)

// Playlists
data class PlaylistsContainer(
    val playlist: List<Playlist>? = null
)

data class Playlist(
    val id: String,
    val name: String,
    val songCount: Int? = null,
    val duration: Int? = null,
    val coverArt: String? = null
)

data class PlaylistWithSongs(
    val id: String,
    val name: String,
    val songCount: Int? = null,
    val entry: List<Song>? = null
)

// Similar Songs
data class SimilarSongs2(
    val song: List<Song>? = null
)

// Starred / Favorites
data class Starred2(
    val artist: List<Artist>? = null,
    val album: List<Album>? = null,
    val song: List<Song>? = null
)

// YouTube extensions
val Song.isYoutube: Boolean get() = id.startsWith("yt:")
val Song.youtubeId: String get() = id.removePrefix("yt:")

fun Song.coverArtUrl(size: Int = 300): String? {
    val ca = coverArt ?: return null
    return if (isYoutube) ca else SubsonicClient.getCoverArtUrl(ca, size)
}
