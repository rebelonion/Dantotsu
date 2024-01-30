package ani.dantotsu.subcriptions

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ani.dantotsu.INCOGNITO_CHANNEL_ID
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.PrefWrapper


class NotificationClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        PrefWrapper.setVal(PrefName.Incognito, false)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(INCOGNITO_CHANNEL_ID)

    }
}