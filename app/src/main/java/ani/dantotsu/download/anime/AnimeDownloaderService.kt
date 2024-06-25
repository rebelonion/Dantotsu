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
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.FileUrl
import ani.dantotsu.R
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.defaultHeaders
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.DownloadsManager.Companion.getSubDirectory
import ani.dantotsu.download.anime.AnimeDownloaderService.AnimeDownloadTask.Companion.getTaskName
import ani.dantotsu.download.findValidName
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.anime.AnimeWatchFragment
import ani.dantotsu.parsers.Video
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.snackString
import ani.dantotsu.toast
import ani.dantotsu.util.Logger
import com.anggrayudi.storage.file.forceDelete
import com.anggrayudi.storage.file.openOutputStream
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
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
    private val ffExtension = Injekt.get<DownloadAddonManager>().extension?.extension

    override fun onBind(intent: Intent?): IBinder? {
        // This is only required for bound services.
        return null
    }

    override fun onCreate() {
        super.onCreate()
        if (ffExtension == null) {
            toast(getString(R.string.download_addon_not_found))
            stopSelf()
            return
        }
        notificationManager = NotificationManagerCompat.from(this)
        builder =
            NotificationCompat.Builder(this, Notifications.CHANNEL_DOWNLOADER_PROGRESS).apply {
                setContentTitle("Anime Download Progress")
                setSmallIcon(R.drawable.ic_download_24)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setOnlyAlertOnce(true)
                setProgress(100, 0, false)
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
        val sessionIds =
            AnimeServiceDataSingleton.downloadQueue.filter { it.getTaskName() == taskName }
                .map { it.sessionId }.toMutableList()
        sessionIds.addAll(currentTasks.filter { it.getTaskName() == taskName }.map { it.sessionId })
        sessionIds.forEach {
            ffExtension!!.cancelDownload(it)
        }
        currentTasks.removeAll { it.getTaskName() == taskName }
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
        withContext(Dispatchers.IO) {
            try {
                val notifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        this@AnimeDownloaderService,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                builder.setContentText("Downloading ${getTaskName(task.title, task.episode)}")
                if (notifi) {
                    withContext(Dispatchers.Main) {
                        notificationManager.notify(NOTIFICATION_ID, builder.build())
                    }
                }

                val baseOutputDir = getSubDirectory(
                    this@AnimeDownloaderService,
                    MediaType.ANIME,
                    false,
                    task.title
                ) ?: throw Exception("Failed to create output directory")
                val outputDir = getSubDirectory(
                    this@AnimeDownloaderService,
                    MediaType.ANIME,
                    true,
                    task.title,
                    task.episode
                ) ?: throw Exception("Failed to create output directory")

                val extension = ffExtension!!.getFileExtension()
                outputDir.findFile("${task.getTaskName().findValidName()}.${extension.first}")
                    ?.delete()

                val outputFile =
                    outputDir.createFile(
                        extension.second,
                        "${task.getTaskName()}.${extension.first}"
                    )
                        ?: throw Exception("Failed to create output file")

                var percent = 0
                var totalLength = 0.0
                val path = ffExtension.setDownloadPath(
                    this@AnimeDownloaderService,
                    outputFile.uri
                )
                if (!task.video.file.headers.containsKey("User-Agent")
                    && !task.video.file.headers.containsKey("user-agent")
                ) {
                    val newHeaders = task.video.file.headers.toMutableMap()
                    newHeaders["User-Agent"] = defaultHeaders["User-Agent"]!!
                    task.video.file.headers = newHeaders
                }

                ffExtension.executeFFProbe(
                    task.video.file.url,
                    task.video.file.headers
                ) {
                    if (it.toDoubleOrNull() != null) {
                        totalLength = it.toDouble()
                    }
                }
                val ffTask =
                    ffExtension.executeFFMpeg(
                        task.video.file.url,
                        path,
                        task.video.file.headers,
                        task.subtitle,
                        task.audio,
                    ) {
                        // CALLED WHEN SESSION GENERATES STATISTICS
                        val timeInMilliseconds = it
                        if (timeInMilliseconds > 0 && totalLength > 0) {
                            percent = ((it / 1000) / totalLength * 100).toInt()
                        }
                        Logger.log("Statistics: $it")
                    }
                task.sessionId = ffTask
                currentTasks.find { it.getTaskName() == task.getTaskName() }?.sessionId =
                    ffTask

                saveMediaInfo(task, baseOutputDir)

                // periodically check if the download is complete
                while (ffExtension.getState(ffTask) != "COMPLETED") {
                    if (ffExtension.getState(ffTask) == "FAILED") {
                        Logger.log("Download failed")
                        builder.setContentText(
                            "${
                                getTaskName(
                                    task.title,
                                    task.episode
                                )
                            } Download failed"
                        )
                        if (notifi) {
                            withContext(Dispatchers.Main) {
                                notificationManager.notify(NOTIFICATION_ID, builder.build())
                            }
                        }
                        toast("${getTaskName(task.title, task.episode)} Download failed")
                        Logger.log("Download failed: ${ffExtension.getStackTrace(ffTask)}")
                        downloadsManager.removeDownload(
                            DownloadedType(
                                task.title,
                                task.episode,
                                MediaType.ANIME,
                            ),
                            false
                        ) {}
                        Injekt.get<CrashlyticsInterface>().logException(
                            Exception(
                                "Anime Download failed:" +
                                        " ${getTaskName(task.title, task.episode)}" +
                                        " url: ${task.video.file.url}" +
                                        " title: ${task.title}" +
                                        " episode: ${task.episode}"
                            )
                        )
                        currentTasks.removeAll { it.getTaskName() == task.getTaskName() }
                        broadcastDownloadFailed(task.episode)
                        break
                    }
                    builder.setProgress(
                        100, percent.coerceAtMost(99),
                        false
                    )
                    broadcastDownloadProgress(
                        task.episode,
                        percent.coerceAtMost(99)
                    )
                    if (notifi) {
                        withContext(Dispatchers.Main) {
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                        }
                    }
                    kotlinx.coroutines.delay(2000)
                }
                if (ffExtension.getState(ffTask) == "COMPLETED") {
                    if (ffExtension.hadError(ffTask)) {
                        Logger.log("Download failed")
                        builder.setContentText(
                            "${
                                getTaskName(
                                    task.title,
                                    task.episode
                                )
                            } Download failed"
                        )
                        if (notifi) {
                            withContext(Dispatchers.Main) {
                                notificationManager.notify(NOTIFICATION_ID, builder.build())
                            }
                        }
                        snackString("${getTaskName(task.title, task.episode)} Download failed")
                        downloadsManager.removeDownload(
                            DownloadedType(
                                task.title,
                                task.episode,
                                MediaType.ANIME
                            ),
                            false
                        ) {}
                        Injekt.get<CrashlyticsInterface>().logException(
                            Exception(
                                "Anime Download failed:" +
                                        " ${getTaskName(task.title, task.episode)}" +
                                        " url: ${task.video.file.url}" +
                                        " title: ${task.title}" +
                                        " episode: ${task.episode}"
                            )
                        )
                        currentTasks.removeAll { it.getTaskName() == task.getTaskName() }
                        broadcastDownloadFailed(task.episode)
                        return@withContext
                    }
                    Logger.log("Download completed")
                    builder.setContentText(
                        "${
                            getTaskName(
                                task.title,
                                task.episode
                            )
                        } Download completed"
                    )
                    if (notifi) {
                        withContext(Dispatchers.Main) {
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                        }
                    }
                    snackString("${getTaskName(task.title, task.episode)} Download completed")
                    PrefManager.getAnimeDownloadPreferences().edit().putString(
                        task.getTaskName(),
                        task.video.file.url
                    ).apply()
                    downloadsManager.addDownload(
                        DownloadedType(
                            task.title,
                            task.episode,
                            MediaType.ANIME,
                        )
                    )

                    currentTasks.removeAll { it.getTaskName() == task.getTaskName() }
                    broadcastDownloadFinished(task.episode)
                } else throw Exception("Download failed")

            } catch (e: Exception) {
                if (e.message?.contains("Coroutine was cancelled") == false) {  //wut
                    Logger.log("Exception while downloading file: ${e.message}")
                    snackString("Exception while downloading file: ${e.message}")
                    e.printStackTrace()
                    Injekt.get<CrashlyticsInterface>().logException(e)
                }
                broadcastDownloadFailed(task.episode)
            }
        }
    }

    private fun saveMediaInfo(task: AnimeDownloadTask, directory: DocumentFile) {
        CoroutineScope(Dispatchers.IO).launch {
            directory.findFile("media.json")?.forceDelete(this@AnimeDownloaderService)
            val file = directory.createFile("application/json", "media.json")
                ?: throw Exception("File not created")
            val episodeDirectory =
                getSubDirectory(
                    this@AnimeDownloaderService,
                    MediaType.ANIME,
                    false,
                    task.title,
                    task.episode
                )
                    ?: throw Exception("Directory not found")

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
                    try {
                        file.openOutputStream(this@AnimeDownloaderService, false).use { output ->
                            if (output == null) throw Exception("Output stream is null")
                            output.write(jsonString.toByteArray())
                        }
                    } catch (e: android.system.ErrnoException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@AnimeDownloaderService,
                            "Error while saving: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private suspend fun downloadImage(url: String, directory: DocumentFile, name: String): String? =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            println("Downloading url $url")
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                }

                directory.findFile(name)?.forceDelete(this@AnimeDownloaderService)
                val file =
                    directory.createFile("image/jpeg", name) ?: throw Exception("File not created")
                file.openOutputStream(this@AnimeDownloaderService, false).use { output ->
                    if (output == null) throw Exception("Output stream is null")
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                return@withContext file.uri.toString()
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
        val subtitle: List<Pair<String, String>> = emptyList(),
        val audio: List<Pair<String, String>> = emptyList(),
        val sourceMedia: Media? = null,
        val episodeImage: String? = null,
        val retries: Int = 2,
        val simultaneousDownloads: Int = 2,
        var sessionId: Long = -1
    ) {
        fun getTaskName(): String {
            return "${title.replace("/", "")}/${episode.replace("/", "")}"
        }

        companion object {
            fun getTaskName(title: String, episode: String): String {
                return "${title.replace("/", "")}/${episode.replace("/", "")}"
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
    var downloadQueue: Queue<AnimeDownloaderService.AnimeDownloadTask> = ConcurrentLinkedQueue()

    @Volatile
    var isServiceRunning: Boolean = false
}