package ani.dantotsu.download.novel

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
import ani.dantotsu.R
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.DownloadsManager.Companion.getSubDirectory
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.novel.NovelReadFragment
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import com.anggrayudi.storage.file.forceDelete
import com.anggrayudi.storage.file.openOutputStream
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.buffer
import okio.sink
import tachiyomi.core.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class NovelDownloaderService : Service() {

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var builder: NotificationCompat.Builder
    private val downloadsManager: DownloadsManager = Injekt.get<DownloadsManager>()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val mutex = Mutex()
    private var isCurrentlyProcessing = false

    private val networkHelper = Injekt.get<NetworkHelper>()

    override fun onBind(intent: Intent?): IBinder? {
        // This is only required for bound services.
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        builder =
            NotificationCompat.Builder(this, Notifications.CHANNEL_DOWNLOADER_PROGRESS).apply {
                setContentTitle("Novel Download Progress")
                setSmallIcon(R.drawable.ic_download_24)
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
        NovelServiceDataSingleton.downloadQueue.clear()
        downloadJobs.clear()
        NovelServiceDataSingleton.isServiceRunning = false
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
            while (NovelServiceDataSingleton.downloadQueue.isNotEmpty()) {
                val task = NovelServiceDataSingleton.downloadQueue.poll()
                if (task != null) {
                    val job = launch { download(task) }
                    mutex.withLock {
                        downloadJobs[task.chapter] = job
                    }
                    job.join() // Wait for the job to complete before continuing to the next task
                    mutex.withLock {
                        downloadJobs.remove(task.chapter)
                    }
                    updateNotification() // Update the notification after each task is completed
                }
                if (NovelServiceDataSingleton.downloadQueue.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        stopSelf() // Stop the service when the queue is empty
                    }
                }
            }
        }
    }

    fun cancelDownload(chapter: String) {
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                downloadJobs[chapter]?.cancel()
                downloadJobs.remove(chapter)
                NovelServiceDataSingleton.downloadQueue.removeAll { it.chapter == chapter }
                updateNotification() // Update the notification after cancellation
            }
        }
    }

    private fun updateNotification() {
        // Update the notification to reflect the current state of the queue
        val pendingDownloads = NovelServiceDataSingleton.downloadQueue.size
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

    private suspend fun isEpubFile(urlString: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(urlString)
                    .head()
                    .build()

                networkHelper.client.newCall(request).execute().use { response ->
                    val contentType = response.header("Content-Type")
                    val contentDisposition = response.header("Content-Disposition")

                    Logger.log("Content-Type: $contentType")
                    Logger.log("Content-Disposition: $contentDisposition")

                    // Return true if the Content-Type or Content-Disposition indicates an EPUB file
                    contentType == "application/epub+zip" ||
                            (contentDisposition?.contains(".epub") == true)
                }
            } catch (e: Exception) {
                Logger.log("Error checking file type: ${e.message}")
                false
            }
        }
    }

    private fun isAlreadyDownloaded(urlString: String): Boolean {
        return urlString.contains("file://")
    }

    suspend fun download(task: DownloadTask) {
        try {
            withContext(Dispatchers.Main) {
                val notifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        this@NovelDownloaderService,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                broadcastDownloadStarted(task.originalLink)

                if (notifi) {
                    builder.setContentText("Downloading ${task.title} - ${task.chapter}")
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }

                if (!isEpubFile(task.downloadLink)) {
                    if (isAlreadyDownloaded(task.originalLink)) {
                        Logger.log("Already downloaded")
                        broadcastDownloadFinished(task.originalLink)
                        snackString("Already downloaded")
                        return@withContext
                    }
                    Logger.log("Download link is not an .epub file")
                    broadcastDownloadFailed(task.originalLink)
                    snackString("Download link is not an .epub file")
                    return@withContext
                }

                val baseDirectory = getSubDirectory(
                    this@NovelDownloaderService,
                    MediaType.NOVEL,
                    false,
                    task.title
                ) ?: throw Exception("Directory not found")

                // Start the download
                withContext(Dispatchers.IO) {
                    try {
                        val request = Request.Builder()
                            .url(task.downloadLink)
                            .build()

                        networkHelper.downloadClient.newCall(request).execute().use { response ->
                            // Ensure the response is successful and has a body
                            if (!response.isSuccessful) {
                                throw IOException("Failed to download file: ${response.message}")
                            }
                            val directory = getSubDirectory(
                                this@NovelDownloaderService,
                                MediaType.NOVEL,
                                false,
                                task.title,
                                task.chapter
                            ) ?: throw Exception("Directory not found")
                            directory.findFile("0.epub")?.forceDelete(this@NovelDownloaderService)

                            val file = directory.createFile("application/epub+zip", "0.epub")
                                ?: throw Exception("File not created")

                            //download cover
                            task.coverUrl?.let {
                                file.parentFile?.let { it1 -> downloadImage(it, it1, "cover.jpg") }
                            }
                            val outputStream =
                                this@NovelDownloaderService.contentResolver.openOutputStream(file.uri)
                                    ?: throw Exception("Could not open OutputStream")

                            val sink = outputStream.sink().buffer()
                            val responseBody = response.body
                            val totalBytes = responseBody.contentLength()
                            var downloadedBytes = 0L

                            val notificationUpdateInterval = 1024 * 1024 // 1 MB
                            val broadcastUpdateInterval = 1024 * 256 // 256 KB
                            var lastNotificationUpdate = 0L
                            var lastBroadcastUpdate = 0L

                            responseBody.source().use { source ->
                                while (true) {
                                    val read = source.read(sink.buffer, 8192)
                                    if (read == -1L) break
                                    downloadedBytes += read
                                    sink.emit()

                                    // Update progress at intervals
                                    if (downloadedBytes - lastNotificationUpdate >= notificationUpdateInterval) {
                                        withContext(Dispatchers.Main) {
                                            val progress =
                                                (downloadedBytes * 100 / totalBytes).toInt()
                                            builder.setProgress(100, progress, false)
                                            if (notifi) {
                                                notificationManager.notify(
                                                    NOTIFICATION_ID,
                                                    builder.build()
                                                )
                                            }
                                        }
                                        lastNotificationUpdate = downloadedBytes
                                    }
                                    if (downloadedBytes - lastBroadcastUpdate >= broadcastUpdateInterval) {
                                        withContext(Dispatchers.Main) {
                                            val progress =
                                                (downloadedBytes * 100 / totalBytes).toInt()
                                            Logger.log("Download progress: $progress")
                                            broadcastDownloadProgress(task.originalLink, progress)
                                        }
                                        lastBroadcastUpdate = downloadedBytes
                                    }
                                }
                            }

                            sink.close()
                            //if the file is smaller than 95% of totalBytes, it means the download was interrupted
                            if (file.length() < totalBytes * 0.95) {
                                throw IOException("Failed to download file: ${response.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Logger.log("Exception while downloading .epub inside request: ${e.message}")
                        throw e
                    }
                }

                // Update notification for download completion
                builder.setContentText("${task.title} - ${task.chapter} Download complete")
                    .setProgress(0, 0, false)
                if (notifi) {
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }

                saveMediaInfo(task, baseDirectory)
                downloadsManager.addDownload(
                    DownloadedType(
                        task.title,
                        task.chapter,
                        MediaType.NOVEL
                    )
                )
                broadcastDownloadFinished(task.originalLink)
                snackString("${task.title} - ${task.chapter} Download finished")
            }
        } catch (e: Exception) {
            Logger.log("Exception while downloading .epub: ${e.message}")
            snackString("Exception while downloading .epub: ${e.message}")
            Injekt.get<CrashlyticsInterface>().logException(e)
            broadcastDownloadFailed(task.originalLink)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveMediaInfo(task: DownloadTask, directory: DocumentFile) {
        launchIO {
            directory.findFile("media.json")?.forceDelete(this@NovelDownloaderService)
            val file = directory.createFile("application/json", "media.json")
                ?: throw Exception("File not created")
            val gson = GsonBuilder()
                .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                    SChapterImpl() // Provide an instance of SChapterImpl
                })
                .create()
            val mediaJson = gson.toJson(task.sourceMedia)
            val media = gson.fromJson(mediaJson, Media::class.java)
            if (media != null) {
                media.cover = media.cover?.let { downloadImage(it, directory, "cover.jpg") }
                media.banner = media.banner?.let { downloadImage(it, directory, "banner.jpg") }

                val jsonString = gson.toJson(media)
                withContext(Dispatchers.Main) {
                    try {
                        file.openOutputStream(this@NovelDownloaderService, false).use { output ->
                            if (output == null) throw Exception("Output stream is null")
                            output.write(jsonString.toByteArray())
                        }
                    } catch (e: android.system.ErrnoException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@NovelDownloaderService,
                            "Error while saving: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }


    private suspend fun downloadImage(url: String, directory: DocumentFile, name: String): String? =
        withContext(
            Dispatchers.IO
        ) {
            var connection: HttpURLConnection? = null
            Logger.log("Downloading url $url")
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                }
                directory.findFile(name)?.forceDelete(this@NovelDownloaderService)
                val file =
                    directory.createFile("image/jpeg", name) ?: throw Exception("File not created")
                file.openOutputStream(this@NovelDownloaderService, false).use { output ->
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
                        this@NovelDownloaderService,
                        "Exception while saving ${name}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                null
            } finally {
                connection?.disconnect()
            }
        }

    private fun broadcastDownloadStarted(link: String) {
        val intent = Intent(NovelReadFragment.ACTION_DOWNLOAD_STARTED).apply {
            putExtra(NovelReadFragment.EXTRA_NOVEL_LINK, link)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadFinished(link: String) {
        val intent = Intent(NovelReadFragment.ACTION_DOWNLOAD_FINISHED).apply {
            putExtra(NovelReadFragment.EXTRA_NOVEL_LINK, link)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadFailed(link: String) {
        val intent = Intent(NovelReadFragment.ACTION_DOWNLOAD_FAILED).apply {
            putExtra(NovelReadFragment.EXTRA_NOVEL_LINK, link)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadProgress(link: String, progress: Int) {
        val intent = Intent(NovelReadFragment.ACTION_DOWNLOAD_PROGRESS).apply {
            putExtra(NovelReadFragment.EXTRA_NOVEL_LINK, link)
            putExtra("progress", progress)
        }
        sendBroadcast(intent)
    }

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_CANCEL_DOWNLOAD) {
                val chapter = intent.getStringExtra(EXTRA_CHAPTER)
                chapter?.let {
                    cancelDownload(it)
                }
            }
        }
    }


    data class DownloadTask(
        val title: String,
        val chapter: String,
        val downloadLink: String,
        val originalLink: String,
        val sourceMedia: Media? = null,
        val coverUrl: String? = null,
        val retries: Int = 2,
    )

    companion object {
        private const val NOTIFICATION_ID = 1103
        const val ACTION_CANCEL_DOWNLOAD = "action_cancel_download"
        const val EXTRA_CHAPTER = "extra_chapter"
    }
}

object NovelServiceDataSingleton {
    var downloadQueue: Queue<NovelDownloaderService.DownloadTask> = ConcurrentLinkedQueue()

    @Volatile
    var isServiceRunning: Boolean = false
}