package eu.kanade.tachiyomi.extension.anime.installer

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.util.lang.use
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import eu.kanade.tachiyomi.util.system.getUriSize

class PackageInstallerInstallerAnime(private val service: Service) : InstallerAnime(service) {

    private val packageInstaller = service.packageManager.packageInstaller

    private val packageActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val userAction = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)
                    if (userAction == null) {
                        Logger.log("Fatal error for $intent")
                        continueQueue(InstallStep.Error)
                        return
                    }
                    userAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    service.startActivity(userAction)
                }

                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    continueQueue(InstallStep.Idle)
                }

                PackageInstaller.STATUS_SUCCESS -> continueQueue(InstallStep.Installed)
                else -> continueQueue(InstallStep.Error)
            }
        }
    }

    private var activeSession: Pair<Entry, Int>? = null

    // Always ready
    override var ready = true

    override fun processEntry(entry: Entry) {
        super.processEntry(entry)
        activeSession = null
        try {
            val installParams =
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                installParams.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            activeSession = entry to packageInstaller.createSession(installParams)
            val fileSize = service.getUriSize(entry.uri) ?: throw IllegalStateException()
            installParams.setSize(fileSize)

            val inputStream =
                service.contentResolver.openInputStream(entry.uri) ?: throw IllegalStateException()
            val session = packageInstaller.openSession(activeSession!!.second)
            val outputStream = session.openWrite(entry.downloadId.toString(), 0, fileSize)
            session.use {
                arrayOf(inputStream, outputStream).use {
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }

                val intentSender = PendingIntent.getBroadcast(
                    service,
                    activeSession!!.second,
                    Intent(INSTALL_ACTION),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE
                    } else 0
                ).intentSender
                session.commit(intentSender)
            }
        } catch (e: Exception) {
            Logger.log(e)
            Logger.log("Failed to install extension ${entry.downloadId} ${entry.uri}")
            snackString("Failed to install extension ${entry.downloadId} ${entry.uri}")
            activeSession?.let { (_, sessionId) ->
                packageInstaller.abandonSession(sessionId)
            }
            continueQueue(InstallStep.Error)
        }
    }

    override fun cancelEntry(entry: Entry): Boolean {
        activeSession?.let { (activeEntry, sessionId) ->
            if (activeEntry == entry) {
                packageInstaller.abandonSession(sessionId)
                return false
            }
        }
        return true
    }

    override fun onDestroy() {
        service.unregisterReceiver(packageActionReceiver)
        super.onDestroy()
    }

    init {
        ContextCompat.registerReceiver(
            service,
            packageActionReceiver,
            IntentFilter(INSTALL_ACTION),
            ContextCompat.RECEIVER_EXPORTED
        )
    }
}

private const val INSTALL_ACTION = "PackageInstallerInstaller.INSTALL_ACTION"
