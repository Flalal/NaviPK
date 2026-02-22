package fr.flal.navipk.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object SubsonicClient {

    private var api: SubsonicApi? = null
    private var baseUrl: String = ""
    private var username: String = ""
    private var token: String = ""
    private var salt: String = ""

    fun configure(serverUrl: String, user: String, pass: String) {
        baseUrl = serverUrl.trimEnd('/')
        username = user
        salt = generateSalt()
        token = md5("$pass$salt")

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val url = originalRequest.url.newBuilder()
                .addQueryParameter("u", username)
                .addQueryParameter("t", token)
                .addQueryParameter("s", salt)
                .addQueryParameter("v", "1.16.1")
                .addQueryParameter("c", "NaviPK")
                .addQueryParameter("f", "json")
                .build()

            chain.proceed(originalRequest.newBuilder().url(url).build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SubsonicApi::class.java)
    }

    fun getApi(): SubsonicApi {
        return api ?: throw IllegalStateException("SubsonicClient not configured. Call configure() first.")
    }

    fun getStreamUrl(songId: String): String {
        return "$baseUrl/rest/stream.view?id=$songId&u=$username&t=$token&s=$salt&v=1.16.1&c=NaviPK"
    }

    fun getCoverArtUrl(coverArtId: String, size: Int = 300): String {
        return "$baseUrl/rest/getCoverArt.view?id=$coverArtId&size=$size&u=$username&t=$token&s=$salt&v=1.16.1&c=NaviPK"
    }

    private fun generateSalt(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12).map { chars.random() }.joinToString("")
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
