package ani.dantotsu.profile.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.connections.anilist.api.NotificationType.Companion.fromFormattedString
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.notifications.comment.CommentStore
import ani.dantotsu.notifications.subscription.SubscriptionStore
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding
    private lateinit var commentStore: List<CommentStore>
    private lateinit var subscriptionStore: List<SubscriptionStore>
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var notificationList: List<Notification> = emptyList()
    val filters = ArrayList<String>()
    private var currentPage: Int = 1
    private var hasNextPage: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.listTitle.text = getString(R.string.notifications)
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }
        binding.listFrameLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.followerGrid.visibility = ViewGroup.GONE
        binding.followerList.visibility = ViewGroup.GONE
        binding.listBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.listProgressBar.visibility = ViewGroup.VISIBLE
        commentStore = PrefManager.getNullableVal<List<CommentStore>>(
            PrefName.CommentNotificationStore,
            null
        ) ?: listOf()
        subscriptionStore = PrefManager.getNullableVal<List<SubscriptionStore>>(
            PrefName.SubscriptionNotificationStore,
            null
        ) ?: listOf()

        binding.followFilterButton.setOnClickListener {
            val dialogView = LayoutInflater.from(currContext()).inflate(R.layout.custom_dialog_layout, null)
            val checkboxContainer = dialogView.findViewById<LinearLayout>(R.id.checkboxContainer)
            val tickAllButton = dialogView.findViewById<ImageButton>(R.id.toggleButton)
            val title = dialogView.findViewById<TextView>(R.id.scantitle)
            title.visibility = ViewGroup.GONE
            fun getToggleImageResource(container: ViewGroup): Int {
                var allChecked = true
                var allUnchecked = true

                for (i in 0 until container.childCount) {
                    val checkBox = container.getChildAt(i) as CheckBox
                    if (!checkBox.isChecked) {
                        allChecked = false
                    } else {
                        allUnchecked = false
                    }
                }
                return when {
                    allChecked -> R.drawable.untick_all_boxes
                    allUnchecked -> R.drawable.tick_all_boxes
                    else -> R.drawable.invert_all_boxes
                }
            }
            NotificationType.entries.forEach { notificationType ->
                val checkBox = CheckBox(currContext())
                checkBox.text = notificationType.toFormattedString()
                checkBox.isChecked = !filters.contains(notificationType.value.fromFormattedString())
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        filters.remove(notificationType.value.fromFormattedString())
                    } else {
                        filters.add(notificationType.value.fromFormattedString())
                    }
                    tickAllButton.setImageResource(getToggleImageResource(checkboxContainer))
                }
                checkboxContainer.addView(checkBox)
            }
            tickAllButton.setImageResource(getToggleImageResource(checkboxContainer))
            tickAllButton.setOnClickListener {
                for (i in 0 until checkboxContainer.childCount) {
                    val checkBox = checkboxContainer.getChildAt(i) as CheckBox
                    checkBox.isChecked = !checkBox.isChecked
                }

                tickAllButton.setImageResource(getToggleImageResource(checkboxContainer))
            }
            val alertD = AlertDialog.Builder(this, R.style.MyPopup)
            alertD.setTitle("Filter")
            alertD.setView(dialogView)
            alertD.setPositiveButton("OK") { _, _ ->
                currentPage = 1
                hasNextPage = true
                adapter.clear()
                adapter.addAll(notificationList.filter { notification ->
                    !filters.contains(notification.notificationType)
                }.map {
                    NotificationItem(
                        it,
                        ::onNotificationClick
                    )
                })
                loadPage(-1) {
                    binding.followRefresh.visibility = ViewGroup.GONE
                }
            }
            alertD.setNegativeButton("Cancel") { _, _ -> }
            val dialog = alertD.show()
            dialog.window?.setDimAmount(0.8f)
        }

        val activityId = intent.getIntExtra("activityId", -1)
        lifecycleScope.launch {
            loadPage(activityId) {
                binding.listProgressBar.visibility = ViewGroup.GONE
            }
            withContext(Dispatchers.Main) {
                binding.listProgressBar.visibility = ViewGroup.GONE
                binding.listRecyclerView.setOnTouchListener { _, event ->
                    if (event?.action == MotionEvent.ACTION_UP) {
                        if (hasNextPage && !binding.listRecyclerView.canScrollVertically(1) && !binding.followRefresh.isVisible
                            && binding.listRecyclerView.adapter!!.itemCount != 0 &&
                            (binding.listRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() == (binding.listRecyclerView.adapter!!.itemCount - 1)
                        ) {
                            binding.followRefresh.visibility = ViewGroup.VISIBLE
                            loadPage(-1) {
                                binding.followRefresh.visibility = ViewGroup.GONE
                            }
                        }
                    }
                    false
                }

                binding.followSwipeRefresh.setOnRefreshListener {
                    currentPage = 1
                    hasNextPage = true
                    adapter.clear()
                    notificationList = emptyList()
                    loadPage(-1) {
                        binding.followSwipeRefresh.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun loadPage(activityId: Int, onFinish: () -> Unit = {}) {
        lifecycleScope.launch(Dispatchers.IO) {
            val resetNotification = activityId == -1
            val res = Anilist.query.getNotifications(
                Anilist.userid ?: PrefManager.getVal<String>(PrefName.AnilistUserId).toIntOrNull()
                ?: 0, currentPage, resetNotification = resetNotification
            )
            withContext(Dispatchers.Main) {
                val newNotifications: MutableList<Notification> = mutableListOf()
                res?.data?.page?.notifications?.let { notifications ->
                    Logger.log("Notifications: $notifications")
                    newNotifications += if (activityId != -1) {
                        notifications.filter { it.id == activityId }
                    } else {
                        notifications
                    }.toMutableList()
                }
                if (activityId == -1) {
                    val furthestTime = newNotifications.minOfOrNull { it.createdAt } ?: 0
                    commentStore.forEach {
                        if ((it.time > furthestTime * 1000L || !hasNextPage) && notificationList.none { notification ->
                                notification.commentId == it.commentId && notification.createdAt == (it.time / 1000L).toInt()
                            }) {
                            val notification = Notification(
                                it.type.toString(),
                                System.currentTimeMillis().toInt(),
                                commentId = it.commentId,
                                notificationType = it.type.toString(),
                                mediaId = it.mediaId,
                                context = it.title + "\n" + it.content,
                                createdAt = (it.time / 1000L).toInt(),
                            )
                            newNotifications += notification
                        }
                    }
                    subscriptionStore.forEach {
                        if ((it.time > furthestTime * 1000L || !hasNextPage) && notificationList.none { notification ->
                                notification.mediaId == it.mediaId && notification.createdAt == (it.time / 1000L).toInt()
                            }) {
                            val notification = Notification(
                                it.type,
                                System.currentTimeMillis().toInt(),
                                commentId = it.mediaId,
                                mediaId = it.mediaId,
                                notificationType = it.type,
                                context = it.content,
                                createdAt = (it.time / 1000L).toInt(),
                            )
                            newNotifications += notification
                        }
                    }
                    newNotifications.sortByDescending { it.createdAt }
                }

                notificationList += newNotifications
                adapter.addAll(newNotifications.filter { notification ->
                    !filters.contains(notification.notificationType)
                }.map {
                    NotificationItem(
                        it,
                        ::onNotificationClick
                    )
                })
                currentPage = res?.data?.page?.pageInfo?.currentPage?.plus(1) ?: 1
                hasNextPage = res?.data?.page?.pageInfo?.hasNextPage ?: false
                binding.followSwipeRefresh.isRefreshing = false
                onFinish()
            }
        }
    }

    private fun onNotificationClick(id: Int, optional: Int?, type: NotificationClickType) {
        when (type) {
            NotificationClickType.USER -> {
                ContextCompat.startActivity(
                    this, Intent(this, ProfileActivity::class.java)
                        .putExtra("userId", id), null
                )
            }

            NotificationClickType.MEDIA -> {
                ContextCompat.startActivity(
                    this, Intent(this, MediaDetailsActivity::class.java)
                        .putExtra("mediaId", id), null
                )
            }

            NotificationClickType.ACTIVITY -> {
                ContextCompat.startActivity(
                    this, Intent(this, FeedActivity::class.java)
                        .putExtra("activityId", id), null
                )
            }

            NotificationClickType.COMMENT -> {
                ContextCompat.startActivity(
                    this, Intent(this, MediaDetailsActivity::class.java)
                        .putExtra("FRAGMENT_TO_LOAD", "COMMENTS")
                        .putExtra("mediaId", id)
                        .putExtra("commentId", optional ?: -1),
                    null
                )

            }

            NotificationClickType.UNDEFINED -> {
                // Do nothing
            }
        }
    }

    companion object {
        enum class NotificationClickType {
            USER, MEDIA, ACTIVITY, COMMENT, UNDEFINED
        }
    }
}