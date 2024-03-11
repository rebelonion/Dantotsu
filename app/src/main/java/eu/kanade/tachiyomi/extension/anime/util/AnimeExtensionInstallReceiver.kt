package eu.kanade.tachiyomi.extension.anime.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import tachiyomi.core.util.lang.launchNow

/**
 * Broadcast receiver that listens for the system's packages installed, updated or removed, and only
 * notifies the given [listener] when the package is an extension.
 *
 * @param listener The listener that should be notified of extension installation events.
 */
internal class AnimeExtensionInstallReceiver(private val listener: Listener) :
    BroadcastReceiver() {

    /**
     * Registers this broadcast receiver
     */
    fun register(context: Context) {
        ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    /**
     * Returns the intent filter this receiver should subscribe to.
     */
    private val filter
        get() = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

    /**
     * Called when one of the events of the [filter] is received. When the package is an extension,
     * it's loaded in background and it notifies the [listener] when finished.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (isReplacing(intent)) return

                launchNow {
                    when (val result = getExtensionFromIntent(context, intent)) {
                        is AnimeLoadResult.Success -> listener.onExtensionInstalled(result.extension)

                        is AnimeLoadResult.Untrusted -> listener.onExtensionUntrusted(result.extension)
                        else -> {}
                    }
                }
            }

            Intent.ACTION_PACKAGE_REPLACED -> {
                launchNow {
                    when (val result = getExtensionFromIntent(context, intent)) {
                        is AnimeLoadResult.Success -> listener.onExtensionUpdated(result.extension)
                        // Not needed as a package can't be upgraded if the signature is different
                        // is LoadResult.Untrusted -> {}
                        else -> {}
                    }
                }
            }

            Intent.ACTION_PACKAGE_REMOVED -> {
                if (isReplacing(intent)) return

                val pkgName = getPackageNameFromIntent(intent)
                if (pkgName != null) {
                    listener.onPackageUninstalled(pkgName)
                }
            }
        }
    }

    /**
     * Returns true if this package is performing an update.
     *
     * @param intent The intent that triggered the event.
     */
    private fun isReplacing(intent: Intent): Boolean {
        return intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
    }

    /**
     * Returns the extension triggered by the given intent.
     *
     * @param context The application context.
     * @param intent The intent containing the package name of the extension.
     */
    private suspend fun getExtensionFromIntent(context: Context, intent: Intent?): AnimeLoadResult {
        val pkgName = getPackageNameFromIntent(intent)
        if (pkgName == null) {
            Logger.log("Package name not found")
            return AnimeLoadResult.Error
        }
        return GlobalScope.async(Dispatchers.Default, CoroutineStart.DEFAULT) {
            AnimeExtensionLoader.loadExtensionFromPkgName(
                context,
                pkgName,
            )
        }.await()
    }

    /**
     * Returns the package name of the installed, updated or removed application.
     */
    private fun getPackageNameFromIntent(intent: Intent?): String? {
        return intent?.data?.encodedSchemeSpecificPart ?: return null
    }

    /**
     * Listener that receives extension installation events.
     */
    interface Listener {
        fun onExtensionInstalled(extension: AnimeExtension.Installed)
        fun onExtensionUpdated(extension: AnimeExtension.Installed)
        fun onExtensionUntrusted(extension: AnimeExtension.Untrusted)
        fun onPackageUninstalled(pkgName: String)
    }
}
