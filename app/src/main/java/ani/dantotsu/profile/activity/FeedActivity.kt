package ani.dantotsu.profile.activity

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityFeedBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import nl.joery.animatedbottombar.AnimatedBottomBar

class FeedActivity: AppCompatActivity() {
    private lateinit var binding: ActivityFeedBinding
    private var selected: Int = 0
    private lateinit var navBar: AnimatedBottomBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navBar = binding.feedNavBar
        navBar.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }
        val personalTab = navBar.createTab(R.drawable.ic_round_person_24, "Following")
        val globalTab = navBar.createTab(R.drawable.ic_globe_24, "Global")
        navBar.addTab(personalTab)
        navBar.addTab(globalTab)
        binding.listTitle.text = "Activities"
        binding.feedViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin += navBarHeight
            topMargin += statusBarHeight
        }
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.feedViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        binding.feedViewPager.setCurrentItem(selected, false)
        binding.feedViewPager.isUserInputEnabled = false
        navBar.selectTabAt(selected)
        navBar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                selected = newIndex
                binding.feedViewPager.setCurrentItem(selected, true)
            }
        })
        binding.listBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        navBar.selectTabAt(selected)
    }


    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FeedFragment.newInstance(null, false)
                else -> FeedFragment.newInstance(null, true)
            }
        }
    }
}