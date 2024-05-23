package eu.kanade.tachiyomi.data.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import eu.kanade.tachiyomi.util.system.buildNotificationChannel
import eu.kanade.tachiyomi.util.system.buildNotificationChannelGroup

/**
 * Class to manage the basic information of all the notifications used in the app.
 */
object Notifications {

    /**
     * Common notification channel and ids used anywhere.
     */
    const val CHANNEL_COMMON = "common_channel"
    const val ID_DOWNLOAD_IMAGE = 2

    /**
     * Notification channel and ids used by the downloader.
     */
    private const val GROUP_DOWNLOADER = "group_downloader"
    const val CHANNEL_DOWNLOADER_PROGRESS = "downloader_progress_channel"
    const val ID_DOWNLOAD_CHAPTER_PROGRESS = -201
    const val ID_DOWNLOAD_EPISODE_PROGRESS = -203
    const val CHANNEL_DOWNLOADER_ERROR = "downloader_error_channel"
    const val ID_DOWNLOAD_CHAPTER_ERROR = -202
    const val ID_DOWNLOAD_EPISODE_ERROR = -204

    /**
     * Notification channel and ids used by the library updater.
     */
    const val CHANNEL_NEW_CHAPTERS_EPISODES = "new_chapters_episodes_channel"
    const val ID_NEW_CHAPTERS = -301
    const val ID_NEW_EPISODES = -1301
    const val GROUP_NEW_CHAPTERS = "eu.kanade.tachiyomi.NEW_CHAPTERS"
    const val GROUP_NEW_EPISODES = "eu.kanade.tachiyomi.NEW_EPISODES"

    /**
     * Notification channel and ids used by the torrent server.
     */
    const val ID_TORRENT_SERVER = -1100
    const val CHANNEL_TORRENT_SERVER = "dantotsu_torrent_server"

    /**
     * Notification channel used for Incognito Mode
     */
    const val CHANNEL_INCOGNITO_MODE = "incognito_mode_channel"
    const val ID_INCOGNITO_MODE = -701

    /**
     * Notification channel used for comment notifications
     */
    private const val GROUP_COMMENTS = "group_comments"
    const val CHANNEL_COMMENTS = "comments_channel"
    const val CHANNEL_COMMENT_WARING = "comment_warning_channel"
    const val ID_COMMENT_REPLY = -801


    const val CHANNEL_APP_GLOBAL = "app_global"

    /**
     * Notification channel and ids used for anilist updates.
     */
    const val GROUP_ANILIST = "group_anilist"
    const val CHANNEL_ANILIST = "anilist_channel"
    const val ID_ANILIST = -901

    /**
     * Notification channel and ids used subscription checks.
     */
    const val GROUP_SUBSCRIPTION_CHECK = "group_subscription_check"
    const val CHANNEL_SUBSCRIPTION_CHECK = "subscription_check_channel"
    const val CHANNEL_SUBSCRIPTION_CHECK_PROGRESS = "subscription_check_progress_channel"
    const val ID_SUBSCRIPTION_CHECK = -1001
    const val ID_SUBSCRIPTION_CHECK_PROGRESS = -1002


    /**
     * Notification channel and ids used for app and extension updates.
     */
    private const val GROUP_APK_UPDATES = "group_apk_updates"
    const val CHANNEL_APP_UPDATE = "app_apk_update_channel"
    const val ID_APP_UPDATER = 1
    const val ID_APP_UPDATE_PROMPT = 2
    const val CHANNEL_EXTENSIONS_UPDATE = "ext_apk_update_channel"
    const val ID_UPDATES_TO_EXTS = -401
    const val ID_EXTENSION_INSTALLER = -402

    private val deprecatedChannels = listOf(
        "downloader_channel",
        "downloader_complete_channel",
        "backup_restore_complete_channel",
        "library_channel",
        "library_progress_channel",
        "updates_ext_channel",
        "downloader_cache_renewal",
        "crash_logs_channel",
        "backup_restore_complete_channel_v2",
        "backup_restore_progress_channel",
        "group_backup_restore",
        "library_skipped_channel",
        "library_errors_channel",
        "library_progress_channel",
    )

    /**
     * Creates the notification channels introduced in Android Oreo.
     * This won't do anything on Android versions that don't support notification channels.
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Delete old notification channels
        deprecatedChannels.forEach(notificationManager::deleteNotificationChannel)

        notificationManager.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(GROUP_DOWNLOADER) {
                    setName("Downloader")
                },
                buildNotificationChannelGroup(GROUP_APK_UPDATES) {
                    setName("App & Extension Updates")
                },
                buildNotificationChannelGroup(GROUP_COMMENTS) {
                    setName("Comments")
                },
                buildNotificationChannelGroup(GROUP_ANILIST) {
                    setName("Anilist")
                },
                buildNotificationChannelGroup(GROUP_SUBSCRIPTION_CHECK) {
                    setName("Subscription Checks")
                },
            ),
        )

        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(CHANNEL_COMMON, IMPORTANCE_LOW) {
                    setName("Common")
                },
                buildNotificationChannel(CHANNEL_NEW_CHAPTERS_EPISODES, IMPORTANCE_DEFAULT) {
                    setName("New Chapters & Episodes")
                },
                buildNotificationChannel(CHANNEL_DOWNLOADER_PROGRESS, IMPORTANCE_LOW) {
                    setName("Downloader Progress")
                    setGroup(GROUP_DOWNLOADER)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_DOWNLOADER_ERROR, IMPORTANCE_LOW) {
                    setName("Downloader Errors")
                    setGroup(GROUP_DOWNLOADER)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_INCOGNITO_MODE, IMPORTANCE_LOW) {
                    setName("Incognito Mode")
                },
                buildNotificationChannel(CHANNEL_TORRENT_SERVER, IMPORTANCE_LOW) {
                    setName("Torrent Server")
                },
                buildNotificationChannel(CHANNEL_COMMENTS, IMPORTANCE_HIGH) {
                    setName("Comments")
                    setGroup(GROUP_COMMENTS)
                },
                buildNotificationChannel(CHANNEL_COMMENT_WARING, IMPORTANCE_HIGH) {
                    setName("Comment Warnings")
                    setGroup(GROUP_COMMENTS)
                },
                buildNotificationChannel(CHANNEL_ANILIST, IMPORTANCE_DEFAULT) {
                    setName("Anilist")
                    setGroup(GROUP_ANILIST)
                },
                buildNotificationChannel(CHANNEL_SUBSCRIPTION_CHECK, IMPORTANCE_LOW) {
                    setName("Subscription Checks")
                    setGroup(GROUP_SUBSCRIPTION_CHECK)
                },
                buildNotificationChannel(CHANNEL_SUBSCRIPTION_CHECK_PROGRESS, IMPORTANCE_DEFAULT) {
                    setName("Subscription Checks Progress")
                    setGroup(GROUP_SUBSCRIPTION_CHECK)
                },
                buildNotificationChannel(CHANNEL_APP_GLOBAL, IMPORTANCE_HIGH) {
                    setName("Global Updates")
                },
                buildNotificationChannel(CHANNEL_APP_UPDATE, IMPORTANCE_DEFAULT) {
                    setGroup(GROUP_APK_UPDATES)
                    setName("App Updates")
                },
                buildNotificationChannel(CHANNEL_EXTENSIONS_UPDATE, IMPORTANCE_DEFAULT) {
                    setGroup(GROUP_APK_UPDATES)
                    setName("Extension Updates")
                },
            ),
        )
    }
}
