package ani.dantotsu.profile.notification

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityNotificationBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.profile.notification.NotificationFragment.Companion.NotificationType
import nl.joery.animatedbottombar.AnimatedBottomBar

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private var selected: Int = 0
    lateinit var navBar: AnimatedBottomBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.notificationTitle.text = getString(R.string.notifications)
        binding.notificationToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }
        navBar = binding.notificationNavBar
        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        val tabs = listOf(
            Pair(R.drawable.ic_round_movie_filter_24, "Media"),
            Pair(R.drawable.ic_round_person_24, "User"),
            Pair(R.drawable.ic_round_notifications_active_24, "Subs"),
            Pair(R.drawable.ic_round_comment_24, "Comments")
        )
        tabs.forEach { (icon, title) -> navBar.addTab(navBar.createTab(icon, title)) }

        binding.notificationBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        val getOne = intent.getIntExtra("activityId", -1)
        if (getOne != -1) navBar.isVisible = false
        binding.notificationViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle, getOne)
        binding.notificationViewPager.setOffscreenPageLimit(4)
        binding.notificationViewPager.setCurrentItem(selected, false)
        navBar.selectTabAt(selected)
        navBar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                selected = newIndex
                binding.notificationViewPager.setCurrentItem(selected, true)
            }
        })
        binding.notificationViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                navBar.selectTabAt(position)
            }
        })
    }
    override fun onResume() {
        super.onResume()
        if (this::navBar.isInitialized) {
            navBar.selectTabAt(selected)
        }
    }
    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        val id: Int = -1
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = if (id != -1) 1 else 4

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> NotificationFragment.newInstance(if (id != -1) NotificationType.ONE else NotificationType.MEDIA, id)
            1 -> NotificationFragment.newInstance(NotificationType.USER)
            2 -> NotificationFragment.newInstance(NotificationType.SUBSCRIPTION)
            3 -> NotificationFragment.newInstance(NotificationType.COMMENT)
            else -> NotificationFragment.newInstance(NotificationType.MEDIA)
        }
    }
}
