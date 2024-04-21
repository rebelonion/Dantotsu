package ani.dantotsu.addons

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.addons.torrent.TorrentAddonManager
import ani.dantotsu.media.AddonType
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver.Companion.filter
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver.Companion.getPackageNameFromIntent
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.util.lang.launchNow

internal class AddonInstallReceiver : BroadcastReceiver() {
    private var listener: AddonListener? = null
    private var type: AddonType? = null

    /**
     * Registers this broadcast receiver
     */
    fun register(context: Context) {
        ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    fun setListener(listener: AddonListener, type: AddonType): AddonInstallReceiver {
        this.listener = listener
        this.type = type
        return this
    }

    /**
     * Called when one of the events of the [filter] is received. When the package is an extension,
     * it's loaded in background and it notifies the [listener] when finished.
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (ExtensionInstallReceiver.isReplacing(intent)) return
                launchNow {
                    when (type) {
                        AddonType.DOWNLOAD -> {
                            getPackageNameFromIntent(intent)?.let { packageName ->
                                if (packageName != DownloadAddonManager.DOWNLOAD_PACKAGE) return@launchNow
                                listener?.onAddonInstalled(
                                    AddonLoader.loadFromPkgName(
                                        context,
                                        packageName,
                                        AddonType.DOWNLOAD
                                    )
                                )
                            }
                        }

                        AddonType.TORRENT -> {
                            getPackageNameFromIntent(intent)?.let { packageName ->
                                if (packageName != TorrentAddonManager.TORRENT_PACKAGE) return@launchNow
                                listener?.onAddonInstalled(
                                    AddonLoader.loadFromPkgName(
                                        context,
                                        packageName,
                                        AddonType.TORRENT
                                    )
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }

            Intent.ACTION_PACKAGE_REPLACED -> {
                launchNow {
                    when (type) {
                        AddonType.DOWNLOAD -> {
                            getPackageNameFromIntent(intent)?.let { packageName ->
                                if (packageName != DownloadAddonManager.DOWNLOAD_PACKAGE) return@launchNow
                                listener?.onAddonUpdated(
                                    AddonLoader.loadFromPkgName(
                                        context,
                                        packageName,
                                        AddonType.DOWNLOAD
                                    )
                                )
                            }
                        }

                        AddonType.TORRENT -> {
                            getPackageNameFromIntent(intent)?.let { packageName ->
                                if (packageName != TorrentAddonManager.TORRENT_PACKAGE) return@launchNow
                                listener?.onAddonUpdated(
                                    AddonLoader.loadFromPkgName(
                                        context,
                                        packageName,
                                        AddonType.TORRENT
                                    )
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }

            Intent.ACTION_PACKAGE_REMOVED -> {
                if (ExtensionInstallReceiver.isReplacing(intent)) return
                getPackageNameFromIntent(intent)?.let { packageName ->
                    when (type) {
                        AddonType.DOWNLOAD -> {
                            if (packageName != DownloadAddonManager.DOWNLOAD_PACKAGE) return
                            listener?.onAddonUninstalled(packageName)
                        }

                        AddonType.TORRENT -> {
                            if (packageName != TorrentAddonManager.TORRENT_PACKAGE) return
                            listener?.onAddonUninstalled(packageName)
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}