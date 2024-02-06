package ani.dantotsu.subcriptions

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ani.dantotsu.currContext
import ani.dantotsu.isOnline
import ani.dantotsu.logger
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.subcriptions.Subscription.Companion.defaultTime
import ani.dantotsu.subcriptions.Subscription.Companion.startSubscription
import ani.dantotsu.subcriptions.Subscription.Companion.timeMinutes
import ani.dantotsu.tryWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> tryWith(true) {
                logger("Starting Dantotsu Subscription Service on Boot")
                context?.startSubscription()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val con = context ?: currContext() ?: return@launch
            if (isOnline(con)) Subscription.perform(con)
        }
    }

    companion object {

        fun alarm(context: Context) {
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            alarmIntent.action = "ani.dantotsu.ACTION_ALARM"

            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val curTime = PrefManager.getVal(PrefName.SubscriptionsTimeS, defaultTime)

            if (timeMinutes[curTime] > 0)
                alarmManager.setRepeating(
                    AlarmManager.RTC,
                    System.currentTimeMillis(),
                    (timeMinutes[curTime] * 60 * 1000),
                    pendingIntent
                )
            else alarmManager.cancel(pendingIntent)
        }

    }
}