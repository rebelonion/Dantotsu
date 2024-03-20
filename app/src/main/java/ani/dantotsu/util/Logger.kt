package ani.dantotsu.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import ani.dantotsu.BuildConfig
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date
import java.util.concurrent.Executors

object Logger {
    var file: File? = null
    private val loggerExecutor = Executors.newSingleThreadExecutor()

    fun init(context: Context) {
        try {
            if (!PrefManager.getVal<Boolean>(PrefName.LogToFile) || file != null) return
            file = File(context.getExternalFilesDir(null), "log.txt")
            if (file?.exists() == true) {
                val oldFile = File(context.getExternalFilesDir(null), "old_log.txt")
                file?.copyTo(oldFile, true)
            } else {
                file?.createNewFile()
            }
            file?.writeText("log started\n")
            file?.appendText("date/time: ${Date()}\n")
            file?.appendText("device: ${Build.MODEL}\n")
            file?.appendText("os version: ${Build.VERSION.RELEASE}\n")
            file?.appendText(
                "app version: ${
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        0
                    ).versionName
                }\n"
            )
            file?.appendText(
                "app version code: ${
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        0
                    ).run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                            longVersionCode
                        else
                            @Suppress("DEPRECATION") versionCode

                    }
                }\n"
            )
            file?.appendText("sdk version: ${Build.VERSION.SDK_INT}\n")
            file?.appendText("manufacturer: ${Build.MANUFACTURER}\n")
            file?.appendText("brand: ${Build.BRAND}\n")
            file?.appendText("product: ${Build.PRODUCT}\n")
            file?.appendText("device: ${Build.DEVICE}\n")
            file?.appendText("hardware: ${Build.HARDWARE}\n")
            file?.appendText("host: ${Build.HOST}\n")
            file?.appendText("id: ${Build.ID}\n")
            file?.appendText("type: ${Build.TYPE}\n")
            file?.appendText("user: ${Build.USER}\n")
            file?.appendText("tags: ${Build.TAGS}\n")
            file?.appendText("time: ${Build.TIME}\n")
            file?.appendText("radio: ${Build.getRadioVersion()}\n")
            file?.appendText("bootloader: ${Build.BOOTLOADER}\n")
            file?.appendText("board: ${Build.BOARD}\n")
            file?.appendText("fingerprint: ${Build.FINGERPRINT}\n")
            file?.appendText("supported_abis: ${Build.SUPPORTED_ABIS.joinToString()}\n")
            file?.appendText("supported_32_bit_abis: ${Build.SUPPORTED_32_BIT_ABIS.joinToString()}\n")
            file?.appendText("supported_64_bit_abis: ${Build.SUPPORTED_64_BIT_ABIS.joinToString()}\n")
            file?.appendText("is emulator: ${Build.FINGERPRINT.contains("generic")}\n")
            file?.appendText("--------------------------------\n")
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

    fun shareLog(context: Context) {
        if (file == null) {
            snackString("No log file found")
            return
        }
        val oldFile = File(context.getExternalFilesDir(null), "old_log.txt")
        val fileToUse = if (oldFile.exists()) {
            file?.readText()?.let { oldFile.appendText(it) }
            oldFile
        } else {
            file
        }
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", fileToUse!!)
        )
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Log file")
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Log file")
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(shareIntent, "Share log file"))
    }
}

class FinalExceptionHandler : Thread.UncaughtExceptionHandler {
    private val defaultUEH: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        Logger.log(e)
        Injekt.get<CrashlyticsInterface>().logException(e)
        defaultUEH?.uncaughtException(t, e)
    }
}