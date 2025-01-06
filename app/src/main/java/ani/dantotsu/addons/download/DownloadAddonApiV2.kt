package ani.dantotsu.addons.download

import android.content.Context
import android.net.Uri

interface DownloadAddonApiV2 {

    fun cancelDownload(sessionId: Long)

    fun setDownloadPath(context: Context, uri: Uri): String

    fun getReadPath(context: Context, uri: Uri): String

    suspend fun executeFFProbe(
        videoUrl: String,
        headers: Map<String, String> = emptyMap(),
        logCallback: (String) -> Unit
    )

    suspend fun executeFFMpeg(
        videoUrl: String,
        downloadPath: String,
        headers: Map<String, String> = emptyMap(),
        subtitleUrls: List<Pair<String, String>> = emptyList(),
        audioUrls: List<Pair<String, String>> = emptyList(),
        statCallback: (Double) -> Unit
    ): Long

    suspend fun customFFMpeg(
        command: String,
        videoUrls: List<String>,
        logCallback: (String) -> Unit
    ): Long

    suspend fun customFFProbe(
        command: String,
        videoUrls: List<String>,
        logCallback: (String) -> Unit
    )

    fun getState(sessionId: Long): String

    fun getStackTrace(sessionId: Long): String?

    fun hadError(sessionId: Long): Boolean

    fun getFileExtension(): Pair<String, String> = Pair("mkv", "video/x-matroska")
}
