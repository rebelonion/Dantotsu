package ani.dantotsu.download.anime

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import ani.dantotsu.FileUrl
import ani.dantotsu.R
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.currActivity
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.video.ExoplayerDownloadService
import ani.dantotsu.download.video.Helper
import ani.dantotsu.logger
import ani.dantotsu.media.Media
import ani.dantotsu.media.SubtitleDownloader
import ani.dantotsu.media.anime.AnimeWatchFragment
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.Video
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.snackString
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeImpl
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class AnimeDownloaderService : Service() {

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var builder: NotificationCompat.Builder
    private val downloadsManager: DownloadsManager = Injekt.get<DownloadsManager>()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val mutex = Mutex()
    private var isCurrentlyProcessing = false
    private var currentTasks: MutableList<AnimeDownloadTask> = mutableListOf()

    override fun onBind(intent: Intent?): IBinder? {
        // This is only required for bound services.
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        builder =
            NotificationCompat.Builder(this, Notifications.CHANNEL_DOWNLOADER_PROGRESS).apply {
                setContentTitle("Anime Download Progress")
                setSmallIcon(R.drawable.ic_download_24)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setOnlyAlertOnce(true)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
        }
        ContextCompat.registerReceiver(
            this,
            cancelReceiver,
            IntentFilter(ACTION_CANCEL_DOWNLOAD),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        AnimeServiceDataSingleton.downloadQueue.clear()
        downloadJobs.clear()
        AnimeServiceDataSingleton.isServiceRunning = false
        unregisterReceiver(cancelReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        snackString("Download started")
        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        serviceScope.launch {
            mutex.withLock {
                if (!isCurrentlyProcessing) {
                    isCurrentlyProcessing = true
                    processQueue()
                    isCurrentlyProcessing = false
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun processQueue() {
        CoroutineScope(Dispatchers.Default).launch {
            while (AnimeServiceDataSingleton.downloadQueue.isNotEmpty()) {
                val task = AnimeServiceDataSingleton.downloadQueue.poll()
                if (task != null) {
                    val job = launch { download(task) }
                    currentTasks.add(task)
                    mutex.withLock {
                        downloadJobs[task.getTaskName()] = job
                    }
                    job.join() // Wait for the job to complete before continuing to the next task
                    mutex.withLock {
                        downloadJobs.remove(task.getTaskName())
                    }
                    updateNotification() // Update the notification after each task is completed
                }
                if (AnimeServiceDataSingleton.downloadQueue.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        stopSelf() // Stop the service when the queue is empty
                    }
                }
            }
        }
    }

    @UnstableApi
    fun cancelDownload(taskName: String) {
        val url =
            AnimeServiceDataSingleton.downloadQueue.find { it.getTaskName() == taskName }?.video?.file?.url
                ?: currentTasks.find { it.getTaskName() == taskName }?.video?.file?.url ?: ""
        if (url.isEmpty()) {
            snackString("Failed to cancel download")
            return
        }
        currentTasks.removeAll { it.getTaskName() == taskName }
        DownloadService.sendSetStopReason(
            this@AnimeDownloaderService,
            ExoplayerDownloadService::class.java,
            url,
            androidx.media3.exoplayer.offline.Download.STATE_STOPPED,
            false
        )
        DownloadService.sendRemoveDownload(
            this@AnimeDownloaderService,
            ExoplayerDownloadService::class.java,
            url,
            false
        )
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                downloadJobs[taskName]?.cancel()
                downloadJobs.remove(taskName)
                AnimeServiceDataSingleton.downloadQueue.removeAll { it.getTaskName() == taskName }
                updateNotification() // Update the notification after cancellation
            }
        }
    }

    private fun updateNotification() {
        // Update the notification to reflect the current state of the queue
        val pendingDownloads = AnimeServiceDataSingleton.downloadQueue.size
        val text = if (pendingDownloads > 0) {
            "Pending downloads: $pendingDownloads"
        } else {
            "All downloads completed"
        }
        builder.setContentText(text)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    suspend fun download(task: AnimeDownloadTask) {
        try {
            val downloadManager = Helper.downloadManager(this@AnimeDownloaderService)
            withContext(Dispatchers.Main) {
                val notifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        this@AnimeDownloaderService,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                builder.setContentText("Downloading ${task.title} - ${task.episode}")
                if (notifi) {
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }

                currActivity()?.let {
                    Helper.downloadVideo(
                        it,
                        task.video,
                        task.subtitle
                    )
                }

                saveMediaInfo(task)
                task.subtitle?.let {
                    SubtitleDownloader.downloadSubtitle(
                        this@AnimeDownloaderService,
                        it.file.url,
                        DownloadedType(
                            task.title,
                            task.episode,
                            DownloadedType.Type.ANIME,
                        )
                    )
                }
                val downloadStarted =
                    hasDownloadStarted(downloadManager, task, 30000) // 30 seconds timeout

                if (!downloadStarted) {
                    logger("Download failed to start")
                    builder.setContentText("${task.title} - ${task.episode} Download failed to start")
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                    snackString("${task.title} - ${task.episode} Download failed to start")
                    broadcastDownloadFailed(task.episode)
                    return@withContext
                }


                // periodically check if the download is complete
                while (downloadManager.downloadIndex.getDownload(task.video.file.url) != null) {
                    val download = downloadManager.downloadIndex.getDownload(task.video.file.url)
                    if (download != null) {
                        if (download.state == androidx.media3.exoplayer.offline.Download.STATE_FAILED) {
                            logger("Download failed")
                            builder.setContentText("${task.title} - ${task.episode} Download failed")
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                            snackString("${task.title} - ${task.episode} Download failed")
                            logger("Download failed: ${download.failureReason}")
                            downloadsManager.removeDownload(
                                DownloadedType(
                                    task.title,
                                    task.episode,
                                    DownloadedType.Type.ANIME,
                                )
                            )
                            Injekt.get<CrashlyticsInterface>().logException(
                                Exception(
                                    "Anime Download failed:" +
                                            " ${download.failureReason}" +
                                            " url: ${task.video.file.url}" +
                                            " title: ${task.title}" +
                                            " episode: ${task.episode}"
                                )
                            )
                            currentTasks.removeAll { it.getTaskName() == task.getTaskName() }
                            broadcastDownloadFailed(task.episode)
                            break
                        }
                        if (download.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) {
                            logger("Download completed")
                            builder.setContentText("${task.title} - ${task.episode} Download completed")
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                            snackString("${task.title} - ${task.episode} Download completed")
                            PrefManager.getAnimeDownloadPreferences().edit().putString(
                                task.getTaskName(),
                                task.video.file.url
                            ).apply()
                            downloadsManager.addDownload(
                                DownloadedType(
                                    task.title,
                                    task.episode,
                                    DownloadedType.Type.ANIME,
                                )
                            )
                            currentTasks.removeAll { it.getTaskName() == task.getTaskName() }
                            broadcastDownloadFinished(task.episode)
                            break
                        }
                        if (download.state == androidx.media3.exoplayer.offline.Download.STATE_STOPPED) {
                            logger("Download stopped")
                            builder.setContentText("${task.title} - ${task.episode} Download stopped")
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                            snackString("${task.title} - ${task.episode} Download stopped")
                            break
                        }
                        broadcastDownloadProgress(
                            task.episode,
                            download.percentDownloaded.toInt()
                        )
                        if (notifi) {
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                        }
                    }
                    kotlinx.coroutines.delay(2000)
                }
            }
        } catch (e: Exception) {
            if (e.message?.contains("Coroutine was cancelled") == false) {  //wut
                logger("Exception while downloading file: ${e.message}")
                snackString("Exception while downloading file: ${e.message}")
                e.printStackTrace()
                Injekt.get<CrashlyticsInterface>().logException(e)
            }
            broadcastDownloadFailed(task.episode)
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    suspend fun hasDownloadStarted(
        downloadManager: DownloadManager,
        task: AnimeDownloadTask,
        timeout: Long
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            val download = downloadManager.downloadIndex.getDownload(task.video.file.url)
            if (download != null) {
                return true
            }
            // Delay between each poll
            kotlinx.coroutines.delay(500)
        }
        return false
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveMediaInfo(task: AnimeDownloadTask) {
        GlobalScope.launch(Dispatchers.IO) {
            val directory = File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "${DownloadsManager.animeLocation}/${task.title}"
            )
            val episodeDirectory = File(directory, task.episode)
            if (!directory.exists()) directory.mkdirs()
            if (!episodeDirectory.exists()) episodeDirectory.mkdirs()

            val file = File(directory, "media.json")
            val gson = GsonBuilder()
                .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                    SChapterImpl() // Provide an instance of SChapterImpl
                })
                .registerTypeAdapter(SAnime::class.java, InstanceCreator<SAnime> {
                    SAnimeImpl() // Provide an instance of SAnimeImpl
                })
                .registerTypeAdapter(SEpisode::class.java, InstanceCreator<SEpisode> {
                    SEpisodeImpl() // Provide an instance of SEpisodeImpl
                })
                .create()
            val mediaJson = gson.toJson(task.sourceMedia)
            val media = gson.fromJson(mediaJson, Media::class.java)
            if (media != null) {
                media.cover = media.cover?.let { downloadImage(it, directory, "cover.jpg") }
                media.banner = media.banner?.let { downloadImage(it, directory, "banner.jpg") }
                if (task.episodeImage != null) {
                    media.anime?.episodes?.get(task.episode)?.let { episode ->
                        episode.thumb = downloadImage(
                            task.episodeImage,
                            episodeDirectory,
                            "episodeImage.jpg"
                        )?.let {
                            FileUrl(
                                it
                            )
                        }
                    }
                    downloadImage(task.episodeImage, episodeDirectory, "episodeImage.jpg")
                }

                val jsonString = gson.toJson(media)
                withContext(Dispatchers.Main) {
                    file.writeText(jsonString)
                }
            }
        }
    }


    private suspend fun downloadImage(url: String, directory: File, name: String): String? =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            println("Downloading url $url")
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                }

                val file = File(directory, name)
                FileOutputStream(file).use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                return@withContext file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AnimeDownloaderService,
                        "Exception while saving ${name}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                null
            } finally {
                connection?.disconnect()
            }
        }

    private fun broadcastDownloadStarted(episodeNumber: String) {
        val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_STARTED).apply {
            putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, episodeNumber)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadFinished(episodeNumber: String) {
        val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_FINISHED).apply {
            putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, episodeNumber)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadFailed(episodeNumber: String) {
        val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_FAILED).apply {
            putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, episodeNumber)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadProgress(episodeNumber: String, progress: Int) {
        val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_PROGRESS).apply {
            putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, episodeNumber)
            putExtra("progress", progress)
        }
        sendBroadcast(intent)
    }

    private val cancelReceiver = object : BroadcastReceiver() {
        @androidx.annotation.OptIn(UnstableApi::class)
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_CANCEL_DOWNLOAD) {
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME)
                taskName?.let {
                    cancelDownload(it)
                }
            }
        }
    }


    data class AnimeDownloadTask(
        val title: String,
        val episode: String,
        val video: Video,
        val subtitle: Subtitle? = null,
        val sourceMedia: Media? = null,
        val episodeImage: String? = null,
        val retries: Int = 2,
        val simultaneousDownloads: Int = 2,
    ) {
        fun getTaskName(): String {
            return "$title - $episode"
        }

        companion object {
            fun getTaskName(title: String, episode: String): String {
                return "$title - $episode"
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1103
        const val ACTION_CANCEL_DOWNLOAD = "action_cancel_download"
        const val EXTRA_TASK_NAME = "extra_task_name"
    }
}

object AnimeServiceDataSingleton {
    var video: Video? = null
    var sourceMedia: Media? = null
    var downloadQueue: Queue<AnimeDownloaderService.AnimeDownloadTask> = ConcurrentLinkedQueue()

    @Volatile
    var isServiceRunning: Boolean = false
}