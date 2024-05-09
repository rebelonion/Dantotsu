package ani.dantotsu.addons.download

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ani.dantotsu.R
import ani.dantotsu.addons.AddonDownloader
import ani.dantotsu.addons.AddonInstallReceiver
import ani.dantotsu.addons.AddonListener
import ani.dantotsu.addons.AddonLoader
import ani.dantotsu.addons.AddonManager
import ani.dantotsu.addons.LoadResult
import ani.dantotsu.media.AddonType
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.extension.InstallStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadAddonManager(
    private val context: Context
) : AddonManager<DownloadAddon.Installed>(context) {

    override var extension: DownloadAddon.Installed? = null
    override var name: String = "Download Addon"
    override var type = AddonType.DOWNLOAD

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

        AddonInstallReceiver()
            .setListener(InstallationListener(), type)
            .register(context)
        try {
            val result = AddonLoader.loadExtension(
                context,
                DOWNLOAD_PACKAGE,
                DOWNLOAD_CLASS,
                AddonType.DOWNLOAD
            ) as? DownloadLoadResult
            result?.let {
                if (it is DownloadLoadResult.Success) {
                    extension = it.extension
                    hasUpdate = AddonDownloader.hasUpdate(REPO, it.extension.versionName)
                }
            }
            Logger.log("Download addon initialized successfully")
            withContext(Dispatchers.Main) {
                _isInitialized.value = true
            }
        } catch (e: Exception) {
            Logger.log("Error initializing Download addon")
            Logger.log(e)
            error = e.message
        }
    }

    override fun isAvailable(andEnabled: Boolean): Boolean {
        return extension?.extension != null
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
            if (result is DownloadLoadResult.Success) {
                extension = result.extension
                hasUpdate = false
                onListenerAction?.invoke(AddonListener.ListenerAction.INSTALL)
            }
        }

        override fun onAddonUpdated(result: LoadResult?) {
            if (result is DownloadLoadResult.Success) {
                extension = result.extension
                hasUpdate = false
                onListenerAction?.invoke(AddonListener.ListenerAction.UPDATE)
            }
        }

        override fun onAddonUninstalled(pkgName: String) {
            if (extension?.pkgName == pkgName) {
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

        const val DOWNLOAD_PACKAGE = "dantotsu.downloadAddon"
        const val DOWNLOAD_CLASS = "ani.dantotsu.downloadAddon.DownloadAddon"
        const val REPO = "rebelonion/Dantotsu-Download-Addon"
    }
}