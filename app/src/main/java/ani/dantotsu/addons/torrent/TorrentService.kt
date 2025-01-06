/**
 * modified source from
 * https://github.com/rebelonion/Dantotsu/pull/305
 * and https://github.com/LuftVerbot/kuukiyomi
 * all credits to the original authors
 */

package ani.dantotsu.addons.torrent

import android.app.ActivityManager
import android.app.Application
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import ani.dantotsu.R
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_TORRENT_SERVER
import eu.kanade.tachiyomi.data.notification.Notifications.ID_TORRENT_SERVER
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.coroutines.EmptyCoroutineContext


class TorrentServerService : Service() {
    private val serviceScope = CoroutineScope(EmptyCoroutineContext)
    private val applicationContext = Injekt.get<Application>()
    private lateinit var extension: TorrentAddonApi

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        extension =
            Injekt.get<TorrentAddonManager>().extension?.extension ?: return START_NOT_STICKY
        intent?.let {
            if (it.action != null) {
                when (it.action) {
                    ACTION_START -> {
                        startServer()
                        notification(applicationContext)
                        return START_STICKY
                    }

                    ACTION_STOP -> {
                        stopServer()
                        return START_NOT_STICKY
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startServer() {
        serviceScope.launch {
            val echo = extension.echo()
            if (echo == "") {
                extension.startServer(filesDir.absolutePath)
            }
        }
    }

    private fun stopServer() {
        serviceScope.launch {
            extension.stopServer()
            applicationContext.cancelNotification(ID_TORRENT_SERVER)
            stopSelf()
        }
    }

    private fun notification(context: Context) {
        val exitPendingIntent =
            PendingIntent.getService(
                applicationContext,
                0,
                Intent(applicationContext, TorrentServerService::class.java).apply {
                    action = ACTION_STOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val builder = context.notificationBuilder(CHANNEL_TORRENT_SERVER) {
            setSmallIcon(R.drawable.notification_icon)
            setContentText("Torrent Server")
            setContentTitle("Server is running…")
            setAutoCancel(false)
            setOngoing(true)
            setUsesChronometer(true)
            addAction(
                R.drawable.ic_circle_cancel,
                "Stop",
                exitPendingIntent,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ID_TORRENT_SERVER,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(ID_TORRENT_SERVER, builder.build())
        }
    }

    companion object {
        const val ACTION_START = "start_torrent_server"
        const val ACTION_STOP = "stop_torrent_server"

        fun isRunning(): Boolean {
            with(Injekt.get<Application>().getSystemService(ACTIVITY_SERVICE) as ActivityManager) {
                @Suppress("DEPRECATION") // We only need our services
                getRunningServices(Int.MAX_VALUE).forEach {
                    if (TorrentServerService::class.java.name.equals(it.service.className)) {
                        return true
                    }
                }
            }
            return false
        }

        fun start() {
            if (Injekt.get<TorrentAddonManager>().extension?.extension == null) {
                return
            }
            try {
                val intent =
                    Intent(Injekt.get<Application>(), TorrentServerService::class.java).apply {
                        action = ACTION_START
                    }
                Injekt.get<Application>().startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        fun stop() {
            try {
                val intent =
                    Intent(Injekt.get<Application>(), TorrentServerService::class.java).apply {
                        action = ACTION_STOP
                    }
                Injekt.get<Application>().startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun wait(timeout: Int = -1): Boolean {
            var count = 0
            if (timeout < 0) {
                count = -20
            }
            var echo = Injekt.get<TorrentAddonManager>().extension?.extension?.echo()
            while (echo == "") {
                Thread.sleep(1000)
                count++
                if (count > timeout) {
                    return false
                }
                echo = Injekt.get<TorrentAddonManager>().extension?.extension?.echo()
            }
            Logger.log("ServerService: Server started: $echo")
            return true
        }

    }
}