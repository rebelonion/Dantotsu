package ani.dantotsu.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import ani.dantotsu.App
import ani.dantotsu.BuildConfig
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.others.CrashActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger.getDeviceAndAppInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date
import java.util.concurrent.Executors
import kotlin.system.exitProcess

object Logger {
    var file: File? = null
    private val loggerExecutor = Executors.newSingleThreadExecutor()

    fun init(context: Context) {
        try {
            if (!PrefManager.getVal<Boolean>(PrefName.LogToFile) || file != null) return
            file = File(context.getExternalFilesDir(null), "log.txt")
            if (file?.exists() == true) {
                if (file!!.length() > 1024 * 1024 * 5) { // 5 MB
                    file?.delete()
                    file?.createNewFile()
                }
            } else {
                file?.createNewFile()
            }
            file?.appendText("log started\n")
            file?.appendText(getDeviceAndAppInfo(context))

        } catch (e: Exception) {
            Injekt.get<CrashlyticsInterface>().logException(e)
            file = null
        }
    }

    fun log(message: String) {
        val trace = Thread.currentThread().stackTrace[3]
        loggerExecutor.execute {
            if (file == null) Log.d("Internal Logger", message)
            else {
                val className = trace.className
                val methodName = trace.methodName
                val lineNumber = trace.lineNumber
                file?.appendText("date/time: ${Date()} | $className.$methodName($lineNumber)\n")
                file?.appendText("message: $message\n-\n")
            }
        }
    }

    fun log(level: Int, message: String, tag: String = "Internal Logger") {
        val trace = Thread.currentThread().stackTrace[3]
        loggerExecutor.execute {
            if (file == null) Log.println(level, tag, message)
            else {
                val className = trace.className
                val methodName = trace.methodName
                val lineNumber = trace.lineNumber
                file?.appendText("date/time: ${Date()} | $className.$methodName($lineNumber)\n")
                file?.appendText("message: $message\n-\n")
            }
        }
    }

    fun log(e: Exception) {
        loggerExecutor.execute {
            if (file == null) e.printStackTrace() else {
                file?.appendText("---------------------------Exception---------------------------\n")
                file?.appendText("date/time: ${Date()} |  ${e.message}\n")
                file?.appendText("trace: ${e.stackTraceToString()}\n")
            }
        }
    }

    fun log(e: Throwable) {
        loggerExecutor.execute {
            if (file == null) e.printStackTrace() else {
                file?.appendText("---------------------------Exception---------------------------\n")
                file?.appendText("date/time: ${Date()} |  ${e.message}\n")
                file?.appendText("trace: ${e.stackTraceToString()}\n")
            }
        }
    }

    fun uncaughtException(t: Thread, e: Throwable) {
        loggerExecutor.execute {
            if (file == null) e.printStackTrace() else {
                file?.appendText("---------------------------Uncaught Exception---------------------------\n")
                file?.appendText("thread: ${t.name}\n")
                file?.appendText("date/time: ${Date()} |  ${e.message}\n")
                file?.appendText("trace: ${e.stackTraceToString()}\n")
            }
        }
    }

    fun shareLog(context: Context) {
        if (file == null) {
            snackString("No log file found")
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.provider",
                file!!
            )
        )
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Log file")
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Log file")
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(shareIntent, "Share log file"))
    }

    fun clearLog() {
        file?.delete()
        file = null
    }

    fun getDeviceAndAppInfo(context: Context): String {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(context.packageName, 0)
        val versionName = pkgInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode
        }

        return buildString {
            append("Date/time: ${Date()}\n")
            append("Device: ${Build.MODEL}\n")
            append("OS version: ${Build.VERSION.RELEASE}\n")
            append("App version: $versionName\n")
            append("App version code: $versionCode\n")
            append("SDK version: ${Build.VERSION.SDK_INT}\n")
            append("Manufacturer: ${Build.MANUFACTURER}\n")
            append("Brand: ${Build.BRAND}\n")
            append("Product: ${Build.PRODUCT}\n")
            append("Device: ${Build.DEVICE}\n")
            append("Hardware: ${Build.HARDWARE}\n")
            append("Host: ${Build.HOST}\n")
            append("ID: ${Build.ID}\n")
            append("Type: ${Build.TYPE}\n")
            append("User: ${Build.USER}\n")
            append("Tags: ${Build.TAGS}\n")
            append("Time: ${Build.TIME}\n")
            append("Radio: ${Build.getRadioVersion()}\n")
            append("Bootloader: ${Build.BOOTLOADER}\n")
            append("Board: ${Build.BOARD}\n")
            append("Fingerprint: ${Build.FINGERPRINT}\n")
            append("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}\n")
            append("Supported 32 bit ABIs: ${Build.SUPPORTED_32_BIT_ABIS.joinToString()}\n")
            append("Supported 64 bit ABIs: ${Build.SUPPORTED_64_BIT_ABIS.joinToString()}\n")
            append("Is emulator: ${Build.FINGERPRINT.contains("generic")}\n")
            append("--------------------------------\n")
        }
    }
}

class FinalExceptionHandler : Thread.UncaughtExceptionHandler {
    private val defaultUEH = Thread.getDefaultUncaughtExceptionHandler()
    private val MAX_STACK_TRACE_SIZE = 131071 //128 KB - 1

    override fun uncaughtException(t: Thread, e: Throwable) {
        val stackTraceString = Log.getStackTraceString(e)
        Injekt.get<CrashlyticsInterface>().logException(e)

        if (App.instance?.applicationContext != null) {
            App.instance?.applicationContext?.let {
                val lastLoadedActivity = App.instance?.mFTActivityLifecycleCallbacks?.lastActivity
                val report = StringBuilder()
                report.append(getDeviceAndAppInfo(it))
                report.append("Thread: ${t.name}\n")
                report.append("Activity: ${lastLoadedActivity}\n")
                report.append("Exception: ${e.message}\n")
                report.append("Stack trace:\n")
                report.append(stackTraceString)
                val reportString = report.toString()
                Logger.uncaughtException(t, Error(reportString))
                val intent = Intent(it, CrashActivity::class.java)
                if (reportString.length > MAX_STACK_TRACE_SIZE) {
                    val subStr = reportString.substring(0, MAX_STACK_TRACE_SIZE)
                    intent.putExtra("stackTrace", subStr)
                } else intent.putExtra("stackTrace", reportString)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                it.startActivity(intent)
            }
        } else {
            Logger.log("App context is null")
            Logger.uncaughtException(t, e)
        }

        defaultUEH?.uncaughtException(t, e)
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(10)
    }
}