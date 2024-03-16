package ani.dantotsu.profile.activity

import android.util.TypedValue
import android.view.View
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.databinding.ItemNotificationBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.activity.NotificationActivity.Companion.NotificationClickType
import ani.dantotsu.setAnimation
import com.xwray.groupie.viewbinding.BindableItem

class NotificationItem(
    private val notification: Notification,
    val clickCallback: (Int, NotificationClickType) -> Unit
) : BindableItem<ItemNotificationBinding>() {
    private lateinit var binding: ItemNotificationBinding
    override fun bind(viewBinding: ItemNotificationBinding, position: Int) {
        binding = viewBinding
        setAnimation(binding.root.context, binding.root)
        setBinding()
    }

    override fun getLayout(): Int {
        return R.layout.item_notification
    }

    override fun initializeViewBinding(view: View): ItemNotificationBinding {
        return ItemNotificationBinding.bind(view)
    }

    private fun image(user: Boolean = false) {

        val cover = if (user) notification.user?.bannerImage
            ?: notification.user?.avatar?.medium else notification.media?.bannerImage
            ?: notification.media?.coverImage?.large
        blurImage(binding.notificationBannerImage, cover)
        val defaultHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            170f,
            binding.root.context.resources.displayMetrics
        ).toInt()
        val userHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            90f,
            binding.root.context.resources.displayMetrics
        ).toInt()

        if (user) {
            binding.notificationCover.visibility = View.GONE
            binding.notificationCoverUser.visibility = View.VISIBLE
            binding.notificationCoverUserContainer.visibility = View.VISIBLE
            binding.notificationCoverUser.loadImage(notification.user?.avatar?.large)
            binding.notificationBannerImage.layoutParams.height = userHeight
        } else {
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

        when (notificationType) {
            NotificationType.ACTIVITY_MESSAGE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.FOLLOWING -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.userId ?: 0, NotificationClickType.USER
                    )
                }
            }

            NotificationType.ACTIVITY_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.THREAD_COMMENT_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_COMMENT_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
            }

            NotificationType.AIRING -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.ACTIVITY_LIKE -> {
                image(true)
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.THREAD_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_COMMENT_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.RELATED_MEDIA_ADDITION -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_DATA_CHANGE -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_MERGE -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_DELETION -> {
                binding.notificationCover.visibility = View.GONE
            }
        }
    }

}