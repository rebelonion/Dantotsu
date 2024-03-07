package ani.dantotsu.profile.activity

import android.view.View
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.databinding.ItemNotificationBinding
import ani.dantotsu.loadImage
import ani.dantotsu.notifications.NotificationActivity
import com.xwray.groupie.viewbinding.BindableItem

class NotificationItem(
    private val notification: Notification,
    val clickCallback: (Int, NotificationActivity.Companion.NotificationClickType) -> Unit
): BindableItem<ItemNotificationBinding>() {
    private lateinit var binding: ItemNotificationBinding
    private lateinit var clickType: NotificationActivity.Companion.NotificationClickType
    private var id = 0
    override fun bind(viewBinding: ItemNotificationBinding, position: Int) {
        binding = viewBinding
        setBinding()
    }

    override fun getLayout(): Int {
        return R.layout.item_notification
    }

    override fun initializeViewBinding(view: View): ItemNotificationBinding {
        return ItemNotificationBinding.bind(view)
    }

    private fun setBinding() {
        val notificationType: NotificationType =
            NotificationType.valueOf(notification.notificationType)
        binding.notificationText.text = NotificationItemBuilder.getContent(notification)
        binding.notificationDate.text = NotificationItemBuilder.getDateTime(notification.createdAt)
        binding.root.setOnClickListener { clickCallback(id, clickType) }

        when (notificationType) {
            NotificationType.ACTIVITY_MESSAGE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.ACTIVITY_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.FOLLOWING -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.ACTIVITY_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_COMMENT_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_COMMENT_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.AIRING -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                binding.notificationBannerImage.loadImage(notification.media?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.MEDIA
                id = notification.media?.id ?: 0
            }
            NotificationType.ACTIVITY_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.ACTIVITY_REPLY_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_COMMENT_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.ACTIVITY_REPLY_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationBannerImage.loadImage(notification.user?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.RELATED_MEDIA_ADDITION -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                binding.notificationBannerImage.loadImage(notification.media?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.MEDIA
                id = notification.media?.id ?: 0
            }
            NotificationType.MEDIA_DATA_CHANGE -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                binding.notificationBannerImage.loadImage(notification.media?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.MEDIA
                id = notification.media?.id ?: 0
            }
            NotificationType.MEDIA_MERGE -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                binding.notificationBannerImage.loadImage(notification.media?.bannerImage)
                clickType = NotificationActivity.Companion.NotificationClickType.MEDIA
                id = notification.media?.id ?: 0
            }
            NotificationType.MEDIA_DELETION -> {
                binding.notificationCover.visibility = View.GONE
                clickType = NotificationActivity.Companion.NotificationClickType.UNDEFINED
                id = 0
            }
        }
    }

}