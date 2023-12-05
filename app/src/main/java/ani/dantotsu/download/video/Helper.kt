package ani.dantotsu.download.video

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.ui.TrackSelectionDialogBuilder
import ani.dantotsu.R
import ani.dantotsu.defaultHeaders
import ani.dantotsu.logError
import ani.dantotsu.okHttpClient
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.SubtitleType
import ani.dantotsu.parsers.Video
import ani.dantotsu.parsers.VideoType
import eu.kanade.tachiyomi.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.IOException
import java.util.concurrent.*

object Helper {

    @SuppressLint("UnsafeOptInUsageError")
    fun downloadVideo(context: Context, video: Video, subtitle: Subtitle?) {
        val dataSourceFactory = DataSource.Factory {
            val dataSource: HttpDataSource =
                OkHttpDataSource.Factory(okHttpClient).createDataSource()
            defaultHeaders.forEach {
                dataSource.setRequestProperty(it.key, it.value)
            }
            video.file.headers.forEach {
                dataSource.setRequestProperty(it.key, it.value)
            }
            dataSource
        }
        val mimeType = when (video.format) {
            VideoType.M3U8 -> MimeTypes.APPLICATION_M3U8
            VideoType.DASH -> MimeTypes.APPLICATION_MPD
            else -> MimeTypes.APPLICATION_MP4
        }

        val builder = MediaItem.Builder().setUri(video.file.url).setMimeType(mimeType)
        var sub: MediaItem.SubtitleConfiguration? = null
        if (subtitle != null) {
            sub = MediaItem.SubtitleConfiguration
                .Builder(Uri.parse(subtitle.file.url))
                .setSelectionFlags(C.SELECTION_FLAG_FORCED)
                .setMimeType(
                    when (subtitle.type) {
                        SubtitleType.VTT -> MimeTypes.TEXT_VTT
                        SubtitleType.ASS -> MimeTypes.TEXT_SSA
                        SubtitleType.SRT -> MimeTypes.APPLICATION_SUBRIP
                        SubtitleType.UNKNOWN -> MimeTypes.TEXT_SSA
                    }
                )
                .build()
        }
        if (sub != null) builder.setSubtitleConfigurations(mutableListOf(sub))
        val mediaItem = builder.build()
        val downloadHelper = DownloadHelper.forMediaItem(
            context,
            mediaItem,
            DefaultRenderersFactory(context),
            dataSourceFactory
        )
        downloadHelper.prepare(object : DownloadHelper.Callback {
            override fun onPrepared(helper: DownloadHelper) {
                TrackSelectionDialogBuilder(
                    context, "Select thingy", helper.getTracks(0).groups
                ) { _, overrides ->
                    val params = TrackSelectionParameters.Builder(context)
                    overrides.forEach {
                        params.addOverride(it.value)
                    }
                    helper.addTrackSelection(0, params.build())
                    MyDownloadService
                    DownloadService.sendAddDownload(
                        context,
                        MyDownloadService::class.java,
                        helper.getDownloadRequest(null),
                        false
                    )
                }.apply {
                    setTheme(R.style.DialogTheme)
                    setTrackNameProvider {
                        if (it.frameRate > 0f) it.height.toString() + "p" else it.height.toString() + "p (fps : N/A)"
                    }
                    build().show()
                }
            }

            override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                logError(e)
            }
        })
    }


    private var download: DownloadManager? = null
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    @Synchronized
    @UnstableApi
    fun downloadManager(context: Context): DownloadManager {
        return download ?: let {
            val database = StandaloneDatabaseProvider(context)
            val downloadDirectory = File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            val dataSourceFactory = DataSource.Factory {
                //val dataSource: HttpDataSource = OkHttpDataSource.Factory(okHttpClient).createDataSource()
                val networkHelper = Injekt.get<NetworkHelper>()
                val okHttpClient = networkHelper.client
                val dataSource: HttpDataSource =
                    OkHttpDataSource.Factory(okHttpClient).createDataSource()
                defaultHeaders.forEach {
                    dataSource.setRequestProperty(it.key, it.value)
                }
                dataSource
            }
            DownloadManager(
                context,
                database,
                SimpleCache(downloadDirectory, NoOpCacheEvictor(), database),
                dataSourceFactory,
                Executor(Runnable::run)
            ).apply {
                requirements =
                    Requirements(Requirements.NETWORK or Requirements.DEVICE_STORAGE_NOT_LOW)
                maxParallelDownloads = 3
            }
        }
    }

    private var downloadDirectory: File? = null

    @Synchronized
    private fun getDownloadDirectory(context: Context): File {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(null)
            if (downloadDirectory == null) {
                downloadDirectory = context.filesDir
            }
        }
        return downloadDirectory!!
    }
}