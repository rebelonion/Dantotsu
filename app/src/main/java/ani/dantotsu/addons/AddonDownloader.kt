package ani.dantotsu.addons

import android.annotation.SuppressLint
import android.app.Activity
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
import ani.dantotsu.BuildConfig
import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.logError
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.toast
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

class AddonDownloader {
    companion object {
        private suspend fun check(repo: String): Pair<String, String> {
            return try {
                val res = client.get("https://api.github.com/repos/$repo/releases")
                    .parsed<JsonArray>().map {
                        Mapper.json.decodeFromJsonElement<AppUpdater.GithubResponse>(it)
                    }
                val r = res.maxByOrNull {
                        it.timeStamp()
                    } ?: throw Exception("No Pre Release Found")
                val v = r.tagName.substringAfter("v", "")
                val md = r.body ?: ""
                val version = v.ifEmpty { throw Exception("Weird Version : ${r.tagName}") }

                Logger.log("Git Version : $version")
                Pair(md, version)
            } catch (e: Exception) {
                Logger.log("Error checking for update")
                Logger.log(e)
                Pair("", "")
            }
        }

        suspend fun hasUpdate(repo: String, currentVersion: String): Boolean {
            val (_, version) = check(repo)
            return compareVersion(version, currentVersion)
        }

        suspend fun update(
            activity: Activity,
            repo: String,
            currentVersion: String,
            success: (Boolean) -> Unit
        ) {
            val (_, version) = check(repo)
            if (!compareVersion(version, currentVersion)) {
                success(false)
                toast(activity.getString(R.string.no_update_found))
                return
            }
            MainScope().launch(Dispatchers.IO) {
                try {
                    val apks =
                        client.get("https://api.github.com/repos/$repo/releases/tags/v$version")
                            .parsed<AppUpdater.GithubResponse>().assets?.filter {
                                it.browserDownloadURL.endsWith(
                                    ".apk"
                                )
                            }
                    val apkToDownload =
                        apks?.find { it.browserDownloadURL.contains(getCurrentABI()) }
                            ?: apks?.find { it.browserDownloadURL.contains("universal") }
                            ?: apks?.first()
                    apkToDownload?.browserDownloadURL.apply {
                        if (this != null) activity.downloadUpdate(version, this, repo) { success(it) }
                        else openLinkInBrowser("https://github.com/repos/$repo/releases/tag/v$version")
                    }
                } catch (e: Exception) {
                    logError(e)
                    success(false)
                }
            }
        }

        /**
         * Returns the ABI that the app is most likely running on.
         * @return The primary ABI for the device.
         */
        private fun getCurrentABI(): String {
            return if (Build.SUPPORTED_ABIS.isNotEmpty()) {
                Build.SUPPORTED_ABIS[0]
            } else "Unknown"
        }

        private fun compareVersion(newVersion: String, oldVersion: String): Boolean {
            fun toDouble(list: List<String>): Double {
                return try {
                    list.mapIndexed { i: Int, s: String ->
                        when (i) {
                            0 -> s.toDouble() * 100
                            1 -> s.toDouble() * 10
                            2 -> s.toDouble()
                            else -> s.toDoubleOrNull() ?: 0.0
                        }
                    }.sum()
                } catch (e: NumberFormatException) {
                    0.0
                }
            }

            val new = toDouble(newVersion.split("."))
            val curr = toDouble(oldVersion.split("."))
            return new > curr
        }

        //Blatantly kanged from https://github.com/LagradOst/CloudStream-3/blob/master/app/src/main/java/com/lagradost/cloudstream3/utils/InAppUpdater.kt
        private fun Activity.downloadUpdate(
            version: String,
            url: String,
            repo: String,
            callback: (Boolean) -> Unit
        ) {

            toast(getString(R.string.downloading_update, version))

            val downloadManager = this.getSystemService<DownloadManager>()!!

            val request = DownloadManager.Request(Uri.parse(url))
                .setMimeType("application/vnd.android.package-archive")
                .setTitle("Downloading Dantotsu $version")
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "${repo.replace("/", "")} $version.apk"
                )
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val id = try {
                downloadManager.enqueue(request)
            } catch (e: Exception) {
                logError(e)
                -1
            }
            if (id == -1L) {
                callback(false)
                return
            }
            ContextCompat.registerReceiver(
                this,
                object : BroadcastReceiver() {
                    @SuppressLint("Range")
                    override fun onReceive(context: Context?, intent: Intent?) {
                        try {
                            val downloadId = intent?.getLongExtra(
                                DownloadManager.EXTRA_DOWNLOAD_ID, id
                            ) ?: id

                            downloadManager.getUriForDownloadedFile(downloadId)?.let {
                                openApk(this@downloadUpdate, it) { success ->
                                    callback(success)
                                }
                            }
                        } catch (e: Exception) {
                            logError(e)
                            callback(false)
                        }
                    }
                }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
            return
        }

        private fun openApk(context: Context, uri: Uri, callback: (Boolean) -> Unit) {
            try {
                uri.path?.let {
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                        data = uri
                    }
                    context.startActivity(installIntent)
                    callback(true)
                }
            } catch (e: Exception) {
                logError(e)
                callback(false)
            }
        }
    }
}