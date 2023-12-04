package ani.dantotsu.media

import android.content.Context
import ani.dantotsu.parsers.SubtitleType
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SubtitleDownloader {

    companion object {
        //doesn't really download the subtitles -\_(o_o)_/-
        suspend fun downloadSubtitles(context: Context, url: String): SubtitleType =
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
                        responseBody?.contains("[Script Info]") == true -> SubtitleType.ASS
                        responseBody?.contains("WEBVTT") == true -> SubtitleType.VTT
                        else -> SubtitleType.SRT
                    }

                    return@withContext subtitleType
                } else {
                    return@withContext SubtitleType.UNKNOWN
                }
            }
    }
}
