package ani.dantotsu.media

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.EmptyAdapter
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.ActivityAuthorBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.getSerialized
import ani.dantotsu.px
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthorBinding
    private val scope = lifecycleScope
    private val model: OtherDetailsViewModel by viewModels()
    private var author: Author? = null
    private var loaded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityAuthorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        this.window.statusBarColor = ContextCompat.getColor(this, R.color.nav_bg)

        val screenWidth = resources.displayMetrics.run { widthPixels / density }

        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.studioRecycler.updatePadding(bottom = 64f.px + navBarHeight)
        binding.studioTitle.isSelected = true

        author = intent.getSerialized("author")
        binding.studioTitle.text = author?.name

        binding.studioClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        model.getAuthor().observe(this) {
            if (it != null) {
                author = it
                loaded = true
                binding.studioProgressBar.visibility = View.GONE
                binding.studioRecycler.visibility = View.VISIBLE
                if (author!!.yearMedia.isNullOrEmpty()) {
                    binding.studioRecycler.visibility = View.GONE
                }
                val titlePosition = arrayListOf<Int>()
                val concatAdapter = ConcatAdapter()
                val map = author!!.yearMedia ?: return@observe
                val keys = map.keys.toTypedArray()
                var pos = 0

                val gridSize = (screenWidth / 124f).toInt()
                val gridLayoutManager = GridLayoutManager(this, gridSize)
                gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (position in titlePosition) {
                            true -> gridSize
                            else -> 1
                        }
                    }
                }
                for (i in keys.indices) {
                    val medias = map[keys[i]]!!
                    val empty = if (medias.size >= 4) medias.size % 4 else 4 - medias.size
                    titlePosition.add(pos)
                    pos += (empty + medias.size + 1)

                    concatAdapter.addAdapter(TitleAdapter("${keys[i]} (${medias.size})"))
                    concatAdapter.addAdapter(MediaAdaptor(0, medias, this, true))
                    concatAdapter.addAdapter(EmptyAdapter(empty))
                }
                binding.studioRecycler.adapter = concatAdapter
                binding.studioRecycler.layoutManager = gridLayoutManager

                binding.charactersRecycler.visibility = View.VISIBLE
                binding.charactersText.visibility = View.VISIBLE
                binding.charactersRecycler.adapter =
                    CharacterAdapter(author!!.character ?: arrayListOf())
                binding.charactersRecycler.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                if (author!!.character.isNullOrEmpty()) {
                    binding.charactersRecycler.visibility = View.GONE
                    binding.charactersText.visibility = View.GONE
                }
            }
        }
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            if (it) {
                scope.launch {
                    if (author != null)
                        withContext(Dispatchers.IO) { model.loadAuthor(author!!) }
                    live.postValue(false)
                }
            }
        }
    }

    override fun onDestroy() {
        if (Refresh.activity.containsKey(this.hashCode())) {
            Refresh.activity.remove(this.hashCode())
        }
        super.onDestroy()
    }

    override fun onResume() {
        binding.studioProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }
}