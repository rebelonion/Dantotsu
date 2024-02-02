package ani.dantotsu.download.video

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import ani.dantotsu.R
import ani.dantotsu.defaultHeaders
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.anime.AnimeDownloaderService
import ani.dantotsu.download.anime.AnimeServiceDataSingleton
import ani.dantotsu.logError
import ani.dantotsu.media.Media
import ani.dantotsu.okHttpClient
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.SubtitleType
import ani.dantotsu.parsers.Video
import ani.dantotsu.parsers.VideoType
import ani.dantotsu.settings.saving.PrefManager
import eu.kanade.tachiyomi.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.IOException
import java.util.concurrent.*

object Helper {

    private var simpleCache: SimpleCache? = null

    @SuppressLint("UnsafeOptInUsageError")
    fun downloadVideo(context: Context, video: Video, subtitle: Subtitle?) {
        val dataSourceFactory = DataSource.Factory {
            val dataSource: HttpDataSource =
                OkHttpDataSource.Factory(okHttpClient).createDataSource()
            defaultHeaders.forEach {
                dataSource.setRequestProperty(it.key, it.value)
            }
            video.file.headers.forEach {
                dataSource.setRequestProperty(it.key, it.value)
            }
            dataSource
        }
        val mimeType = when (video.format) {
            VideoType.M3U8 -> MimeTypes.APPLICATION_M3U8
            VideoType.DASH -> MimeTypes.APPLICATION_MPD
            else -> MimeTypes.APPLICATION_MP4
        }

        val builder = MediaItem.Builder().setUri(video.file.url).setMimeType(mimeType)
        var sub: MediaItem.SubtitleConfiguration? = null
        if (subtitle != null) {
            sub = MediaItem.SubtitleConfiguration
                .Builder(Uri.parse(subtitle.file.url))
                .setSelectionFlags(C.SELECTION_FLAG_FORCED)
                .setMimeType(
                    when (subtitle.type) {
                        SubtitleType.VTT -> MimeTypes.TEXT_VTT
                        SubtitleType.ASS -> MimeTypes.TEXT_SSA
                        SubtitleType.SRT -> MimeTypes.APPLICATION_SUBRIP
                        SubtitleType.UNKNOWN -> MimeTypes.TEXT_SSA
                    }
                )
                .build()
        }
        if (sub != null) builder.setSubtitleConfigurations(mutableListOf(sub))
        val mediaItem = builder.build()
        val downloadHelper = DownloadHelper.forMediaItem(
            context,
            mediaItem,
            DefaultRenderersFactory(context),
            dataSourceFactory
        )
        downloadHelper.prepare(object : DownloadHelper.Callback {
            override fun onPrepared(helper: DownloadHelper) {
                helper.getDownloadRequest(null).let {
                    DownloadService.sendAddDownload(
                        context,
                        ExoplayerDownloadService::class.java,
                        it,
                        false
                    )
                }
            }

            override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                logError(e)
            }
        })
    }


    private var download: DownloadManager? = null
    private const val DOWNLOAD_CONTENT_DIRECTORY = "Anime_Downloads"

    @Synchronized
    @UnstableApi
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
                            Log.e("Downloader", "Download Completed")
                        } else if (download.state == Download.STATE_FAILED) {
                            Log.e("Downloader", "Download Failed")
                        } else if (download.state == Download.STATE_STOPPED) {
                            Log.e("Downloader", "Download Stopped")
                        } else if (download.state == Download.STATE_QUEUED) {
                            Log.e("Downloader", "Download Queued")
                        } else if (download.state == Download.STATE_DOWNLOADING) {
                            Log.e("Downloader", "Download Downloading")
                        }
                    }
                }
            )

            downloadManager
        }
    }

    private var downloadDirectory: File? = null

    @Synchronized
    private fun getDownloadDirectory(context: Context): File {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(null)
            if (downloadDirectory == null) {
                downloadDirectory = context.filesDir
            }
        }
        return downloadDirectory!!
    }

    @OptIn(UnstableApi::class)
    fun startAnimeDownloadService(
        context: Context,
        title: String,
        episode: String,
        video: Video,
        subtitle: Subtitle? = null,
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
            sourceMedia,
            episodeImage
        )

        val downloadsManger = Injekt.get<DownloadsManager>()
        val downloadCheck = downloadsManger
            .queryDownload(title, episode, DownloadedType.Type.ANIME)

        if (downloadCheck) {
            AlertDialog.Builder(context, R.style.MyPopup)
                .setTitle("Download Exists")
                .setMessage("A download for this episode already exists. Do you want to overwrite it?")
                .setPositiveButton("Yes") { _, _ ->
                    DownloadService.sendRemoveDownload(
                        context,
                        ExoplayerDownloadService::class.java,
                        PrefManager.getAnimeDownloadPreferences().getString(
                            animeDownloadTask.getTaskName(),
                            ""
                        ) ?: "",
                        false
                    )
                    PrefManager.getAnimeDownloadPreferences().edit()
                        .remove(animeDownloadTask.getTaskName())
                        .apply()
                    downloadsManger.removeDownload(
                        DownloadedType(
                            title,
                            episode,
                            DownloadedType.Type.ANIME
                        )
                    )
                    AnimeServiceDataSingleton.downloadQueue.offer(animeDownloadTask)
                    if (!AnimeServiceDataSingleton.isServiceRunning) {
                        val intent = Intent(context, AnimeDownloaderService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                        AnimeServiceDataSingleton.isServiceRunning = true
                    }
                }
                .setNegativeButton("No") { _, _ -> }
                .show()
        } else {
            AnimeServiceDataSingleton.downloadQueue.offer(animeDownloadTask)
            if (!AnimeServiceDataSingleton.isServiceRunning) {
                val intent = Intent(context, AnimeDownloaderService::class.java)
                ContextCompat.startForegroundService(context, intent)
                AnimeServiceDataSingleton.isServiceRunning = true
            }
        }
    }

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

    private fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}