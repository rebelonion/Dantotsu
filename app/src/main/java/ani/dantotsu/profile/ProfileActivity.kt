package ani.dantotsu.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.ActivityProfileBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar


class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var selected: Int = 0
    private lateinit var navBar: AnimatedBottomBar

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navBar = binding.profileNavBar
        navBar.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = navBarHeight }
        val profileTab = navBar.createTab(R.drawable.ic_round_person_24, "Profile")
        val statsTab = navBar.createTab(R.drawable.ic_stats_24, "Stats")
        navBar.addTab(profileTab)
        navBar.addTab(statsTab)
        navBar.visibility = View.GONE
        binding.profileViewPager.isUserInputEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            val userid = intent.getIntExtra("userId", 0)
            val respond = Anilist.query.getUserProfile(userid)
            val user = respond?.data?.user
            if (user == null) {
                toast("User not found")
                finish()
                return@launch
            }
            withContext(Dispatchers.Main) {
                binding.profileViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = navBarHeight
                }
                binding.profileViewPager.adapter =
                    ViewPagerAdapter(supportFragmentManager, lifecycle, user)
                navBar.visibility = View.VISIBLE
                navBar.selectTabAt(selected)
                navBar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
                    override fun onTabSelected(
                        lastIndex: Int,
                        lastTab: AnimatedBottomBar.Tab?,
                        newIndex: Int,
                        newTab: AnimatedBottomBar.Tab
                    ) {
                        selected = newIndex
                        binding.profileViewPager.setCurrentItem(selected, true)
                    }
                })
                val userLevel = intent.getStringExtra("username") ?: ""
                binding.followButton.visibility =
                    if (user.id == Anilist.userid || Anilist.userid == null) View.GONE else View.VISIBLE
                binding.followButton.text =
                    if (user.isFollowing) "Unfollow" else if (user.isFollower) "Follows you" else "Follow"
                if (user.isFollowing && user.isFollower) binding.followButton.text = "Mutual"
                binding.followButton.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val res = Anilist.query.toggleFollow(user.id)
                        if (res?.data?.toggleFollow != null) {
                            withContext(Dispatchers.Main) {
                                snackString("Success")
                                user.isFollowing = res.data.toggleFollow.isFollowing
                                binding.followButton.text =
                                    if (user.isFollowing) "Unfollow" else if (user.isFollower) "Follows you" else "Follow"
                                if (user.isFollowing && user.isFollower) binding.followButton.text =
                                    "Mutual"
                            }
                        }
                    }
                }
                binding.profileProgressBar.visibility = View.GONE
                binding.profileTopContainer.visibility = View.VISIBLE

                binding.profileMenuButton.setOnClickListener {
                    val popup = PopupMenu(this@ProfileActivity, binding.profileMenuButton)
                    popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_view_following -> {
                                ContextCompat.startActivity(
                                    this@ProfileActivity,
                                    Intent(this@ProfileActivity, FollowActivity::class.java)
                                        .putExtra("title", "Following")
                                        .putExtra("userId", user.id),
                                    null
                                )
                                true
                            }
                            R.id.action_view_followers -> {
                                ContextCompat.startActivity(
                                    this@ProfileActivity,
                                    Intent(this@ProfileActivity, FollowActivity::class.java)
                                        .putExtra("title", "Followers")
                                        .putExtra("userId", user.id),
                                    null
                                )
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }

                binding.profileUserAvatar.loadImage(user.avatar?.medium)
                binding.profileUserAvatar.setOnLongClickListener {
                    ImageViewDialog.newInstance(
                        this@ProfileActivity,
                        "${user.name}'s [Avatar]",
                        user.avatar?.medium
                    )
                }

                binding.profileUserName.text = "${user.name} $userLevel"
                if (!(PrefManager.getVal(PrefName.BannerAnimations) as Boolean)) binding.profileBannerImage.pause()
                binding.profileBannerImage.loadImage(user.bannerImage)
                binding.profileBannerImage.updateLayoutParams { height += statusBarHeight }
                binding.profileBannerGradient.updateLayoutParams { height += statusBarHeight }
                binding.profileMenuButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin += statusBarHeight
                }
                binding.profileBannerImage.setOnLongClickListener {
                    ImageViewDialog.newInstance(
                        this@ProfileActivity,
                        user.name + " [Banner]",
                        user.bannerImage
                    )
                }

            }
        }
    }

    override fun onResume() {
        if (this::navBar.isInitialized) {
            navBar.selectTabAt(selected)
        }
        super.onResume()
    }

    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val user: Query.UserProfile
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ProfileFragment.newInstance(user)
            1 -> StatsFragment.newInstance(user)
            else -> ProfileFragment.newInstance(user)
        }
    }
}