package ani.dantotsu.download.video

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.scheduler.Requirements
import ani.dantotsu.R
import ani.dantotsu.defaultHeaders
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.anime.AnimeDownloaderService
import ani.dantotsu.download.anime.AnimeServiceDataSingleton
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaType
import ani.dantotsu.parsers.Video
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.util.Logger
import ani.dantotsu.util.customAlertDialog
import eu.kanade.tachiyomi.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
object Helper {
    @OptIn(UnstableApi::class)
    fun startAnimeDownloadService(
        context: Context,
        title: String,
        episode: String,
        video: Video,
        subtitle: List<Pair<String, String>> = emptyList(),
        audio: List<Pair<String, String>> = emptyList(),
        sourceMedia: Media? = null,
        episodeImage: String? = null
    ) {
        if (!isNotificationPermissionGranted(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        val animeDownloadTask = AnimeDownloaderService.AnimeDownloadTask(
            title,
            episode,
            video,
            subtitle,
            audio,
            sourceMedia,
            episodeImage
        )

        val downloadsManager = Injekt.get<DownloadsManager>()
        val downloadCheck = downloadsManager
            .queryDownload(title, episode, MediaType.ANIME)

        if (downloadCheck) {
            context.customAlertDialog().apply {
                setTitle("Download Exists")
                setMessage("A download for this episode already exists. Do you want to overwrite it?")
                setPosButton(R.string.yes) {
                    PrefManager.getAnimeDownloadPreferences().edit()
                        .remove(animeDownloadTask.getTaskName())
                        .apply()
                    downloadsManager.removeDownload(
                        DownloadedType(
                            title,
                            episode,
                            MediaType.ANIME
                        )
                    ) {
                        AnimeServiceDataSingleton.downloadQueue.offer(animeDownloadTask)
                        if (!AnimeServiceDataSingleton.isServiceRunning) {
                            val intent = Intent(context, AnimeDownloaderService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                            AnimeServiceDataSingleton.isServiceRunning = true
                        }
                    }
                }
                setNegButton(R.string.no)
                show()
            }
        } else {
            AnimeServiceDataSingleton.downloadQueue.offer(animeDownloadTask)
            if (!AnimeServiceDataSingleton.isServiceRunning) {
                val intent = Intent(context, AnimeDownloaderService::class.java)
                ContextCompat.startForegroundService(context, intent)
                AnimeServiceDataSingleton.isServiceRunning = true
            }
        }
    }

    private fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    @Synchronized
    @UnstableApi
    @Deprecated("exoplayer download manager is no longer used")
    fun downloadManager(context: Context): DownloadManager {
        return download ?: let {
            val database = Injekt.get<StandaloneDatabaseProvider>()
            val downloadDirectory = File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            val dataSourceFactory = DataSource.Factory {
                //val dataSource: HttpDataSource = OkHttpDataSource.Factory(okHttpClient).createDataSource()
                val networkHelper = Injekt.get<NetworkHelper>()
                val okHttpClient = networkHelper.client
                val dataSource: HttpDataSource =
                    OkHttpDataSource.Factory(okHttpClient).createDataSource()
                defaultHeaders.forEach {
                    dataSource.setRequestProperty(it.key, it.value)
                }
                dataSource
            }
            val threadPoolSize = Runtime.getRuntime().availableProcessors()
            val executorService = Executors.newFixedThreadPool(threadPoolSize)
            val downloadManager = DownloadManager(
                context,
                database,
                getSimpleCache(context),
                dataSourceFactory,
                executorService
            ).apply {
                requirements =
                    Requirements(Requirements.NETWORK or Requirements.DEVICE_STORAGE_NOT_LOW)
                maxParallelDownloads = 3
            }
            downloadManager.addListener(  //for testing
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?
                    ) {
                        if (download.state == Download.STATE_COMPLETED) {
                            Logger.log("Download Completed")
                        } else if (download.state == Download.STATE_FAILED) {
                            Logger.log("Download Failed")
                        } else if (download.state == Download.STATE_STOPPED) {
                            Logger.log("Download Stopped")
                        } else if (download.state == Download.STATE_QUEUED) {
                            Logger.log("Download Queued")
                        } else if (download.state == Download.STATE_DOWNLOADING) {
                            Logger.log("Download Downloading")
                        }
                    }
                }
            )

            downloadManager
        }
    }

    @Deprecated("exoplayer download manager is no longer used")
    @OptIn(UnstableApi::class)
    fun getSimpleCache(context: Context): SimpleCache {
        return if (simpleCache == null) {
            val downloadDirectory = File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            val database = Injekt.get<StandaloneDatabaseProvider>()
            simpleCache = SimpleCache(downloadDirectory, NoOpCacheEvictor(), database)
            simpleCache!!
        } else {
            simpleCache!!
        }
    }

    @Synchronized
    @Deprecated("exoplayer download manager is no longer used")
    private fun getDownloadDirectory(context: Context): File {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(null)
            if (downloadDirectory == null) {
                downloadDirectory = context.filesDir
            }
        }
        return downloadDirectory!!
    }

    @Deprecated("exoplayer download manager is no longer used")
    private var download: DownloadManager? = null

    @Deprecated("exoplayer download manager is no longer used")
    private const val DOWNLOAD_CONTENT_DIRECTORY = "Anime_Downloads"

    @Deprecated("exoplayer download manager is no longer used")
    private var simpleCache: SimpleCache? = null

    @Deprecated("exoplayer download manager is no longer used")
    private var downloadDirectory: File? = null
}