package ani.dantotsu.addons.torrent

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ani.dantotsu.R
import ani.dantotsu.addons.AddonDownloader.Companion.hasUpdate
import ani.dantotsu.addons.AddonInstallReceiver
import ani.dantotsu.addons.AddonListener
import ani.dantotsu.addons.AddonLoader
import ani.dantotsu.addons.AddonManager
import ani.dantotsu.addons.LoadResult
import ani.dantotsu.media.AddonType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.extension.InstallStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorrentAddonManager(
    private val context: Context
) : AddonManager<TorrentAddon.Installed>(context) {
    override var extension: TorrentAddon.Installed? = null
    override var name: String = "Torrent Addon"
    override var type: AddonType = AddonType.TORRENT
    var torrentHash: String? = null

    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> = _isInitialized

    private var error: String? = null

    override suspend fun init() {
        extension = null
        error = null
        hasUpdate = false
        withContext(Dispatchers.Main) {
            _isInitialized.value = false
        }
        if (Build.VERSION.SDK_INT < 23) {
            Logger.log("Torrent extension is not supported on this device.")
            error = context.getString(R.string.torrent_extension_not_supported)
            return
        }

        AddonInstallReceiver()
            .setListener(InstallationListener(), type)
            .register(context)
        try {
            val result = AddonLoader.loadExtension(
                context,
                TORRENT_PACKAGE,
                TORRENT_CLASS,
                type
            ) as TorrentLoadResult?
            result?.let {
                if (it is TorrentLoadResult.Success) {
                    extension = it.extension
                    hasUpdate = hasUpdate(REPO, it.extension.versionName)
                }
            }
            Logger.log("Torrent addon initialized successfully")
            withContext(Dispatchers.Main) {
                _isInitialized.value = true
            }
        } catch (e: Exception) {
            Logger.log("Error initializing torrent addon")
            Logger.log(e)
            error = e.message
        }
    }

    override fun isAvailable(andEnabled: Boolean): Boolean {
        return extension?.extension != null && if (andEnabled) {
            PrefManager.getVal(PrefName.TorrentEnabled)
        } else true
    }

    override fun getVersion(): String? {
        return extension?.versionName
    }

    override fun getPackageName(): String? {
        return extension?.pkgName
    }

    override fun hadError(context: Context): String? {
        return if (isInitialized.value == true) {
            if (error != null) {
                error
            } else if (extension != null) {
                context.getString(R.string.loaded_successfully)
            } else {
                null
            }
        } else {
            null
        }
    }

    private inner class InstallationListener : AddonListener {
        override fun onAddonInstalled(result: LoadResult?) {
            if (result is TorrentLoadResult.Success) {
                extension = result.extension
                hasUpdate = false
                onListenerAction?.invoke(AddonListener.ListenerAction.INSTALL)
            }
        }

        override fun onAddonUpdated(result: LoadResult?) {
            if (result is TorrentLoadResult.Success) {
                extension = result.extension
                hasUpdate = false
                onListenerAction?.invoke(AddonListener.ListenerAction.UPDATE)
            }
        }

        override fun onAddonUninstalled(pkgName: String) {
            if (pkgName == TORRENT_PACKAGE) {
                extension = null
                hasUpdate = false
                onListenerAction?.invoke(AddonListener.ListenerAction.UNINSTALL)
            }
        }
    }

    override fun updateInstallStep(id: Long, step: InstallStep) {
        installer.updateInstallStep(id, step)
    }

    override fun setInstalling(id: Long) {
        installer.updateInstallStep(id, InstallStep.Installing)
    }

    companion object {
        const val TORRENT_PACKAGE = "dantotsu.torrentAddon"
        const val TORRENT_CLASS = "ani.dantotsu.torrentAddon.TorrentAddon"
        const val REPO = "rebelonion/Dantotsu-Torrent-Addon"
    }
}