package fr.flal.navipk.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateManager {

    data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String,
        val name: String,
        val body: String,
        val assets: List<GitHubAsset>
    )

    data class GitHubAsset(
        val name: String,
        @SerializedName("browser_download_url") val browserDownloadUrl: String,
        val size: Long
    )

    data class UpdateState(
        val release: GitHubRelease? = null,
        val isDownloading: Boolean = false,
        val downloadProgress: Float = 0f
    )

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun checkForUpdate(currentVersion: String) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/Flalal/NaviPK/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext

                val body = response.body?.string() ?: return@withContext
                val release = gson.fromJson(body, GitHubRelease::class.java)

                val remoteVersion = release.tagName.removePrefix("v")
                if (isNewerVersion(currentVersion, remoteVersion)) {
                    _state.value = _state.value.copy(release = release)
                }
            } catch (_: Exception) {
                // Silently fail — update check is not critical
            }
        }
    }

    suspend fun downloadAndInstall(context: Context) {
        val release = _state.value.release ?: return
        val asset = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return

        _state.value = _state.value.copy(isDownloading = true, downloadProgress = 0f)

        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(asset.browserDownloadUrl)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    _state.value = _state.value.copy(isDownloading = false)
                    return@withContext
                }

                val responseBody = response.body ?: run {
                    _state.value = _state.value.copy(isDownloading = false)
                    return@withContext
                }

                val updatesDir = File(context.cacheDir, "updates")
                updatesDir.mkdirs()
                val apkFile = File(updatesDir, "NaviPK.apk")

                val totalBytes = responseBody.contentLength()
                var bytesRead = 0L

                responseBody.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (totalBytes > 0) {
                                _state.value = _state.value.copy(
                                    downloadProgress = bytesRead.toFloat() / totalBytes.toFloat()
                                )
                            }
                        }
                    }
                }

                _state.value = _state.value.copy(isDownloading = false, downloadProgress = 1f)

                // Launch install intent on main thread
                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(isDownloading = false)
            }
        }
    }

    fun dismiss() {
        _state.value = _state.value.copy(release = null)
    }

    internal fun isNewerVersion(current: String, remote: String): Boolean {
        val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
