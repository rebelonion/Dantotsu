package eu.kanade.tachiyomi.extension.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import ani.dantotsu.media.AddonType
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.Type
import ani.dantotsu.util.Logger
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.installer.Installer
import eu.kanade.tachiyomi.util.storage.getUriCompat
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The installer which installs, updates and uninstalls the extensions.
 *
 * @param context The application context.
 */
class ExtensionInstaller(private val context: Context) {

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

    private val extensionInstaller = Injekt.get<BasePreferences>().extensionInstaller()

    /**
     * Adds the given extension to the downloads queue and returns an observable containing its
     * step in the installation process.
     *
     * @param url The url of the apk.
     * @param extension The extension to install.
     */
    fun <T : Type> downloadAndInstall(
        url: String,
        pkgName: String,
        name: String,
        type: T
    ): Observable<InstallStep> = Observable.defer {
        val oldDownload = activeDownloads[pkgName]
        if (oldDownload != null) {
            deleteDownload(pkgName)
        }

        downloadReceiver.register()

        val downloadUri = url.toUri()
        val request = DownloadManager.Request(downloadUri)
            .setTitle(name)
            .setMimeType(APK_MIME)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                downloadUri.lastPathSegment
            )
            .setDescription(type.asText())
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = downloadManager.enqueue(request)
        activeDownloads[pkgName] = id

        downloadsRelay.filter { it.first == id }
            .map { it.second }
            .mergeWith(pollStatus(id))
            .takeUntil { it.isCompleted() }
            .observeOn(AndroidSchedulers.mainThread())
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
                    cursor.moveToFirst()
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
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
                    else -> Observable.empty()
                }
            }
    }

    /**
     * Starts an intent to install the extension at the given uri.
     *
     * @param uri The uri of the extension to install.
     */
    fun installApk(type: Type, downloadId: Long, uri: Uri) {
        when (val installer = extensionInstaller.get()) {
            BasePreferences.ExtensionInstaller.LEGACY -> {
                val intent = Intent(context, ExtensionInstallActivity::class.java)
                    .setDataAndType(uri, APK_MIME)
                    .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (type is MediaType) {
                    intent.putExtra(EXTRA_EXTENSION_TYPE, type)
                } else if (type is AddonType) {
                    intent.putExtra(EXTRA_ADDON_TYPE, type)
                }

                context.startActivity(intent)
            }

            else -> {
                val intent =
                    ExtensionInstallService.getIntent(context, type, downloadId, uri, installer)
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    /**
     * Cancels extension install and remove from download manager and installer.
     */
    fun cancelInstall(pkgName: String) {
        val downloadId = activeDownloads.remove(pkgName) ?: return
        downloadManager.remove(downloadId)
        Installer.cancelInstallQueue(context, downloadId)
    }

    /**
     * Starts an intent to uninstall the extension by the given package name.
     *
     * @param pkgName The package name of the extension to uninstall
     */
    fun uninstallApk(pkgName: String) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Intent(Intent.ACTION_DELETE).setData("package:$pkgName".toUri())
        else
            @Suppress("DEPRECATION")
            Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:$pkgName".toUri())

        context.startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
                    val type = MediaType.fromText(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION),
                        )
                    ) ?: AddonType.fromText(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION),
                        )
                    ) ?: return

                    installApk(type, id, File(localUri).getUriCompat(context))
                }
            }
        }
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val EXTRA_DOWNLOAD_ID = "ExtensionInstaller.extra.DOWNLOAD_ID"
        const val EXTRA_EXTENSION_TYPE = "ExtensionInstaller.extra.EXTENSION_TYPE"
        const val EXTRA_ADDON_TYPE = "ExtensionInstaller.extra.ADDON_TYPE"
        const val FILE_SCHEME = "file://"
    }
}
