package ani.dantotsu.settings

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import ani.dantotsu.R
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.InstallStep
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class InstallerSteps(
    private val notificationManager: NotificationManager,
    private val context: Context
) {

    fun onInstallStep(installStep: InstallStep, extra: () -> Unit) {
        val builder = NotificationCompat.Builder(
            context,
            Notifications.CHANNEL_DOWNLOADER_PROGRESS
        )
            .setSmallIcon(R.drawable.ic_round_sync_24)
            .setContentTitle(context.getString(R.string.installing_extension))
            .setContentText(context.getString(R.string.install_step, installStep))
            .setPriority(NotificationCompat.PRIORITY_LOW)
        notificationManager.notify(1, builder.build())
    }

    fun onError(error: Throwable, extra: () -> Unit) {
        Injekt.get<CrashlyticsInterface>().logException(error)
        Logger.log(error)
        val builder = NotificationCompat.Builder(
            context,
            Notifications.CHANNEL_DOWNLOADER_ERROR
        )
            .setSmallIcon(R.drawable.ic_round_info_24)
            .setContentTitle(context.getString(R.string.installation_failed, error.message))
            .setContentText(context.getString(R.string.error_message, error.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.notify(1, builder.build())
        snackString(context.getString(R.string.installation_failed, error.message))
    }

    fun onComplete(extra: () -> Unit) {
        val builder = NotificationCompat.Builder(
            context,
            Notifications.CHANNEL_DOWNLOADER_PROGRESS
        )
            .setSmallIcon(R.drawable.ic_download_24)
            .setContentTitle(context.getString(R.string.installation_complete))
            .setContentText(context.getString(R.string.extension_has_been_installed))
            .setPriority(NotificationCompat.PRIORITY_LOW)
        notificationManager.notify(1, builder.build())
        snackString(context.getString(R.string.extension_installed))
    }
}