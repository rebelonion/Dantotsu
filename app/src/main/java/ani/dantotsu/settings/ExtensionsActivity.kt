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
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.*
import ani.dantotsu.databinding.ActivityExtensionsBinding
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.others.LangSet
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtensionsActivity : AppCompatActivity()  {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@ExtensionsActivity)
    }
    lateinit var binding: ActivityExtensionsBinding


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LangSet.setLocale(this)
ThemeManager(this).applyTheme()
        binding = ActivityExtensionsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> InstalledAnimeExtensionsFragment()
                    1 -> AnimeExtensionsFragment()
                    2 -> InstalledMangaExtensionsFragment()
                    3 -> MangaExtensionsFragment()
                    else -> AnimeExtensionsFragment()
                }
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Installed Anime"
                1 -> "Available Anime"
                2 -> "Installed Manga"
                3 -> "Available Manga"
                else -> null
            }
        }.attach()


        val searchView: AutoCompleteTextView = findViewById(R.id.searchViewText)

        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentFragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
                if (currentFragment is SearchQueryHandler) {
                    currentFragment.updateContentBasedOnQuery(s?.toString()?.trim())
                }
            }
        })


        initActivity(this)

        fun bind(extension: AnimeExtension.Available) {
            binding.languageselect.setOnClickListener {
                val popup = PopupMenu(this, it)
                popup.setOnMenuItemClickListener { item ->
                    val selectedLanguage = when (item.itemId) { //INCOMPLETE REBEL WILL DO IT
                        R.id.all -> "all"
                        R.id.multi -> "multi"
                        R.id.arabic -> "arabic"
                        R.id.german -> "german"
                        R.id.english -> "english"
                        R.id.spanish -> "spanish"
                        R.id.french -> "french"
                        R.id.indonesian -> "indonesian"
                        R.id.italian -> "italian"
                        R.id.japanese -> "japanese"
                        R.id.korean -> "korean"
                        R.id.polish -> "polish"
                        R.id.portuguese_brazil -> "portuguese_brazil"
                        R.id.russian -> "russian"
                        R.id.thai -> "thai"
                        R.id.turkish -> "turkish"
                        R.id.ukrainian -> "ukrainian"
                        R.id.vietnamese -> "vietnamese"
                        R.id.chinese -> "chinese"
                        R.id.chinese_simplified -> "chinese_simplified"
                        else -> null
                    }
                    true
                }
                popup.inflate(R.menu.launguage_selector_menu)
                popup.show()
            }
        }
        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }


    }

}

interface SearchQueryHandler {
    fun updateContentBasedOnQuery(query: String?)
}
