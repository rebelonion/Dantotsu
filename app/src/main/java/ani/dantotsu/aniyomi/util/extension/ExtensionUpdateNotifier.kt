package ani.dantotsu.aniyomi.util.extension

import android.content.Context
import androidx.core.app.NotificationCompat
import ani.dantotsu.R
import ani.dantotsu.aniyomi.data.NotificationReceiver
import ani.dantotsu.aniyomi.data.Notifications
import ani.dantotsu.aniyomi.util.system.notify

class ExtensionUpdateNotifier(private val context: Context) {

    fun promptUpdates(names: List<String>) {
        context.notify(
            Notifications.ID_UPDATES_TO_EXTS,
            Notifications.CHANNEL_EXTENSIONS_UPDATE,
        ) {
            setContentTitle(
                "Extension updates available"
            )
            val extNames = names.joinToString(", ")
            setContentText(extNames)
            setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
            setSmallIcon(R.drawable.ic_round_favorite_24)
            setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
            setAutoCancel(true)
        }
    }
}
