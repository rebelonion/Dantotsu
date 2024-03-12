package ani.dantotsu.profile.activity

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.databinding.ItemNotificationBinding
import ani.dantotsu.loadImage
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.xwray.groupie.viewbinding.BindableItem
import jp.wasabeef.glide.transformations.BlurTransformation

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

    private fun image(user: Boolean = false) {

        val cover = if (user) notification.user?.bannerImage ?: notification.user?.avatar?.medium else notification.media?.bannerImage ?: notification.media?.coverImage?.large
        blurImage(binding.notificationBannerImage, cover)
        val defaultHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170f, binding.root.context.resources.displayMetrics).toInt()
        val userHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90f, binding.root.context.resources.displayMetrics).toInt()

        if (user) {
            binding.notificationCover.visibility = View.GONE
            binding.notificationCoverUser.visibility = View.VISIBLE
            binding.notificationCoverUserContainer.visibility = View.VISIBLE
            binding.notificationCoverUser.loadImage(notification.user?.avatar?.large)
            binding.notificationBannerImage.layoutParams.height = userHeight
        } else{
            binding.notificationCover.visibility = View.VISIBLE
            binding.notificationCoverUser.visibility = View.VISIBLE
            binding.notificationCoverUserContainer.visibility = View.GONE
            binding.notificationCover.loadImage(notification.media?.coverImage?.large)
            binding.notificationBannerImage.layoutParams.height = defaultHeight
        }
    }

    private fun setBinding() {
        val notificationType: NotificationType =
            NotificationType.valueOf(notification.notificationType)
        binding.notificationText.text = ActivityItemBuilder.getContent(notification)
        binding.notificationDate.text = ActivityItemBuilder.getDateTime(notification.createdAt)
        binding.root.setOnClickListener { clickCallback(id, clickType) }
        
        when (notificationType) {
            NotificationType.ACTIVITY_MESSAGE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.ACTIVITY_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.FOLLOWING -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.ACTIVITY_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_COMMENT_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_COMMENT_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.AIRING -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                clickType = NotificationActivity.Companion.NotificationClickType.MEDIA
                id = notification.media?.id ?: 0
            }
            NotificationType.ACTIVITY_LIKE -> {
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.ACTIVITY_REPLY_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.THREAD_COMMENT_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.ACTIVITY_REPLY_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                clickType = NotificationActivity.Companion.NotificationClickType.USER
                id = notification.user?.id ?: 0
            }
            NotificationType.RELATED_MEDIA_ADDITION -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                clickType = NotificationActivity.Companion.NotificationClickType.MEDIA
                id = notification.media?.id ?: 0
            }
            NotificationType.MEDIA_DATA_CHANGE -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                clickType = NotificationActivity.Companion.NotificationClickType.MEDIA
                id = notification.media?.id ?: 0
            }
            NotificationType.MEDIA_MERGE -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
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