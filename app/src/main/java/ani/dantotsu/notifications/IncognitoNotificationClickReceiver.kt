package ani.dantotsu.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ani.dantotsu.INCOGNITO_CHANNEL_ID
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName


class IncognitoNotificationClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        PrefManager.setVal(PrefName.Incognito, false)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(INCOGNITO_CHANNEL_ID)

    }
}