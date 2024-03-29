package eu.kanade.tachiyomi.data.torrentServer.service

import android.app.Application
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.torrentServer.TorrentServerApi
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
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
            if (TorrentServerApi.echo() == "") {
                if (BuildConfig.DEBUG) Log.d("TorrentService", "startServer()")
                server.Server.start(filesDir.absolutePath, "", "", "", "", false, false, false)
            }
        }
    }

    private fun stopServer() {
        serviceScope.launch {
            if (BuildConfig.DEBUG) Log.d("TorrentService", "stopServer()")
            server.Server.stop()
            TorrentServerApi.shutdown()
            applicationContext.cancelNotification(Notifications.ID_TORRENT_SERVER)
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
        val builder = context.notificationBuilder(Notifications.CHANNEL_TORRENT_SERVER) {
            setSmallIcon(R.drawable.ic_download_24)
            setContentText(getString(R.string.torrent_running))
            setContentTitle(getString(R.string.torrserver))
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
                Notifications.ID_TORRENT_SERVER,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(Notifications.ID_TORRENT_SERVER, builder.build())
        }
    }

    companion object {
        const val ACTION_START = "start_torrent_server"
        const val ACTION_STOP = "stop_torrent_server"
        val applicationContext = Injekt.get<Application>()

        fun start() {
            try {
                val intent =
                    Intent(applicationContext, TorrentServerService::class.java).apply {
                        action = ACTION_START
                    }
                applicationContext.startService(intent)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.d("TorrentService", "start() error: ${e.message}")
                e.printStackTrace()
            }
        }

        fun stop() {
            try {
                val intent =
                    Intent(applicationContext, TorrentServerService::class.java).apply {
                        action = ACTION_STOP
                    }
                applicationContext.startService(intent)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.d("TorrentService", "stop() error: ${e.message}")
                e.printStackTrace()
            }
        }

        fun wait(timeout: Int = -1): Boolean {
            var count = 0
            if (timeout < 0) {
                count = -20
            }
            while (TorrentServerApi.echo() == "") {
                Thread.sleep(1000)
                count++
                if (count > timeout) {
                    return false
                }
            }
            return true
        }
    }
}
