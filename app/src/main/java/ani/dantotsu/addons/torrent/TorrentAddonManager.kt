package ani.dantotsu.addons.torrent

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ani.dantotsu.addons.AddonLoader
import ani.dantotsu.util.Logger
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.util.system.getApplicationIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorrentAddonManager(
    private val context: Context
) {

    var result: TorrentLoadResult? = null
    var extension: TorrentAddonApi? = null
    var torrentHash: String? = null
    private val _isInitialized = MutableLiveData<Boolean>().apply { value = false }
    val isInitialized: LiveData<Boolean> = _isInitialized

    suspend fun init() {
        if (Build.VERSION.SDK_INT < 23) {
            Logger.log("Torrent extension is not supported on this device.")
            return
        }
        try {
            result = AddonLoader.loadExtension(
                context,
                TORRENT_PACKAGE,
                TORRENT_CLASS,
                AddonLoader.Companion.AddonType.TORRENT
            ) as TorrentLoadResult
            result?.let {
                if (it is TorrentLoadResult.Success) {
                    extension = it.extension.extension
                }
            }
            withContext(Dispatchers.Main) {
                _isInitialized.value = true
            }
        } catch (e: Exception) {
            Logger.log("Error initializing torrent extension")
            Logger.log(e)
        }
    }

    fun isAvailable(): Boolean {
        return extension != null
    }

    private fun hadError(): Boolean = isInitialized.value == true && extension == null

    companion object {

        private const val TORRENT_PACKAGE = "dantotsu.torrentAddon"
        private const val TORRENT_CLASS = "ani.dantotsu.torrentAddon.TorrentAddon"
    }
}