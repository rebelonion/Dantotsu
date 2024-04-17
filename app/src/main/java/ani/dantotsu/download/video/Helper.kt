package ani.dantotsu.download.video

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.R
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.anime.AnimeDownloaderService
import ani.dantotsu.download.anime.AnimeServiceDataSingleton
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaType
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.Video
import ani.dantotsu.settings.saving.PrefManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@SuppressLint("UnsafeOptInUsageError")
object Helper {
    @OptIn(UnstableApi::class)
    fun startAnimeDownloadService(
        context: Context,
        title: String,
        episode: String,
        video: Video,
        subtitle: Subtitle? = null,
        sourceMedia: Media? = null,
        episodeImage: String? = null
    ) {
        if (!isNotificationPermissionGranted(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        val animeDownloadTask = AnimeDownloaderService.AnimeDownloadTask(
            title,
            episode,
            video,
            subtitle,
            sourceMedia,
            episodeImage
        )

        val downloadsManger = Injekt.get<DownloadsManager>()
        val downloadCheck = downloadsManger
            .queryDownload(title, episode, MediaType.ANIME)

        if (downloadCheck) {
            AlertDialog.Builder(context, R.style.MyPopup)
                .setTitle("Download Exists")
                .setMessage("A download for this episode already exists. Do you want to overwrite it?")
                .setPositiveButton("Yes") { _, _ ->
                    PrefManager.getAnimeDownloadPreferences().edit()
                        .remove(animeDownloadTask.getTaskName())
                        .apply()
                    downloadsManger.removeDownload(
                        DownloadedType(
                            title,
                            episode,
                            MediaType.ANIME
                        )
                    ) {
                        AnimeServiceDataSingleton.downloadQueue.offer(animeDownloadTask)
                        if (!AnimeServiceDataSingleton.isServiceRunning) {
                            val intent = Intent(context, AnimeDownloaderService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                            AnimeServiceDataSingleton.isServiceRunning = true
                        }
                    }
                }
                .setNegativeButton("No") { _, _ -> }
                .show()
        } else {
            AnimeServiceDataSingleton.downloadQueue.offer(animeDownloadTask)
            if (!AnimeServiceDataSingleton.isServiceRunning) {
                val intent = Intent(context, AnimeDownloaderService::class.java)
                ContextCompat.startForegroundService(context, intent)
                AnimeServiceDataSingleton.isServiceRunning = true
            }
        }
    }

    private fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}