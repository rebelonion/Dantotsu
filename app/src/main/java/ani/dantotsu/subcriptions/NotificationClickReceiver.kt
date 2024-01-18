package ani.dantotsu.subcriptions

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ani.dantotsu.INCOGNITO_CHANNEL_ID


class NotificationClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
            .putBoolean("incognito", false)
            .apply()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(INCOGNITO_CHANNEL_ID)

    }
}