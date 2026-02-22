package fr.flal.navipk.api

import retrofit2.http.GET
import retrofit2.http.Query

interface SubsonicApi {

    @GET("rest/ping.view")
    suspend fun ping(): SubsonicResponse

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String = "newest",
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0
    ): SubsonicResponse

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/getArtists.view")
    suspend fun getArtists(): SubsonicResponse

    @GET("rest/getArtist.view")
    suspend fun getArtist(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 20
    ): SubsonicResponse

    @GET("rest/getRandomSongs.view")
    suspend fun getRandomSongs(
        @Query("size") size: Int = 50
    ): SubsonicResponse

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(): SubsonicResponse

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/star.view")
    suspend fun star(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/unstar.view")
    suspend fun unstar(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/getStarred2.view")
    suspend fun getStarred2(): SubsonicResponse

    @GET("rest/updatePlaylist.view")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("songIdToAdd") songIdToAdd: String
    ): SubsonicResponse

    @GET("rest/createPlaylist.view")
    suspend fun createPlaylist(
        @Query("name") name: String
    ): SubsonicResponse

    @GET("rest/deletePlaylist.view")
    suspend fun deletePlaylist(
        @Query("id") id: String
    ): SubsonicResponse

    @GET("rest/getSimilarSongs2.view")
    suspend fun getSimilarSongs2(
        @Query("id") id: String,
        @Query("count") count: Int = 50
    ): SubsonicResponse
}
