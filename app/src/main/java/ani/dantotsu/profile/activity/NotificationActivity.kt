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
import ani.dantotsu.connections.anilist.AnilistQueries
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var notificationList: List<Notification> = emptyList()
    private var page: Int = 1

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.listTitle.text = "Notifications"
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = statusBarHeight }
        binding.listFrameLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = navBarHeight }
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.followerGrid.visibility = ViewGroup.GONE
        binding.followerList.visibility = ViewGroup.GONE
        binding.listBack.setOnClickListener {
            onBackPressed()
        }
        binding.listProgressBar.visibility = ViewGroup.VISIBLE
        val activityId = intent.getIntExtra("activityId", -1)
        lifecycleScope.launch {
            val resetNotification = activityId == -1
            val res = Anilist.query.getNotifications(Anilist.userid?:0, resetNotification = resetNotification)
            res?.data?.page?.notifications?.let { notifications ->
                notificationList = if (activityId != -1) {
                    notifications.filter { it.id == activityId }
                } else {
                    notifications
                }
                adapter.update(notificationList.map { NotificationItem(it, ::onNotificationClick) })
            }
            withContext(Dispatchers.Main){
                binding.listProgressBar.visibility = ViewGroup.GONE
                binding.listRecyclerView.setOnTouchListener { _, event ->
                    if (event?.action == MotionEvent.ACTION_UP) {
                        if (adapter.itemCount % AnilistQueries.ITEMS_PER_PAGE != 0) {
                            snackString("No more notifications")
                        } else if (!binding.listRecyclerView.canScrollVertically(1) && !binding.followRefresh.isVisible
                            && binding.listRecyclerView.adapter!!.itemCount != 0 &&
                            (binding.listRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() == (binding.listRecyclerView.adapter!!.itemCount - 1)
                        ) {
                            page++
                            binding.followRefresh.visibility = ViewGroup.VISIBLE
                            lifecycleScope.launch(Dispatchers.IO) {
                                val res = Anilist.query.getNotifications(Anilist.userid?:0, page)
                                withContext(Dispatchers.Main) {
                                    res?.data?.page?.notifications?.let { notifications ->
                                        notificationList += notifications
                                        adapter.addAll(notifications.map { NotificationItem(it, ::onNotificationClick) })
                                    }
                                    binding.followRefresh.visibility = ViewGroup.GONE
                                }
                            }
                        }
                    }
                    false
                }
            }
        }
    }

    private fun onNotificationClick(id: Int, type: NotificationClickType) {
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
            NotificationClickType.UNDEFINED -> {
                // Do nothing
            }
        }
    }

    companion object {
        enum class NotificationClickType {
            USER, MEDIA, ACTIVITY, UNDEFINED
        }
    }
}