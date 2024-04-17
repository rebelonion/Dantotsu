package ani.dantotsu.addons.torrent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorrentExtensionManager(
    private val context: Context
) {
    var result: TorrentLoadResult? = null
    var extension: TorrentExtensionApi? = null
    var torrentHash: String? = null
    private val _isInitialized = MutableLiveData<Boolean>().apply { value = false }
    val isInitialized: LiveData<Boolean> = _isInitialized

    suspend fun init() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            Logger.log("Torrent extension is not supported on this device.")
            return
        }
        result = ExtensionLoader.loadTorrentExtension(context)
        result?.let {
            if (it is TorrentLoadResult.Success) {
                extension = it.extension.extension
            }
        }
        withContext(Dispatchers.Main) {
            _isInitialized.value = true
        }
    }

    fun isAvailable(): Boolean {
        return extension != null
    }
}