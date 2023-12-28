package ani.dantotsu.download.anime

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import ani.dantotsu.R
import ani.dantotsu.currActivity
import ani.dantotsu.download.Download
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.anime.AnimeDownloaderService
import ani.dantotsu.download.anime.AnimeServiceDataSingleton
import ani.dantotsu.download.video.Helper
import ani.dantotsu.download.video.MyDownloadService
import ani.dantotsu.logger
import ani.dantotsu.media.Media
import ani.dantotsu.media.anime.AnimeWatchFragment
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.Video
import ani.dantotsu.snackString
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    override fun onBind(intent: Intent?): IBinder? {
        // This is only required for bound services.
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        builder = NotificationCompat.Builder(this, Notifications.CHANNEL_DOWNLOADER_PROGRESS).apply {
            setContentTitle("Anime Download Progress")
            setSmallIcon(R.drawable.ic_round_download_24)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setOnlyAlertOnce(true)
            setProgress(0, 0, false)
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
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                val url = AnimeServiceDataSingleton.downloadQueue.find { it.getTaskName() == taskName }?.video?.file?.url ?: ""
                DownloadService.sendRemoveDownload(
                    this@AnimeDownloaderService,
                    MyDownloadService::class.java,
                    url,
                    false
                )
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

    @androidx.annotation.OptIn(UnstableApi::class) suspend fun download(task: DownloadTask) {
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

                broadcastDownloadStarted(task.getTaskName())

                currActivity()?.let {
                    Helper.downloadVideo(
                        it,
                        task.video,
                        task.subtitle)
                }

                saveMediaInfo(task)
                downloadsManager.addDownload(
                    Download(
                        task.title,
                        task.episode,
                        Download.Type.ANIME,
                    )
                )

                // periodically check if the download is complete
                while (downloadManager.downloadIndex.getDownload(task.video.file.url) != null) {
                    val download = downloadManager.downloadIndex.getDownload(task.video.file.url)
                    if (download != null) {
                        if (download.state == androidx.media3.exoplayer.offline.Download.STATE_FAILED) {
                            logger("Download failed")
                            builder.setContentText("${task.title} - ${task.episode} Download failed")
                                .setProgress(0, 0, false)
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                            snackString("${task.title} - ${task.episode} Download failed")
                            broadcastDownloadFailed(task.getTaskName())
                            break
                        }
                        if (download.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) {
                            logger("Download completed")
                            builder.setContentText("${task.title} - ${task.episode} Download completed")
                                .setProgress(0, 0, false)
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                            snackString("${task.title} - ${task.episode} Download completed")
                            getSharedPreferences(getString(R.string.anime_downloads), Context.MODE_PRIVATE).edit().putString(
                                task.getTaskName(),
                                task.video.file.url
                            ).apply()
                            broadcastDownloadFinished(task.getTaskName())
                            break
                        }
                        if (download.state == androidx.media3.exoplayer.offline.Download.STATE_STOPPED) {
                            logger("Download stopped")
                            builder.setContentText("${task.title} - ${task.episode} Download stopped")
                                .setProgress(0, 0, false)
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                            snackString("${task.title} - ${task.episode} Download stopped")
                            break
                        }
                        broadcastDownloadProgress(task.getTaskName(), download.percentDownloaded.toInt())
                        builder.setProgress(100, download.percentDownloaded.toInt(), false)
                        if (notifi) {
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                        }
                    }
                    kotlinx.coroutines.delay(2000)
                }
            }
        } catch (e: Exception) {
            logger("Exception while downloading file: ${e.message}")
            snackString("Exception while downloading file: ${e.message}")
            FirebaseCrashlytics.getInstance().recordException(e)
            broadcastDownloadFailed(task.getTaskName())
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveMediaInfo(task: DownloadTask) {
        GlobalScope.launch(Dispatchers.IO) {
            val directory = File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Anime/${task.title}"
            )
            if (!directory.exists()) directory.mkdirs()

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

    private fun broadcastDownloadStarted(chapterNumber: String) {
        val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_STARTED).apply {
            putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, chapterNumber)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadFinished(chapterNumber: String) {
        val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_FINISHED).apply {
            putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, chapterNumber)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadFailed(chapterNumber: String) {
        val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_FAILED).apply {
            putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, chapterNumber)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadProgress(chapterNumber: String, progress: Int) {
        val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_PROGRESS).apply {
            putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, chapterNumber)
            putExtra("progress", progress)
        }
        sendBroadcast(intent)
    }

    private val cancelReceiver = object : BroadcastReceiver() {
        @androidx.annotation.OptIn(UnstableApi::class) override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_CANCEL_DOWNLOAD) {
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME)
                taskName?.let {
                    cancelDownload(it)
                }
            }
        }
    }


    data class DownloadTask(
        val title: String,
        val episode: String,
        val video: Video,
        val subtitle: Subtitle? = null,
        val sourceMedia: Media? = null,
        val retries: Int = 2,
        val simultaneousDownloads: Int = 2,
    ) {
        fun getTaskName(): String {
            return "$title - $episode"
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
    var downloadQueue: Queue<AnimeDownloaderService.DownloadTask> = ConcurrentLinkedQueue()

    @Volatile
    var isServiceRunning: Boolean = false
}