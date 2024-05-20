package ani.dantotsu.addons.download

import android.content.Context
import android.net.Uri

interface DownloadAddonApiV2 {

    fun cancelDownload(sessionId: Long)

    fun setDownloadPath(context: Context, uri: Uri): String

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

    fun getState(sessionId: Long): String

    fun getStackTrace(sessionId: Long): String?

    fun hadError(sessionId: Long): Boolean
}
