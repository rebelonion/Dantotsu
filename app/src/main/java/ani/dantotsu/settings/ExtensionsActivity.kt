package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build.*
import android.os.Build.VERSION.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.*
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import ani.dantotsu.databinding.ActivityExtensionsBinding
import ani.dantotsu.home.AnimeFragment
import ani.dantotsu.home.MangaFragment
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import eu.kanade.tachiyomi.data.notification.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import javax.inject.Inject


class ExtensionsActivity : AppCompatActivity()  {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@ExtensionsActivity)
    }
    lateinit var binding: ActivityExtensionsBinding


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtensionsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> AnimeExtensionsFragment()
                    1 -> MangaExtensionsFragment()
                    else -> AnimeExtensionsFragment()
                }
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Anime" // Your tab title
                1 -> "Manga" // Your tab title
                else -> null
            }
        }.attach()


        val searchView: SearchView = findViewById(R.id.searchView)

        val extensionsHeader: LinearLayout = findViewById(R.id.extensionsHeader)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val currentFragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
                if (currentFragment is SearchQueryHandler) {
                    currentFragment.updateContentBasedOnQuery(newText)
                }

                return true
            }
        })


        initActivity(this)



        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        onBackPressedDispatcher.addCallback(this, restartMainActivity)

        binding.settingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


    }

}

interface SearchQueryHandler {
    fun updateContentBasedOnQuery(query: String?)
}
