package ani.dantotsu.addons.download

import android.content.Context
import android.net.Uri

interface DownloadAddonApi {

    fun cancelDownload(sessionId: Long)

    fun setDownloadPath(context: Context, uri: Uri): String

    suspend fun executeFFProbe(request: String, logCallback: (String) -> Unit)

    suspend fun executeFFMpeg(request: String, statCallback: (Double) -> Unit): Long

    fun getState(sessionId: Long): String

    fun getStackTrace(sessionId: Long): String?

    fun hadError(sessionId: Long): Boolean
}