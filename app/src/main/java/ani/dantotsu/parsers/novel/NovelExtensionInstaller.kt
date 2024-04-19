package ani.dantotsu.parsers.novel

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.util.storage.getUriCompat
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * The installer which installs, updates and uninstalls the extensions.
 *
 * @param context The application context.
 */
internal class NovelExtensionInstaller(private val context: Context) {

    /**
     * The system's download manager
     */
    private val downloadManager = context.getSystemService<DownloadManager>()!!

    /**
     * The broadcast receiver which listens to download completion events.
     */
    private val downloadReceiver = DownloadCompletionReceiver()

    /**
     * The currently requested downloads, with the package name (unique id) as key, and the id
     * returned by the download manager.
     */
    private val activeDownloads = hashMapOf<String, Long>()

    /**
     * Relay used to notify the installation step of every download.
     */
    private val downloadsRelay = PublishRelay.create<Pair<Long, InstallStep>>()

    /**
     * Adds the given extension to the downloads queue and returns an observable containing its
     * step in the installation process.
     *
     * @param url The url of the apk.
     * @param extension The extension to install.
     */
    fun downloadAndInstall(url: String, extension: NovelExtension): Observable<InstallStep> =
        Observable.defer {
            val pkgName = extension.pkgName

            val oldDownload = activeDownloads[pkgName]
            if (oldDownload != null) {
                deleteDownload(pkgName)
            }

            val sourcePath =
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
            //if the file is already downloaded, remove it
            val fileToDelete = File("$sourcePath/${url.toUri().lastPathSegment}")
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Logger.log("APK file deleted successfully.")
                } else {
                    Logger.log("Failed to delete APK file.")
                }
            } else {
                Logger.log("APK file not found.")
            }

            // Register the receiver after removing (and unregistering) the previous download
            downloadReceiver.register()

            val downloadUri = url.toUri()
            val request = DownloadManager.Request(downloadUri)
                .setTitle(extension.name)
                .setMimeType(APK_MIME)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    downloadUri.lastPathSegment
                )
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val id = downloadManager.enqueue(request)
            activeDownloads[pkgName] = id

            downloadsRelay.filter { it.first == id }
                .map { it.second }
                // Poll download status
                .mergeWith(pollStatus(id))
                // Stop when the application is installed or errors
                .takeUntil { it.isCompleted() }
                // Always notify on main thread
                .observeOn(AndroidSchedulers.mainThread())
                // Always remove the download when unsubscribed
                .doOnUnsubscribe { deleteDownload(pkgName) }
        }

    /**
     * Returns an observable that polls the given download id for its status every second, as the
     * manager doesn't have any notification system. It'll stop once the download finishes.
     *
     * @param id The id of the download to poll.
     */
    private fun pollStatus(id: Long): Observable<InstallStep> {
        val query = DownloadManager.Query().setFilterById(id)

        return Observable.interval(0, 1, TimeUnit.SECONDS)
            // Get the current download status
            .map {
                downloadManager.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    } else {
                        DownloadManager.STATUS_FAILED
                    }
                }
            }
            // Ignore duplicate results
            .distinctUntilChanged()
            // Stop polling when the download fails or finishes
            .takeUntil { it == DownloadManager.STATUS_SUCCESSFUL || it == DownloadManager.STATUS_FAILED }
            // Map to our model
            .flatMap { status ->
                when (status) {
                    DownloadManager.STATUS_PENDING -> Observable.just(InstallStep.Pending)
                    DownloadManager.STATUS_RUNNING -> Observable.just(InstallStep.Downloading)
                    DownloadManager.STATUS_SUCCESSFUL -> Observable.just(InstallStep.Installing)
                    else -> Observable.empty()
                }
            }
    }

    fun installApk(downloadId: Long, uri: Uri, context: Context, pkgName: String): InstallStep {
        val sourcePath =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/" + uri.lastPathSegment
        val destinationPath =
            context.getExternalFilesDir(null)?.absolutePath + "/extensions/novel/$pkgName.apk"

        // Check if source path is obtained correctly
        if (!sourcePath.startsWith(FILE_SCHEME)) {
            Logger.log("Source APK path not found.")
            downloadsRelay.call(downloadId to InstallStep.Error)
            return InstallStep.Error
        }

        // Create the destination directory if it doesn't exist
        val destinationDir = File(destinationPath).parentFile
        if (destinationDir?.exists() == false) {
            destinationDir.mkdirs()
        }
        if (destinationDir?.setWritable(true) == false) {
            Logger.log("Failed to set destinationDir to writable.")
            downloadsRelay.call(downloadId to InstallStep.Error)
            return InstallStep.Error
        }

        // Copy the file to the new location
        copyFileToInternalStorage(sourcePath, destinationPath)
        Logger.log("APK moved to $destinationPath")
        downloadsRelay.call(downloadId to InstallStep.Installed)
        return InstallStep.Installed
    }

    /**
     * Cancels extension install and remove from download manager and installer.
     */
    fun cancelInstall(pkgName: String) {
        val downloadId = activeDownloads.remove(pkgName) ?: return
        downloadManager.remove(downloadId)
    }

    fun uninstallApk(pkgName: String, context: Context) {
        val apkPath =
            context.getExternalFilesDir(null)?.absolutePath + "/extensions/novel/$pkgName.apk"
        val fileToDelete = File(apkPath)
        //give write permission to the file
        if (fileToDelete.exists() && !fileToDelete.canWrite()) {
            Logger.log("File is not writable. Giving write permission.")
            val a = fileToDelete.setWritable(true)
            Logger.log("Success: $a")
        }
        //set the directory to writable
        val destinationDir = File(apkPath).parentFile
        if (destinationDir?.exists() == false) {
            destinationDir.mkdirs()
        }
        val s = destinationDir?.setWritable(true)
        Logger.log("Success destinationDir: $s")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.delete(fileToDelete.toPath())
            } catch (e: Exception) {
                Logger.log("Failed to delete APK file.")
                Logger.log(e)
                snackString("Failed to delete APK file.")
            }
        } else {
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Logger.log("APK file deleted successfully.")
                    snackString("APK file deleted successfully.")
                } else {
                    Logger.log("Failed to delete APK file.")
                    snackString("Failed to delete APK file.")
                }
            } else {
                Logger.log("APK file not found.")
                snackString("APK file not found.")
            }
        }
    }

    private fun copyFileToInternalStorage(sourcePath: String, destinationPath: String) {
        val source = File(sourcePath)
        val destination = File(destinationPath)
        destination.setWritable(true)

        //delete the file if it already exists
        if (destination.exists()) {
            if (destination.delete()) {
                Logger.log("File deleted successfully.")
            } else {
                Logger.log("Failed to delete file.")
            }
        }

        var inputChannel: FileChannel? = null
        var outputChannel: FileChannel? = null
        try {
            inputChannel = FileInputStream(source).channel
            outputChannel = FileOutputStream(destination).channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            destination.setWritable(false)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputChannel?.close()
            outputChannel?.close()
        }

        Logger.log("File copied to internal storage.")
    }

    @Suppress("unused")
    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor != null && cursor.moveToFirst() && columnIndex != null) {
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Sets the step of the installation of an extension.
     *
     * @param downloadId The id of the download.
     * @param step New install step.
     */
    fun updateInstallStep(downloadId: Long, step: InstallStep) {
        downloadsRelay.call(downloadId to step)
    }

    /**
     * Deletes the download for the given package name.
     *
     * @param pkgName The package name of the download to delete.
     */
    private fun deleteDownload(pkgName: String) {
        val downloadId = activeDownloads.remove(pkgName)
        if (downloadId != null) {
            downloadManager.remove(downloadId)
        }
        if (activeDownloads.isEmpty()) {
            downloadReceiver.unregister()
        }
    }

    /**
     * Receiver that listens to download status events.
     */
    private inner class DownloadCompletionReceiver : BroadcastReceiver() {

        /**
         * Whether this receiver is currently registered.
         */
        private var isRegistered = false

        /**
         * Registers this receiver if it's not already.
         */
        fun register() {
            if (isRegistered) return
            isRegistered = true

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_EXPORTED)
        }

        /**
         * Unregisters this receiver if it's not already.
         */
        fun unregister() {
            if (!isRegistered) return
            isRegistered = false

            context.unregisterReceiver(this)
        }

        /**
         * Called when a download event is received. It looks for the download in the current active
         * downloads and notifies its installation step.
         */
        override fun onReceive(context: Context, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) ?: return

            // Avoid events for downloads we didn't request
            if (id !in activeDownloads.values) return

            val uri = downloadManager.getUriForDownloadedFile(id)

            // Set next installation step
            if (uri == null) {
                Logger.log("Couldn't locate downloaded APK")
                downloadsRelay.call(id to InstallStep.Error)
                return
            }

            val query = DownloadManager.Query().setFilterById(id)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val localUri = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI),
                    ).removePrefix(FILE_SCHEME)
                    val pkgName = extractPkgNameFromUri(localUri)
                    installApk(id, File(localUri).getUriCompat(context), context, pkgName)
                }
            }
        }

        private fun extractPkgNameFromUri(localUri: String): String {
            val uri = Uri.parse(localUri)
            val path = uri.path
            val pkgName = path?.substring(path.lastIndexOf('/') + 1)?.removeSuffix(".apk")
            Logger.log("Package name: $pkgName")
            return pkgName ?: ""
        }
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val FILE_SCHEME = "file://"
    }
}
