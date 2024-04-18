package ani.dantotsu.addons.download

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ani.dantotsu.R
import ani.dantotsu.addons.AddonDownloader
import ani.dantotsu.addons.AddonLoader
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadAddonManager(
    private val context: Context
) {

    var result: DownloadLoadResult? = null
        private set
    var extension: DownloadAddonApi? = null
        private set
    var hasUpdate: Boolean = false
        private set

    private val _isInitialized = MutableLiveData<Boolean>().apply { value = false }
    val isInitialized: LiveData<Boolean> = _isInitialized

    private var error: String? = null

    suspend fun init() {
        result = null
        extension = null
        error = null
        hasUpdate = false
        withContext(Dispatchers.Main) {
            _isInitialized.value = false
        }
        try {
            result = AddonLoader.loadExtension(
                context,
                Download_PACKAGE,
                Download_CLASS,
                AddonLoader.Companion.AddonType.DOWNLOAD
            ) as DownloadLoadResult
            result?.let {
                if (it is DownloadLoadResult.Success) {
                    extension = it.extension.extension
                    hasUpdate = AddonDownloader.hasUpdate(REPO, it.extension.versionName)
                }
            }
            withContext(Dispatchers.Main) {
                _isInitialized.value = true
            }
        } catch (e: Exception) {
            Logger.log("Error initializing Download extension")
            Logger.log(e)
            error = e.message
        }
    }

    fun isAvailable(): Boolean {
        return extension != null
    }

    fun getVersion(): String? {
        return result?.let {
            if (it is DownloadLoadResult.Success) it.extension.versionName else null
        }
    }

    fun hadError(context: Context): String? {
        return if (isInitialized.value == true) {
            error ?: context.getString(R.string.loaded_successfully)
        } else {
            null
        }
    }

    companion object {

        private const val Download_PACKAGE = "dantotsu.downloadAddon"
        private const val Download_CLASS = "ani.dantotsu.downloadAddon.DownloadAddon"
        const val REPO = "rebelonion/Dantotsu-Download-Addon"
    }
}