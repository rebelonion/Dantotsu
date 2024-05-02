package ani.dantotsu.addons

import android.content.Context
import ani.dantotsu.media.AddonType
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import rx.Observable

abstract class AddonManager<T : Addon.Installed>(
    private val context: Context
) {
    abstract var extension: T?
    abstract var name: String
    abstract var type: AddonType
    protected val installer by lazy { ExtensionInstaller(context) }
    var hasUpdate: Boolean = false
        protected set

    protected var onListenerAction: ((AddonListener.ListenerAction) -> Unit)? = null

    abstract suspend fun init()
    abstract fun isAvailable(andEnabled: Boolean = true): Boolean
    abstract fun getVersion(): String?
    abstract fun getPackageName(): String?
    abstract fun hadError(context: Context): String?
    abstract fun updateInstallStep(id: Long, step: InstallStep)
    abstract fun setInstalling(id: Long)

    fun uninstall() {
        getPackageName()?.let {
            installer.uninstallApk(it)
        }
    }

    fun addListenerAction(action: (AddonListener.ListenerAction) -> Unit) {
        onListenerAction = action
    }

    fun removeListenerAction() {
        onListenerAction = null
    }

    fun install(url: String): Observable<InstallStep> {
        return installer.downloadAndInstall(url, getPackageName() ?: "", name, type)
    }
}