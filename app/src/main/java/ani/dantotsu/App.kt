package ani.dantotsu

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.util.LongSparseArray
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import ani.dantotsu.aniyomi.anime.custom.AppModule
import ani.dantotsu.aniyomi.anime.custom.PreferenceModule
import ani.dantotsu.download.Download
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.others.DisabledReports
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.parsers.NovelSources
import ani.dantotsu.parsers.novel.NovelExtensionManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.HarmonizedColorAttributes
import com.google.android.material.color.HarmonizedColors
import com.google.android.material.color.HarmonizedColorsOptions
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import tachiyomi.core.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.lang.reflect.Field


@SuppressLint("StaticFieldLeak")
class App : MultiDexApplication() {
    private lateinit var animeExtensionManager: AnimeExtensionManager
    private lateinit var mangaExtensionManager: MangaExtensionManager
    private lateinit var novelExtensionManager: NovelExtensionManager
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    init {
        instance = this
    }

    val mFTActivityLifecycleCallbacks = FTActivityLifecycleCallbacks()

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
        val useMaterialYou = sharedPreferences.getBoolean("use_material_you", false)
        if(useMaterialYou) {
            DynamicColors.applyToActivitiesIfAvailable(this)
            //TODO: HarmonizedColors
        }
        registerActivityLifecycleCallbacks(mFTActivityLifecycleCallbacks)

        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!DisabledReports)

        Injekt.importModule(AppModule(this))
        Injekt.importModule(PreferenceModule(this))

        initializeNetwork(baseContext)

        setupNotificationChannels()
        if (!LogcatLogger.isInstalled) {
            LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
        }

        animeExtensionManager = Injekt.get()
        mangaExtensionManager = Injekt.get()
        novelExtensionManager = Injekt.get()

        val animeScope = CoroutineScope(Dispatchers.Default)
        animeScope.launch {
            animeExtensionManager.findAvailableExtensions()
            logger("Anime Extensions: ${animeExtensionManager.installedExtensionsFlow.first()}")
            AnimeSources.init(animeExtensionManager.installedExtensionsFlow)
        }
        val mangaScope = CoroutineScope(Dispatchers.Default)
        mangaScope.launch {
            mangaExtensionManager.findAvailableExtensions()
            logger("Manga Extensions: ${mangaExtensionManager.installedExtensionsFlow.first()}")
            MangaSources.init(mangaExtensionManager.installedExtensionsFlow)
        }
        val novelScope = CoroutineScope(Dispatchers.Default)
        novelScope.launch {
            novelExtensionManager.findAvailableExtensions()
            logger("Novel Extensions: ${novelExtensionManager.installedExtensionsFlow.first()}")
            NovelSources.init(novelExtensionManager.installedExtensionsFlow)
        }

    }


    private fun setupNotificationChannels() {
        try {
            Notifications.createChannels(this)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to modify notification channels" }
        }
    }

    inner class FTActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
        var currentActivity: Activity? = null
        override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
        override fun onActivityStarted(p0: Activity) {
            currentActivity = p0
        }

        override fun onActivityResumed(p0: Activity) {
            currentActivity = p0
        }

        override fun onActivityPaused(p0: Activity) {}
        override fun onActivityStopped(p0: Activity) {}
        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
        override fun onActivityDestroyed(p0: Activity) {}
    }

    companion object {
        private var instance: App? = null
        var context : Context? = null
        fun currentContext(): Context? {
            return instance?.mFTActivityLifecycleCallbacks?.currentActivity ?: context
        }

        fun currentActivity(): Activity? {
            return instance?.mFTActivityLifecycleCallbacks?.currentActivity
        }
    }
}