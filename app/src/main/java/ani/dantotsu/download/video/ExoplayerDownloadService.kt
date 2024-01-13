package ani.dantotsu.download.video

import android.app.Notification
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import ani.dantotsu.R

@UnstableApi
class ExoplayerDownloadService :
    DownloadService(1, 2000, "download_service", R.string.downloads, 0) {
    companion object {
        private const val JOB_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }

    override fun getDownloadManager(): DownloadManager = Helper.downloadManager(this)

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification =
        DownloadNotificationHelper(this, "download_service").buildProgressNotification(
            this,
            R.drawable.mono,
            null,
            null,
            downloads,
            notMetRequirements
        )
}