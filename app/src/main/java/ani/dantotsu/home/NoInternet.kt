package ani.dantotsu.home

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.R
import ani.dantotsu.ZoomOutPageTransformer
import ani.dantotsu.databinding.ActivityNoInternetBinding
import ani.dantotsu.download.manga.OfflineMangaFragment
import ani.dantotsu.initActivity
import ani.dantotsu.loadData
import ani.dantotsu.navBarHeight
import ani.dantotsu.offline.OfflineFragment
import ani.dantotsu.others.LangSet
import ani.dantotsu.selectedOption
import ani.dantotsu.settings.UserInterfaceSettings
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import nl.joery.animatedbottombar.AnimatedBottomBar

class NoInternet : AppCompatActivity() {
    private lateinit var binding: ActivityNoInternetBinding
    lateinit var bottomBar: AnimatedBottomBar
    private var uiSettings = UserInterfaceSettings()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LangSet.setLocale(this)
        ThemeManager(this).applyTheme()

        binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val _bottomBar = findViewById<AnimatedBottomBar>(R.id.navbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val backgroundDrawable = _bottomBar.background as GradientDrawable
            val currentColor = backgroundDrawable.color?.defaultColor ?: 0
            val semiTransparentColor = (currentColor and 0x00FFFFFF) or 0xE8000000.toInt()
            backgroundDrawable.setColor(semiTransparentColor)
            _bottomBar.background = backgroundDrawable
        }
        val colorOverflow = this.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            .getBoolean("colorOverflow", false)
        if (!colorOverflow) {
            _bottomBar.background = ContextCompat.getDrawable(this, R.drawable.bottom_nav_gray)

        }

        var doubleBackToExitPressedOnce = false
        onBackPressedDispatcher.addCallback(this) {
            if (doubleBackToExitPressedOnce) {
                finishAffinity()
            }
            doubleBackToExitPressedOnce = true
            snackString(this@NoInternet.getString(R.string.back_to_exit))
            Handler(Looper.getMainLooper()).postDelayed(
                { doubleBackToExitPressedOnce = false },
                2000
            )
        }

        binding.root.doOnAttach {
            initActivity(this)
            uiSettings = loadData("ui_settings") ?: uiSettings
            selectedOption = uiSettings.defaultStartUpTab
            binding.includedNavbar.navbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight
            }
        }
        val navbar = binding.includedNavbar.navbar
        ani.dantotsu.bottomBar = navbar
        navbar.visibility = View.VISIBLE
        val mainViewPager = binding.viewpager
        mainViewPager.isUserInputEnabled = false
        mainViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        mainViewPager.setPageTransformer(ZoomOutPageTransformer(uiSettings))
        navbar.setOnTabSelectListener(object :
            AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                navbar.animate().translationZ(12f).setDuration(200).start()
                selectedOption = newIndex
                mainViewPager.setCurrentItem(newIndex, false)
            }
        })
        navbar.selectTabAt(selectedOption)

        //supportFragmentManager.beginTransaction().replace(binding.fragmentContainer.id, OfflineFragment()).commit()

    }

    private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return OfflineFragment()
                1 -> return OfflineFragment()
                2 -> return OfflineMangaFragment()
            }
            return LoginFragment()
        }
    }
}