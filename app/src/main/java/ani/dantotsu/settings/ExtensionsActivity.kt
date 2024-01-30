package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.os.Build.*
import android.os.Build.VERSION.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.*
import ani.dantotsu.databinding.ActivityExtensionsBinding
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ExtensionsActivity : AppCompatActivity() {
    lateinit var binding: ActivityExtensionsBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ThemeManager(this).applyTheme()
        binding = ActivityExtensionsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.offscreenPageLimit = 1

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 6

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> InstalledAnimeExtensionsFragment()
                    1 -> AnimeExtensionsFragment()
                    2 -> InstalledMangaExtensionsFragment()
                    3 -> MangaExtensionsFragment()
                    4 -> InstalledNovelExtensionsFragment()
                    5 -> NovelExtensionsFragment()
                    else -> AnimeExtensionsFragment()
                }
            }

        }

        val searchView: AutoCompleteTextView = findViewById(R.id.searchViewText)

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    searchView.setText("")
                    searchView.clearFocus()
                    tabLayout.clearFocus()
                    viewPager.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    viewPager.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    tabLayout.clearFocus()
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    viewPager.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    // Do nothing
                }
            }
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Installed Anime"
                1 -> "Available Anime"
                2 -> "Installed Manga"
                3 -> "Available Manga"
                4 -> "Installed Novels"
                5 -> "Available Novels"
                else -> null
            }
        }.attach()


        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentFragment =
                    supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
                if (currentFragment is SearchQueryHandler) {
                    currentFragment.updateContentBasedOnQuery(s?.toString()?.trim())
                }
            }
        })


        initActivity(this)
        binding.languageselect.visibility = View.GONE
        /* TODO
                binding.languageselect.setOnClickListener {
                    val popup = PopupMenu(this, it)
                    popup.inflate(R.menu.launguage_selector_menu)
                    popup.setOnMenuItemClickListener { menuItem ->
                        true
                    }
                    popup.setOnDismissListener {
                    }
                    popup.show()
                }*/
        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }


    }

}

interface SearchQueryHandler {
    fun updateContentBasedOnQuery(query: String?)
}
