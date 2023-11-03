package ani.dantotsu.download.manga

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ani.dantotsu.R
import ani.dantotsu.download.Download
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.media.Media
import ani.dantotsu.media.manga.ImageData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileOutputStream
import com.google.gson.Gson
import eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_DOWNLOADER_PROGRESS
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.ContextCompat
import ani.dantotsu.media.manga.MangaReadFragment.Companion.ACTION_DOWNLOAD_FINISHED
import ani.dantotsu.media.manga.MangaReadFragment.Companion.ACTION_DOWNLOAD_STARTED
import ani.dantotsu.media.manga.MangaReadFragment.Companion.EXTRA_CHAPTER_NUMBER
import ani.dantotsu.snackString
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.*

class MangaDownloaderService : Service() {

    private var title: String = ""
    private var chapter: String = ""
    private var retries: Int = 2
    private var simultaneousDownloads: Int = 2
    private var imageData: List<ImageData> = listOf()
    private var sourceMedia: Media? = null
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var builder: NotificationCompat.Builder
    private val downloadsManager: DownloadsManager = Injekt.get<DownloadsManager>()

    override fun onBind(intent: Intent?): IBinder? {
        // This is only required for bound services.
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        builder = NotificationCompat.Builder(this, CHANNEL_DOWNLOADER_PROGRESS).apply {
            setContentTitle("Manga Download Progress")
            setContentText("Downloading $title - $chapter")
            setSmallIcon(R.drawable.ic_round_download_24)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setOnlyAlertOnce(true)
            setProgress(0, 0, false)
        }
        startForeground(NOTIFICATION_ID, builder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        snackString("Download started")
        title = intent?.getStringExtra("title") ?: ""
        chapter = intent?.getStringExtra("chapter") ?: ""
        retries = intent?.getIntExtra("retries", 2) ?: 2
        simultaneousDownloads = intent?.getIntExtra("simultaneousDownloads", 2) ?: 2
        imageData = ServiceDataSingleton.imageData
        sourceMedia = ServiceDataSingleton.sourceMedia
        ServiceDataSingleton.imageData = listOf()
        ServiceDataSingleton.sourceMedia = null

        CoroutineScope(Dispatchers.Default).launch {
            download()
        }

        return START_NOT_STICKY
    }

    suspend fun download() {
        withContext(Dispatchers.Main) {
            if (ContextCompat.checkSelfPermission(
                    this@MangaDownloaderService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    this@MangaDownloaderService,
                    "Please grant notification permission",
                    Toast.LENGTH_SHORT
                ).show()
                return@withContext
            }
            notificationManager.notify(NOTIFICATION_ID, builder.build())

            val deferredList = mutableListOf<Deferred<Bitmap?>>()

            // Loop through each ImageData object
            var farthest = 0
            for ((index, image) in imageData.withIndex()) {
                // Limit the number of simultaneous downloads
                if (deferredList.size >= simultaneousDownloads) {
                    // Wait for all deferred to complete and clear the list
                    deferredList.awaitAll()
                    deferredList.clear()
                }

                // Download the image and add to deferred list
                val deferred = async(Dispatchers.IO) {
                    var bitmap: Bitmap? = null
                    var retryCount = 0

                    while (bitmap == null && retryCount < retries) {
                        bitmap = imageData[index].fetchAndProcessImage(
                            imageData[index].page,
                            imageData[index].source,
                            this@MangaDownloaderService
                        )
                        retryCount++
                    }

                    // Cache the image if successful
                    if (bitmap != null) {
                        saveToDisk("$index.jpg", bitmap)
                    }
                    farthest++
                    builder.setProgress(imageData.size, farthest + 1, false)
                    notificationManager.notify(NOTIFICATION_ID, builder.build())

                    bitmap
                }

                deferredList.add(deferred)
            }

            // Wait for any remaining deferred to complete
            deferredList.awaitAll()

            builder.setContentText("Download complete")
                .setProgress(0, 0, false)
            notificationManager.notify(NOTIFICATION_ID, builder.build())

            saveMediaInfo()
            downloadsManager.addDownload(Download(title, chapter, Download.Type.MANGA))
            downloadsManager.exportDownloads(Download(title, chapter, Download.Type.MANGA))
            broadcastDownloadFinished(chapter)
            snackString("Download finished")
            stopSelf()

        }
    }

    fun saveToDisk(fileName: String, bitmap: Bitmap) {
        try {
            // Define the directory within the private external storage space
            val directory = File(
                this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Manga/$title/$chapter"
            )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Create a file reference within that directory for your image
            val file = File(directory, fileName)

            // Use a FileOutputStream to write the bitmap to the file
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }


        } catch (e: Exception) {
            println("Exception while saving image: ${e.message}")
            Toast.makeText(this, "Exception while saving image: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    fun saveMediaInfo() {
        GlobalScope.launch(Dispatchers.IO) {
            val directory = File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Manga/$title/$chapter"
            )
            if (!directory.exists()) directory.mkdirs()

            val file = File(directory, "media.json")
            val gson = GsonBuilder()
                .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                    SChapterImpl() // Provide an instance of SChapterImpl
                })
                .create()
            val mediaJson = gson.toJson(sourceMedia)  //need a deep copy of sourceMedia
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

    suspend fun downloadImage(url: String, directory: File, name: String): String? = withContext(Dispatchers.IO) {
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
                Toast.makeText(this@MangaDownloaderService, "Exception while saving ${name}: ${e.message}", Toast.LENGTH_LONG).show()
            }
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun broadcastDownloadStarted(chapterNumber: String) {
        val intent = Intent(ACTION_DOWNLOAD_STARTED).apply {
            putExtra(EXTRA_CHAPTER_NUMBER, chapterNumber)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDownloadFinished(chapterNumber: String) {
        val intent = Intent(ACTION_DOWNLOAD_FINISHED).apply {
            putExtra(EXTRA_CHAPTER_NUMBER, chapterNumber)
        }
        sendBroadcast(intent)
    }

    companion object {
        private const val NOTIFICATION_ID = 1103
    }
}

object ServiceDataSingleton {
    var imageData: List<ImageData> = listOf()
    var sourceMedia: Media? = null
}