package ani.dantotsu.media

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.GenresViewModel
import ani.dantotsu.databinding.ActivityGenreBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class GenreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGenreBinding
    val model: GenresViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityGenreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)

        binding.genreContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight;bottomMargin += navBarHeight }
        val screenWidth = resources.displayMetrics.run { widthPixels / density }
        val type = intent.getStringExtra("type")
        if (type != null) {
            val adapter = GenreAdapter(type, true)
            model.doneListener = {
                MainScope().launch {
                    binding.mediaInfoGenresProgressBar.visibility = View.GONE
                }
            }
            if (model.genres != null) {
                adapter.genres = model.genres!!
                adapter.pos = ArrayList(model.genres!!.keys)
                if (model.done)
                    model.doneListener?.invoke()
            }
            binding.mediaInfoGenresRecyclerView.adapter = adapter
            binding.mediaInfoGenresRecyclerView.layoutManager =
                GridLayoutManager(this, (screenWidth / 156f).toInt())

            lifecycleScope.launch(Dispatchers.IO) {
                model.loadGenres(
                    Anilist.genres ?: loadLocalGenres() ?: arrayListOf()
                ) {
                    MainScope().launch {
                        adapter.addGenre(it)
                    }
                }
            }
        }
    }

    private fun loadLocalGenres(): ArrayList<String>? {
        val genres = PrefManager.getVal<Set<String>>(PrefName.GenresList)
            .toMutableList()
        return if (genres.isEmpty()) {
            null
        } else {
            //sort alphabetically
            genres.sort().let { genres as ArrayList<String> }
        }
    }
}