package ani.dantotsu.media

import android.content.Context
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.parsers.SubtitleType
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import com.anggrayudi.storage.file.openOutputStream
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SubtitleDownloader {

    companion object {
        //doesn't really download the subtitles -\_(o_o)_/-
        suspend fun loadSubtitleType(url: String): SubtitleType =
            withContext(Dispatchers.IO) {
                return@withContext try {
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

                        subtitleType
                    } else {
                        SubtitleType.UNKNOWN
                    }
                } catch (e: Exception) {
                    Logger.log(e)
                    SubtitleType.UNKNOWN
                }
            }

        //actually downloads lol
        @Deprecated("handled externally")
        suspend fun downloadSubtitle(
            context: Context,
            url: String,
            downloadedType: DownloadedType
        ) {
            try {
                val directory = DownloadsManager.getSubDirectory(
                    context,
                    downloadedType.type,
                    false,
                    downloadedType.titleName,
                    downloadedType.chapterName
                ) ?: throw Exception("Could not create directory")
                val type = loadSubtitleType(url)
                directory.findFile("subtitle.${type}")?.delete()
                val subtitleFile = directory.createFile("*/*", "subtitle.${type}")
                    ?: throw Exception("Could not create subtitle file")

                val client = Injekt.get<NetworkHelper>().client
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    snackString("Failed to download subtitle")
                    return
                }

                response.body.byteStream().use { input ->
                    subtitleFile.openOutputStream(context, false).use { output ->
                        if (output == null) throw Exception("Could not open output stream")
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
