package ani.dantotsu.profile.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.notifications.comment.CommentStore
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var notificationList: List<Notification> = emptyList()
    private var currentPage: Int = 1
    private var hasNextPage: Boolean = true

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.listTitle.text = "Notifications"
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
            onBackPressed()
        }
        binding.listProgressBar.visibility = ViewGroup.VISIBLE
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
                if (activityId == -1 && currentPage == 1) {
                    val commentStore = PrefManager.getNullableVal<List<CommentStore>>(
                        PrefName.CommentNotificationStore,
                        null
                    ) ?: listOf()
                    commentStore.forEach {
                        val notification = Notification(
                            "COMMENT_REPLY",
                            System.currentTimeMillis().toInt(),
                            commentId = it.commentId,
                            notificationType = "COMMENT_REPLY",
                            mediaId = it.mediaId,
                            context = it.title + "\n" + it.content,
                            createdAt = (it.time / 1000L).toInt(),
                        )
                        newNotifications += notification
                    }
                    newNotifications.sortByDescending { it.createdAt }
                }

                notificationList += newNotifications
                adapter.addAll(newNotifications.map {
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