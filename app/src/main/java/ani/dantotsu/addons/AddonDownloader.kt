package ani.dantotsu.addons

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.logError
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.settings.InstallerSteps
import ani.dantotsu.toast
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import rx.android.schedulers.AndroidSchedulers

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

                Logger.log("Git Version for $repo: $version")
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
            manager: AddonManager<*>,
            repo: String,
            currentVersion: String
        ) {
            val (_, version) = check(repo)
            if (!compareVersion(version, currentVersion)) {
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
                        if (this != null) {
                            val notificationManager =
                                activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val installerSteps = InstallerSteps(notificationManager, activity)
                            manager.install(this)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                    { installStep -> installerSteps.onInstallStep(installStep) {} },
                                    { error -> installerSteps.onError(error) {} },
                                    { installerSteps.onComplete {} }
                                )
                        } else openLinkInBrowser("https://github.com/repos/$repo/releases/tag/v$version")
                    }
                } catch (e: Exception) {
                    logError(e)
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

    }
}