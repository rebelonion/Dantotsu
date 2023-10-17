package ani.dantotsu.aniyomi.anime.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import ani.dantotsu.R
import ani.dantotsu.aniyomi.domain.base.BasePreferences
import ani.dantotsu.aniyomi.data.Notifications
import ani.dantotsu.aniyomi.anime.installer.InstallerAnime
import ani.dantotsu.aniyomi.anime.installer.PackageInstallerInstallerAnime
import ani.dantotsu.aniyomi.anime.util.AnimeExtensionInstaller.Companion.EXTRA_DOWNLOAD_ID
import ani.dantotsu.aniyomi.util.system.getSerializableExtraCompat
import ani.dantotsu.aniyomi.util.system.notificationBuilder
import logcat.LogPriority
import ani.dantotsu.aniyomi.util.logcat

class AnimeExtensionInstallService : Service() {

    private var installer: InstallerAnime? = null

    override fun onCreate() {
        val notification = notificationBuilder(Notifications.CHANNEL_EXTENSIONS_UPDATE) {
            setSmallIcon(R.drawable.spinner_icon)
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setContentTitle("Installing Anime Extension...")
            setProgress(100, 100, true)
        }.build()
        startForeground(Notifications.ID_EXTENSION_INSTALLER, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.data
        val id = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)?.takeIf { it != -1L }
        val installerUsed = intent?.getSerializableExtraCompat<BasePreferences.ExtensionInstaller>(
            EXTRA_INSTALLER,
        )
        if (uri == null || id == null || installerUsed == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (installer == null) {
            installer = when (installerUsed) {
                BasePreferences.ExtensionInstaller.PACKAGEINSTALLER -> PackageInstallerInstallerAnime(this)
                else -> {
                    logcat(LogPriority.ERROR) { "Not implemented for installer $installerUsed" }
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        installer!!.addToQueue(id, uri)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        installer?.onDestroy()
        installer = null
    }

    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_INSTALLER = "EXTRA_INSTALLER"

        fun getIntent(
            context: Context,
            downloadId: Long,
            uri: Uri,
            installer: BasePreferences.ExtensionInstaller,
        ): Intent {
            return Intent(context, AnimeExtensionInstallService::class.java)
                .setDataAndType(uri, AnimeExtensionInstaller.APK_MIME)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                .putExtra(EXTRA_INSTALLER, installer)
        }
    }
}
