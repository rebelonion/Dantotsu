package ani.dantotsu.profile

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.ActivityProfileBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.profile.activity.FeedFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar
import kotlin.math.abs


class ProfileActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {
    lateinit var binding: ActivityProfileBinding
    private var selected: Int = 0
    private lateinit var navBar: AnimatedBottomBar

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        screenWidth = resources.displayMetrics.widthPixels.toFloat()
        navBar = binding.profileNavBar
        val navBarRightMargin = if (resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE) navBarHeight else 0
        val navBarBottomMargin = if (resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE) 0 else navBarHeight
        navBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            rightMargin = navBarRightMargin
            bottomMargin = navBarBottomMargin
        }
        val feedTab = navBar.createTab(R.drawable.ic_round_filter_24, "Feed")
        val profileTab = navBar.createTab(R.drawable.ic_round_person_24, "Profile")
        val statsTab = navBar.createTab(R.drawable.ic_stats_24, "Stats")
        navBar.addTab(profileTab)
        navBar.addTab(feedTab)
        navBar.addTab(statsTab)
        navBar.visibility = View.GONE
        binding.profileViewPager.isUserInputEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val userid = intent.getIntExtra("userId", -1)
            val username = intent.getStringExtra("username") ?: ""
            val respond =
                if (userid != -1) Anilist.query.getUserProfile(userid) else
                    Anilist.query.getUserProfile(username)
            val user = respond?.data?.user
            if (user == null) {
                toast("User not found")
                finish()
                return@launch
            }
            val following = respond.data.followingPage?.pageInfo?.total ?: 0
            val followers = respond.data.followerPage?.pageInfo?.total ?: 0
            withContext(Dispatchers.Main) {
                binding.profileViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = navBarHeight
                }
                binding.profileViewPager.adapter =
                    ViewPagerAdapter(supportFragmentManager, lifecycle, user)
                binding.profileViewPager.setOffscreenPageLimit(3)
                binding.profileViewPager.setCurrentItem(selected, false)
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
                val userLevel = intent.getStringExtra("userLVL") ?: ""
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
                binding.profileAppBar.visibility = View.VISIBLE
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

                            R.id.action_view_on_anilist -> {
                                openLinkInBrowser("https://anilist.co/user/${user.name}")
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
                blurImage(binding.profileBannerImage, user.bannerImage ?: user.avatar?.medium)
                binding.profileBannerImage.updateLayoutParams { height += statusBarHeight }
                binding.profileBannerGradient.updateLayoutParams { height += statusBarHeight }
                binding.profileMenuButton.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
                binding.profileButtonContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
                binding.profileBannerImage.setOnLongClickListener {
                    ImageViewDialog.newInstance(
                        this@ProfileActivity,
                        user.name + " [Banner]",
                        user.bannerImage
                    )
                }

                mMaxScrollSize = binding.profileAppBar.totalScrollRange
                binding.profileAppBar.addOnOffsetChangedListener(this@ProfileActivity)


                binding.profileFollowerCount.text = followers.toString()
                binding.profileFollowerCountContainer.setOnClickListener {
                    ContextCompat.startActivity(
                        this@ProfileActivity,
                        Intent(this@ProfileActivity, FollowActivity::class.java)
                            .putExtra("title", "Followers")
                            .putExtra("userId", user.id),
                        null
                    )
                }

                binding.profileFollowingCount.text = following.toString()
                binding.profileFollowingCountContainer.setOnClickListener {
                    ContextCompat.startActivity(
                        this@ProfileActivity,
                        Intent(this@ProfileActivity, FollowActivity::class.java)
                            .putExtra("title", "Following")
                            .putExtra("userId", user.id),
                        null
                    )
                }

                binding.profileAnimeCount.text = user.statistics.anime.count.toString()
                binding.profileAnimeCountContainer.setOnClickListener {
                    ContextCompat.startActivity(
                        this@ProfileActivity, Intent(this@ProfileActivity, ListActivity::class.java)
                            .putExtra("anime", true)
                            .putExtra("userId", user.id)
                            .putExtra("username", user.name), null
                    )
                }

                binding.profileMangaCount.text = user.statistics.manga.count.toString()
                binding.profileMangaCountContainer.setOnClickListener {
                    ContextCompat.startActivity(
                        this@ProfileActivity, Intent(this@ProfileActivity, ListActivity::class.java)
                            .putExtra("anime", false)
                            .putExtra("userId", user.id)
                            .putExtra("username", user.name), null
                    )
                }
            }
        }
    }

    //Collapsing UI Stuff
    private var isCollapsed = false
    private val percent = 65
    private var mMaxScrollSize = 0
    private var screenWidth: Float = 0f

    override fun onOffsetChanged(appBar: AppBarLayout, i: Int) {
        if (mMaxScrollSize == 0) mMaxScrollSize = appBar.totalScrollRange
        val percentage = abs(i) * 100 / mMaxScrollSize

        binding.profileUserAvatarContainer.visibility =
            if (binding.profileUserAvatarContainer.scaleX == 0f) View.GONE else View.VISIBLE
        val duration = (200 * (PrefManager.getVal(PrefName.AnimationSpeed) as Float)).toLong()
        val typedValue = TypedValue()
        this@ProfileActivity.theme.resolveAttribute(
            com.google.android.material.R.attr.colorSecondary,
            typedValue,
            true
        )
        val color = typedValue.data
        if (percentage >= percent && !isCollapsed) {
            isCollapsed = true
            ObjectAnimator.ofFloat(binding.profileUserDataContainer, "translationX", screenWidth)
                .setDuration(duration).start()
            ObjectAnimator.ofFloat(binding.profileUserAvatarContainer, "translationX", screenWidth)
                .setDuration(duration).start()
            ObjectAnimator.ofFloat(binding.profileButtonContainer, "translationX", screenWidth)
                .setDuration(duration).start()
            binding.profileBannerImage.pause()
        }
        if (percentage <= percent && isCollapsed) {
            isCollapsed = false
            ObjectAnimator.ofFloat(binding.profileUserDataContainer, "translationX", 0f)
                .setDuration(duration).start()
            ObjectAnimator.ofFloat(binding.profileUserAvatarContainer, "translationX", 0f)
                .setDuration(duration).start()
            ObjectAnimator.ofFloat(binding.profileButtonContainer, "translationX", 0f)
                .setDuration(duration).start()

            if (PrefManager.getVal(PrefName.BannerAnimations)) binding.profileBannerImage.resume()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val rightMargin = if (resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE) navBarHeight else 0
        val bottomMargin = if (resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE) 0 else navBarHeight
        val params : ViewGroup.MarginLayoutParams =
            navBar.layoutParams as ViewGroup.MarginLayoutParams
        params.updateMargins(right = rightMargin, bottom = bottomMargin)
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

        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ProfileFragment.newInstance(user)
            1 -> FeedFragment.newInstance(user.id, false, -1)
            2 -> StatsFragment.newInstance(user)
            else -> ProfileFragment.newInstance(user)
        }
    }
}
