package ani.dantotsu.others

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.BuildConfig
import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.client
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.currContext
import ani.dantotsu.decodeBase64ToString
import ani.dantotsu.logError
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.snackString
import ani.dantotsu.toast
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.text.SimpleDateFormat
import java.util.Locale

object AppUpdater {
    private val fallbackStableUrl: String
        get() = "aHR0cHM6Ly9hcGkuZGFudG90c3UuYXBwL3VwZGF0ZXMvc3RhYmxl".decodeBase64ToString()
    private val fallbackBetaUrl: String
        get() = "aHR0cHM6Ly9hcGkuZGFudG90c3UuYXBwL3VwZGF0ZXMvYmV0YQ==".decodeBase64ToString()

    @Serializable
    data class FallbackResponse(
        val version: String,
        val changelog: String,
        val downloadUrl: String? = null
    )

    private suspend fun fetchUpdateInfo(repo: String, isDebug: Boolean): Pair<String, String>? {
        return try {
            fetchFromGithub(repo, isDebug)
        } catch (e: Exception) {
            Logger.log("Github fetch failed, trying fallback: ${e.message}")
            try {
                fetchFromFallback(isDebug)
            } catch (e: Exception) {
                Logger.log("Fallback fetch failed: ${e.message}")
                null
            }
        }
    }

    private suspend fun fetchFromGithub(repo: String, isDebug: Boolean): Pair<String, String> {
        return if (isDebug) {
            val res = client.get("https://api.github.com/repos/$repo/releases")
                .parsed<JsonArray>().map {
                    Mapper.json.decodeFromJsonElement<GithubResponse>(it)
                }
            val r = res.filter { it.prerelease }.filter { !it.tagName.contains("fdroid") }
                .maxByOrNull {
                    it.timeStamp()
                } ?: throw Exception("No Pre Release Found")
            val v = r.tagName.substringAfter("v", "")
            (r.body ?: "") to v.ifEmpty { throw Exception("Weird Version : ${r.tagName}") }
        } else {
            val res = client.get("https://raw.githubusercontent.com/$repo/main/stable.md").text
            res to res.substringAfter("# ").substringBefore("\n")
        }
    }

    private suspend fun fetchFromFallback(isDebug: Boolean): Pair<String, String> {
        val url = if (isDebug) fallbackBetaUrl else fallbackStableUrl
        val response = CommentsAPI.requestBuilder().get(url).parsed<FallbackResponse>()
        return response.changelog to response.version
    }

    private suspend fun fetchApkUrl(repo: String, version: String, isDebug: Boolean): String? {
        return try {
            fetchApkUrlFromGithub(repo, version)
        } catch (e: Exception) {
            Logger.log("Github APK fetch failed, trying fallback: ${e.message}")
            try {
                fetchApkUrlFromFallback(version, isDebug)
            } catch (e: Exception) {
                Logger.log("Fallback APK fetch failed: ${e.message}")
                null
            }
        }
    }

    private suspend fun fetchApkUrlFromGithub(repo: String, version: String): String? {
        val apks = client.get("https://api.github.com/repos/$repo/releases/tags/v$version")
            .parsed<GithubResponse>().assets?.filter {
                it.browserDownloadURL.endsWith(".apk")
            }
        return apks?.firstOrNull()?.browserDownloadURL
    }

    private suspend fun fetchApkUrlFromFallback(version: String, isDebug: Boolean): String? {
        val url = if (isDebug) fallbackBetaUrl else fallbackStableUrl
        return CommentsAPI.requestBuilder().get("$url/$version").parsed<FallbackResponse>().downloadUrl
    }

