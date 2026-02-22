package fr.flal.navipk.api.youtube

import android.util.Log
import fr.flal.navipk.api.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object YoutubeClient {

    private const val TAG = "YoutubeClient"

    // Cache: videoUrl -> Pair(streamUrl, expiryTimestamp)
    private val streamUrlCache = mutableMapOf<String, Pair<String, Long>>()
    private const val CACHE_DURATION_MS = 4 * 60 * 60 * 1000L // 4 hours

    fun init() {
        NewPipe.init(YoutubeDownloader())
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val extractor = ServiceList.YouTube.getSearchExtractor(query)
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    Song(
                        id = "yt:${item.url}",
                        title = item.name,
                        artist = item.uploaderName,
                        duration = item.duration.toInt(),
                        coverArt = item.thumbnails.lastOrNull()?.url
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for '$query'", e)
            emptyList()
        }
    }

    suspend fun getStreamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        getCachedStreamUrl(videoUrl)?.let { return@withContext it }

        Log.d(TAG, "Resolving stream URL for: $videoUrl")

        // Use StreamInfo.getInfo() for robust extraction (handles consent, errors, retries)
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)

        Log.d(TAG, "Audio streams: ${info.audioStreams.size}, Video streams: ${info.videoStreams.size}")

        // Try audio-only streams first (sorted by bitrate, best first)
        var streamUrl = info.audioStreams
            .filter { it.content.isNotBlank() }
            .sortedByDescending { it.averageBitrate }
            .firstOrNull()
            ?.content

        // Fallback to progressive video+audio streams
        if (streamUrl.isNullOrBlank()) {
            Log.d(TAG, "No audio-only streams, falling back to video streams")
            streamUrl = info.videoStreams
                .filter { !it.isVideoOnly && it.content.isNotBlank() }
                .firstOrNull()
                ?.content
        }

        if (streamUrl.isNullOrBlank()) {
            throw Exception("No playable streams found for $videoUrl")
        }

        Log.d(TAG, "Stream URL resolved (${streamUrl.length} chars)")
        streamUrlCache[videoUrl] = Pair(streamUrl, System.currentTimeMillis() + CACHE_DURATION_MS)
        streamUrl
    }

    fun getCachedStreamUrl(videoUrl: String): String? {
        val cached = streamUrlCache[videoUrl] ?: return null
        return if (System.currentTimeMillis() < cached.second) cached.first else {
            streamUrlCache.remove(videoUrl)
            null
        }
    }
}
