package ani.dantotsu.addons.download

import android.content.Context
import android.net.Uri

interface DownloadAddonApi {

    fun cancelDownload(sessionId: Long)

    fun setDownloadPath(context: Context, uri: Uri)

    suspend fun executeFFProbe(request: String, logCallback: (String) -> Unit)

    suspend fun executeFFMpeg(request: String, statCallback: (Double) -> Unit): Long

    fun getState(sessionId: Long): String

    fun getStackTrace(sessionId: Long): String?

    fun hadError(sessionId: Long): Boolean
}

class Stub : DownloadAddonApi {
    override fun cancelDownload(sessionId: Long) {
        throw NotImplementedError("Stub")
    }

    override fun setDownloadPath(context: Context, uri: Uri) {
        throw NotImplementedError("Stub")
    }

    override suspend fun executeFFProbe(request: String, logCallback: (String) -> Unit) {
        throw NotImplementedError("Stub")
    }

    override suspend fun executeFFMpeg(request: String, statCallback: (Double) -> Unit): Long {
        throw NotImplementedError("Stub")
    }

    override fun getState(sessionId: Long): String {
        throw NotImplementedError("Stub")
    }

    override fun getStackTrace(sessionId: Long): String? {
        throw NotImplementedError("Stub")
    }

    override fun hadError(sessionId: Long): Boolean {
        throw NotImplementedError("Stub")
    }
}