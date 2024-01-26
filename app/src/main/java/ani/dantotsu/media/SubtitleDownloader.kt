package ani.dantotsu.media

import android.content.Context
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.parsers.SubtitleType
import ani.dantotsu.snackString
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class SubtitleDownloader {

    companion object {
        //doesn't really download the subtitles -\_(o_o)_/-
        suspend fun loadSubtitleType(context: Context, url: String): SubtitleType =
            withContext(Dispatchers.IO) {
                // Initialize the NetworkHelper instance. Replace this line based on how you usually initialize it
                val networkHelper = Injekt.get<NetworkHelper>()
                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = networkHelper.client.newCall(request).execute()

                // Check if response is successful
                if (response.isSuccessful) {
                    val responseBody = response.body.string()


                    val subtitleType = when {
                        responseBody.contains("[Script Info]") -> SubtitleType.ASS
                        responseBody.contains("WEBVTT") -> SubtitleType.VTT
                        else -> SubtitleType.SRT
                    }

                    return@withContext subtitleType
                } else {
                    return@withContext SubtitleType.UNKNOWN
                }
            }

        //actually downloads lol
        suspend fun downloadSubtitle(
            context: Context,
            url: String,
            downloadedType: DownloadedType
        ) {
            try {
                val directory = DownloadsManager.getDirectory(
                    context,
                    downloadedType.type,
                    downloadedType.title,
                    downloadedType.chapter
                )
                if (!directory.exists()) { //just in case
                    directory.mkdirs()
                }
                val type = loadSubtitleType(context, url)
                val subtiteFile = File(directory, "subtitle.${type}")
                if (subtiteFile.exists()) {
                    subtiteFile.delete()
                }
                subtiteFile.createNewFile()

                val client = Injekt.get<NetworkHelper>().client
                val request = Request.Builder().url(url).build()
                val reponse = client.newCall(request).execute()

                if (!reponse.isSuccessful) {
                    snackString("Failed to download subtitle")
                    return
                }

                reponse.body.byteStream().use { input ->
                    subtiteFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                snackString("Failed to download subtitle")
                e.printStackTrace()
                return
            }

        }
    }
}
