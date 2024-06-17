package ani.dantotsu.media.user

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.ActivityListBinding
import ani.dantotsu.getThemeColor
import ani.dantotsu.hideSystemBarsExtendView
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding
    private val scope = lifecycleScope
    private var selectedTabIdx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityListBinding.inflate(layoutInflater)

        val primaryColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
        val primaryTextColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val secondaryTextColor = getThemeColor(com.google.android.material.R.attr.colorOutline)

        window.statusBarColor = primaryColor
        window.navigationBarColor = primaryColor
        binding.listed.visibility = View.GONE
        binding.listTabLayout.setBackgroundColor(primaryColor)
        binding.listAppBar.setBackgroundColor(primaryColor)
        binding.listTitle.setTextColor(primaryTextColor)
        binding.listTabLayout.setTabTextColors(secondaryTextColor, primaryTextColor)
        binding.listTabLayout.setSelectedTabIndicatorColor(primaryTextColor)
        if (!PrefManager.getVal<Boolean>(PrefName.ImmersiveMode)) {
            this.window.statusBarColor =
                ContextCompat.getColor(this, R.color.nav_bg_inv)
            binding.root.fitsSystemWindows = true

        } else {
            binding.root.fitsSystemWindows = false
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            hideSystemBarsExtendView()
            binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
            }
        }
        setContentView(binding.root)

        val anime = intent.getBooleanExtra("anime", true)
        binding.listTitle.text = getString(
            R.string.user_list, intent.getStringExtra("username"),
            if (anime) getString(R.string.anime) else getString(R.string.manga)
        )
        binding.listTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                this@ListActivity.selectedTabIdx = tab?.position ?: 0
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        val model: ListViewModel by viewModels()
        model.getLists().observe(this) {
            val defaultKeys = listOf(
                "Reading",
                "Watching",
                "Completed",
                "Paused",
                "Dropped",
                "Planning",
                "Favourites",
                "Rewatching",
                "Rereading",
                "All"
            )
            val userKeys: Array<String> = resources.getStringArray(R.array.keys)

            if (it != null) {
                binding.listProgressBar.visibility = View.GONE
                binding.listViewPager.adapter = ListViewPagerAdapter(it.size, false, this)
                val keys = it.keys.toList()
                    .map { key -> userKeys.getOrNull(defaultKeys.indexOf(key)) ?: key }
                val values = it.values.toList()
                val savedTab = this.selectedTabIdx
                TabLayoutMediator(binding.listTabLayout, binding.listViewPager) { tab, position ->
                    tab.text = "${keys[position]} (${values[position].size})"
                }.attach()
                binding.listViewPager.setCurrentItem(savedTab, false)
            }
        }

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        model.loadLists(
                            anime,
                            intent.getIntExtra("userId", 0)
                        )
                    }
                    live.postValue(false)
                }
            }
        }

        binding.listSort.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.setOnMenuItemClickListener { item ->
                val sort = when (item.itemId) {
                    R.id.score -> "score"
                    R.id.title -> "title"
                    R.id.updated -> "updatedAt"
                    R.id.release -> "release"
                    else -> null
                }
                PrefManager.setVal(
                    if (anime) PrefName.AnimeListSortOrder else PrefName.MangaListSortOrder,
                    sort ?: ""
                )
                binding.listProgressBar.visibility = View.VISIBLE
                binding.listViewPager.adapter = null
                scope.launch {
                    withContext(Dispatchers.IO) {
                        model.loadLists(
                            anime,
                            intent.getIntExtra("userId", 0),
                            sort
                        )
                    }
                }
                true
            }
            popup.inflate(R.menu.list_sort_menu)
            popup.show()
        }

        binding.filter.setOnClickListener {
            val genres =
                PrefManager.getVal<Set<String>>(PrefName.GenresList).toMutableSet().sorted()
            val popup = PopupMenu(this, it)
            popup.menu.add("All")
            genres.forEach { genre ->
                popup.menu.add(genre)
            }
            popup.setOnMenuItemClickListener { menuItem ->
                val selectedGenre = menuItem.title.toString()
                model.filterLists(selectedGenre)
                true
            }
            popup.show()
        }

        binding.random.setOnClickListener {
            //get the current tab
            val currentTab =
                binding.listTabLayout.getTabAt(binding.listTabLayout.selectedTabPosition)
            val currentFragment =
                supportFragmentManager.findFragmentByTag("f" + currentTab?.position.toString()) as? ListFragment
            currentFragment?.randomOptionClick()
        }

        binding.search.setOnClickListener {
            toggleSearchView(binding.searchView.isVisible)
            if (!binding.searchView.isVisible) {
                model.unfilterLists()
            }
        }

        binding.searchViewText.addTextChangedListener {
            model.searchLists(binding.searchViewText.text.toString())
        }
    }

    private fun toggleSearchView(isVisible: Boolean) {
        if (isVisible) {
            binding.searchView.visibility = View.GONE
            binding.searchViewText.text.clear()
        } else {
            binding.searchView.visibility = View.VISIBLE
            binding.searchViewText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.searchViewText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}
