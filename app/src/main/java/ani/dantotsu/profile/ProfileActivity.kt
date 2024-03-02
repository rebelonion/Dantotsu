package ani.dantotsu.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar


class ProfileActivity : AppCompatActivity(){
    private lateinit var binding: ActivityProfileBinding
    private var selected: Int = 0
    private lateinit var tabLayout: AnimatedBottomBar
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tabLayout = binding.typeTab
        val profileTab = tabLayout.createTab(R.drawable.ic_round_person_24, "Profile")
        val statsTab = tabLayout.createTab(R.drawable.ic_stats_24, "Stats")
        tabLayout.addTab(profileTab)
        tabLayout.addTab(statsTab)
        tabLayout.visibility = View.GONE
        binding.mediaViewPager.isUserInputEnabled = false


        lifecycleScope.launch(Dispatchers.IO) {
            val userid = intent.getIntExtra("userId", 0)
            val respond = Anilist.query.getUserProfile(userid)
            val user = respond?.data?.user
            if (user == null) {
                snackString("User not found")
                finish()
                return@launch
            }
            withContext(Dispatchers.Main) {
                binding.mediaViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle, user, this@ProfileActivity)
                tabLayout.visibility = View.VISIBLE
                tabLayout.selectTabAt(selected)
                tabLayout.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
                    override fun onTabSelected(
                        lastIndex: Int,
                        lastTab: AnimatedBottomBar.Tab?,
                        newIndex: Int,
                        newTab: AnimatedBottomBar.Tab
                    ) {
                        selected = newIndex
                        binding.mediaViewPager.setCurrentItem(selected, true)
                    }
                })
                val userLevel =  intent.getStringExtra("username")?: ""

                binding.profileProgressBar.visibility = View.GONE
                binding.profileBannerImage.loadImage(user.bannerImage)
                binding.profileUserAvatar.loadImage(user.avatar?.medium)
                binding.profileUserName.text = "${user.name} $userLevel"
                binding.profileUserEpisodesWatched.text = user.statistics.anime.episodesWatched.toString()
                binding.profileUserChaptersRead.text = user.statistics.manga.chaptersRead.toString()

                binding.profileBannerImage.loadImage(user.bannerImage)
                binding.profileBannerImage.setOnLongClickListener {
                    ImageViewDialog.newInstance(
                        this@ProfileActivity,
                        user.name + " [Banner]",
                        user.bannerImage
                    )
                }
                binding.profileUserAvatar.loadImage(user.avatar?.medium)
                binding.profileUserAvatar.setOnLongClickListener {
                    ImageViewDialog.newInstance(
                        this@ProfileActivity,
                        user.name + " [Avatar]",
                        user.avatar?.medium
                    )
                }
            }
        }

    }

    override fun onResume() {
        if (this::tabLayout.isInitialized) {
            tabLayout.selectTabAt(selected)
        }
        super.onResume()
    }

    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val user: Query.UserProfile,
        private val activity: ProfileActivity
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ProfileFragment(user, activity)
            1 -> StatsFragment(user, activity)
            else -> ProfileFragment(user, activity)
        }
    }
}