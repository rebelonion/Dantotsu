package ani.dantotsu.aniyomi.anime.custom
/*
import android.app.Application
import ani.dantotsu.aniyomi.data.Notifications
import ani.dantotsu.aniyomi.util.logcat
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import uy.kohesive.injekt.Injekt

class App : Application() {
    override fun onCreate() {
        super<Application>.onCreate()
        Injekt.importModule(AppModule(this))
        Injekt.importModule(PreferenceModule(this))

        setupNotificationChannels()
        if (!LogcatLogger.isInstalled) {
            LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
        }
    }

    private fun setupNotificationChannels() {
        try {
            Notifications.createChannels(this)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to modify notification channels" }
        }
    }
}*/