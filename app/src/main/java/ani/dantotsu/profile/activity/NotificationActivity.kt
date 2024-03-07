package ani.dantotsu.profile.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var notificationList: List<Notification> = emptyList()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.listTitle.text = "Notifications"
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = statusBarHeight }
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.followerGrid.visibility = ViewGroup.GONE
        binding.followerList.visibility = ViewGroup.GONE
        binding.listBack.setOnClickListener {
            onBackPressed()
        }

        lifecycleScope.launch {
            val res = Anilist.query.getNotifications(Anilist.userid?:0)
            res?.data?.page?.notifications?.let { notifications ->
                notificationList = notifications
                adapter.update(notificationList.map { NotificationItem(it, ::onNotificationClick) })
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
            NotificationClickType.UNDEFINED -> {
                // Do nothing
            }
        }
    }

    companion object {
        enum class NotificationClickType {
            USER, MEDIA, UNDEFINED
        }
    }
}