    suspend fun check(activity: FragmentActivity, post: Boolean = false) {
        if (post) snackString(currContext()?.getString(R.string.checking_for_update))
        val repo = activity.getString(R.string.repo)
        tryWithSuspend {
            val (md, version) = fetchUpdateInfo(repo, BuildConfig.DEBUG) ?: return@tryWithSuspend

            Logger.log("Git Version : $version")
            val dontShow = PrefManager.getCustomVal("dont_ask_for_update_$version", false)
            if (compareVersion(version) && !dontShow && !activity.isDestroyed) activity.runOnUiThread {
                CustomBottomDialog.newInstance().apply {
                    setTitleText(
                        "${if (BuildConfig.DEBUG) "Beta " else ""}Update " + currContext()!!.getString(
                            R.string.available
                        )
                    )
                    addView(
                        TextView(activity).apply {
                            val markWon = try {
                                buildMarkwon(activity, false)
                            } catch (e: IllegalArgumentException) {
                                return@runOnUiThread
                            }
                            markWon.setMarkdown(this, md)
                        }
                    )

                    setCheck(
                        currContext()!!.getString(R.string.dont_show_again, version),
                        false
                    ) { isChecked ->
                        if (isChecked) {
                            PrefManager.setCustomVal("dont_ask_for_update_$version", true)
                        }
                    }
                    setPositiveButton(currContext()!!.getString(R.string.lets_go)) {
                        MainScope().launch(Dispatchers.IO) {
                            try {
                                val apkUrl = fetchApkUrl(repo, version, BuildConfig.DEBUG)
                                if (apkUrl != null) {
                                    activity.downloadUpdate(version, apkUrl)
                                } else {
                                    openLinkInBrowser("https://github.com/repos/$repo/releases/tag/v$version")
                                }
                            } catch (e: Exception) {
                                logError(e)
                            }
                        }
                        dismiss()
                    }
                    setNegativeButton(currContext()!!.getString(R.string.cope)) {
                        dismiss()
                    }
                    show(activity.supportFragmentManager, "dialog")
                }
            } else {
                if (post) snackString(currContext()?.getString(R.string.no_update_found))
            }
        }
    }

    private fun compareVersion(version: String): Boolean {
        return when (BuildConfig.BUILD_TYPE) {
            "debug" -> BuildConfig.VERSION_NAME != version
            "alpha" -> false
            else -> {
                fun toDouble(list: List<String>): Double {
                    return list.mapIndexed { i: Int, s: String ->
                        when (i) {
                            0 -> s.toDouble() * 100
                            1 -> s.toDouble() * 10
                            2 -> s.toDouble()
                            else -> s.toDoubleOrNull() ?: 0.0
                        }
                    }.sum()
                }

                val new = toDouble(version.split("."))
                val curr = toDouble(BuildConfig.VERSION_NAME.split("."))
                new > curr
            }
        }
    }


    //Blatantly kanged from https://github.com/LagradOst/CloudStream-3/blob/master/app/src/main/java/com/lagradost/cloudstream3/utils/InAppUpdater.kt
    private fun Activity.downloadUpdate(version: String, url: String) {
        toast(getString(R.string.downloading_update, version))

        val downloadManager = this.getSystemService<DownloadManager>()!!

        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType("application/vnd.android.package-archive")
            .setTitle("Downloading Dantotsu $version")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Dantotsu $version.apk"
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
        if (id == -1L) return
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
                            openApk(this@downloadUpdate, it)
                        }
                    } catch (e: Exception) {
                        logError(e)
                    }
                }
            }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun openApk(context: Context, uri: Uri) {
        try {
            uri.path?.let {
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    data = uri
                }
                context.startActivity(installIntent)
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    @Serializable
    data class GithubResponse(
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("tag_name")
        val tagName: String,
        val prerelease: Boolean,
        @SerialName("created_at")
        val createdAt: String,
        val body: String? = null,
        val assets: List<Asset>? = null
    ) {
        @Serializable
        data class Asset(
            @SerialName("browser_download_url")
            val browserDownloadURL: String
        )

        fun timeStamp(): Long {
            return dateFormat.parse(createdAt)!!.time
        }
    }
}