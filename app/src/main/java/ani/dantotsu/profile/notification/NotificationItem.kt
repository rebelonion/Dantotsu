package ani.dantotsu.profile.notification

import android.view.View
import android.view.ViewGroup
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.databinding.ItemNotificationBinding
import ani.dantotsu.loadImage
import ani.dantotsu.notifications.comment.CommentStore
import ani.dantotsu.notifications.subscription.SubscriptionStore
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.profile.notification.NotificationFragment.Companion.NotificationClickType
import ani.dantotsu.profile.notification.NotificationFragment.Companion.NotificationType.COMMENT
import ani.dantotsu.profile.notification.NotificationFragment.Companion.NotificationType.SUBSCRIPTION
import ani.dantotsu.setAnimation
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.toPx
import ani.dantotsu.util.customAlertDialog
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem

class NotificationItem(
    private val notification: Notification,
    val type: NotificationFragment.Companion.NotificationType,
    val parentAdapter: GroupieAdapter,
    val clickCallback: (Int, Int?, NotificationClickType) -> Unit,

    ) : BindableItem<ItemNotificationBinding>() {
    private lateinit var binding: ItemNotificationBinding
    override fun bind(viewBinding: ItemNotificationBinding, position: Int) {
        binding = viewBinding
        setAnimation(binding.root.context, binding.root)
        setBinding()
    }

    fun dialog() {
        when (type) {
            COMMENT, SUBSCRIPTION -> {
                binding.root.context.customAlertDialog().apply {
                    setTitle(R.string.delete)
                    setMessage(ActivityItemBuilder.getContent(notification))
                    setPosButton(R.string.yes) {
                        when (type) {
                            COMMENT -> {
                                val list = PrefManager.getNullableVal<List<CommentStore>>(
                                    PrefName.CommentNotificationStore,
                                    null
                                ) ?: listOf()
                                val newList = list.filter { it.commentId != notification.commentId }
                                PrefManager.setVal(PrefName.CommentNotificationStore, newList)
                                parentAdapter.remove(this@NotificationItem)

                            }

                            SUBSCRIPTION -> {
                                val list = PrefManager.getNullableVal<List<SubscriptionStore>>(
                                    PrefName.SubscriptionNotificationStore,
                                    null
                                ) ?: listOf()
                                val newList =
                                    list.filter { (it.time / 1000L).toInt() != notification.createdAt }
                                PrefManager.setVal(PrefName.SubscriptionNotificationStore, newList)
                                parentAdapter.remove(this@NotificationItem)
                            }

                            else -> {}
                        }
                    }
                    setNegButton(R.string.no)
                    show()
                }
            }

            else -> {}
        }

    }

    override fun getLayout(): Int {
        return R.layout.item_notification
    }

    override fun initializeViewBinding(view: View): ItemNotificationBinding {
        return ItemNotificationBinding.bind(view)
    }

    private fun image(
        user: Boolean = false,
        commentNotification: Boolean = false,
        newRelease: Boolean = false
    ) {

        val cover = if (user) notification.user?.bannerImage
            ?: notification.user?.avatar?.medium else notification.media?.bannerImage
            ?: notification.media?.coverImage?.large
        blurImage(binding.notificationBannerImage, if (newRelease) notification.banner else cover)

        val defaultHeight = 153.toPx

        val userHeight = 90.toPx

        val textMarginStart = 125.toPx

        if (user) {
            binding.notificationCover.visibility = View.GONE
            binding.notificationCoverUser.visibility = View.VISIBLE
            binding.notificationCoverUserContainer.visibility = View.VISIBLE
            if (commentNotification) {
                binding.notificationCoverUser.setImageResource(R.drawable.ic_dantotsu_round)
                binding.notificationCoverUser.scaleX = 1.4f
                binding.notificationCoverUser.scaleY = 1.4f
            } else {
                binding.notificationCoverUser.loadImage(notification.user?.avatar?.large)
            }
            binding.notificationBannerImage.layoutParams.height = userHeight
            binding.notificationGradiant.layoutParams.height = userHeight
            (binding.notificationTextContainer.layoutParams as ViewGroup.MarginLayoutParams).marginStart =
                userHeight
        } else {
            binding.notificationCover.visibility = View.VISIBLE
            binding.notificationCoverUser.visibility = View.VISIBLE
            binding.notificationCoverUserContainer.visibility = View.GONE
            binding.notificationCover.loadImage(if (newRelease) notification.image else notification.media?.coverImage?.large)
            binding.notificationBannerImage.layoutParams.height = defaultHeight
            binding.notificationGradiant.layoutParams.height = defaultHeight
            (binding.notificationTextContainer.layoutParams as ViewGroup.MarginLayoutParams).marginStart =
                textMarginStart
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
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.FOLLOWING -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.userId ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.ACTIVITY_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.THREAD_COMMENT_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_COMMENT_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.AIRING -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.ACTIVITY_LIKE -> {
                image(true)
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.THREAD_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_COMMENT_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.RELATED_MEDIA_ADDITION -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_DATA_CHANGE -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_MERGE -> {
                binding.notificationCover.loadImage(notification.media?.coverImage?.large)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_DELETION -> {
                binding.notificationCover.visibility = View.GONE
            }

            NotificationType.COMMENT_REPLY -> {
                image(user = true, commentNotification = true)
                if (notification.commentId != null && notification.mediaId != null) {
                    binding.notificationBannerImage.setOnClickListener {
                        clickCallback(
                            notification.mediaId,
                            notification.commentId,
                            NotificationClickType.COMMENT
                        )
                    }
                }
            }

            NotificationType.COMMENT_WARNING -> {
                image(user = true, commentNotification = true)
                if (notification.commentId != null && notification.mediaId != null) {
                    binding.notificationBannerImage.setOnClickListener {
                        clickCallback(
                            notification.mediaId,
                            notification.commentId,
                            NotificationClickType.COMMENT
                        )
                    }
                }
            }

            NotificationType.DANTOTSU_UPDATE -> {
                image(user = true)
            }

            NotificationType.SUBSCRIPTION -> {
                image(newRelease = true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.mediaId ?: 0, null, NotificationClickType.MEDIA
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.mediaId ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }
        }
        binding.notificationCoverUser.setOnLongClickListener {
            dialog()
            true
        }
        binding.notificationBannerImage.setOnLongClickListener {
            dialog()
            true
        }
    }

}