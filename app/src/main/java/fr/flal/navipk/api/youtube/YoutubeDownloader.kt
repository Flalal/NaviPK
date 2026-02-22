package fr.flal.navipk.api.youtube

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class YoutubeDownloader : Downloader() {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0"
    }

    private val cookies = mutableMapOf<String, String>().apply {
        // Pre-set EU consent cookie to bypass consent page
        put("CONSENT", "PENDING+999")
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val requestBuilder = okhttp3.Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())

        // Browser User-Agent (YouTube serves different pages based on UA)
        requestBuilder.header("User-Agent", USER_AGENT)

        // Send stored cookies
        val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        if (cookieHeader.isNotEmpty()) {
            requestBuilder.addHeader("Cookie", cookieHeader)
        }

        // Forward headers from NewPipeExtractor
        request.headers().forEach { (name, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(name, value)
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        // Store cookies from response for subsequent requests
        response.headers("Set-Cookie").forEach { setCookie ->
            val nameValue = setCookie.split(";")[0]
            val parts = nameValue.split("=", limit = 2)
            if (parts.size == 2) {
                cookies[parts[0].trim()] = parts[1].trim()
            }
        }

        val responseBody = response.body?.string()
        val responseHeaders = mutableMapOf<String, List<String>>()
        response.headers.forEach { (name, value) ->
            responseHeaders[name] = responseHeaders.getOrDefault(name, emptyList()) + value
        }

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            response.request.url.toString()
        )
    }
}
