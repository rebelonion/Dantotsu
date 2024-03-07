package ani.dantotsu.media

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.*
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistSearch
import ani.dantotsu.connections.anilist.SearchResults
import ani.dantotsu.databinding.ActivitySearchBinding
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.themes.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private val scope = lifecycleScope
    val model: AnilistSearch by viewModels()

    var style: Int = 0
    private var screenWidth: Float = 0f

    private lateinit var mediaAdaptor: MediaAdaptor
    private lateinit var progressAdapter: ProgressAdapter
    private lateinit var concatAdapter: ConcatAdapter
    private lateinit var headerAdaptor: SearchAdapter

    lateinit var result: SearchResults
    lateinit var updateChips: (() -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)
        screenWidth = resources.displayMetrics.run { widthPixels / density }

        binding.searchRecyclerView.updatePaddingRelative(
            top = statusBarHeight,
            bottom = navBarHeight + 80f.px
        )

        style = PrefManager.getVal(PrefName.SearchStyle)
        var listOnly: Boolean? = intent.getBooleanExtra("listOnly", false)
        if (!listOnly!!) listOnly = null

        val notSet = model.notSet
        if (model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults(
                intent.getStringExtra("type") ?: "ANIME",
                isAdult = if (Anilist.adult) intent.getBooleanExtra("hentai", false) else false,
                onList = listOnly,
                genres = intent.getStringExtra("genre")?.let { mutableListOf(it) },
                tags = intent.getStringExtra("tag")?.let { mutableListOf(it) },
                sort = intent.getStringExtra("sortBy"),
                season = intent.getStringExtra("season"),
                seasonYear = intent.getStringExtra("seasonYear")?.toIntOrNull(),
                results = mutableListOf(),
                hasNextPage = false
            )
        }

        result = model.searchResults

        progressAdapter = ProgressAdapter(searched = model.searched)
        mediaAdaptor = MediaAdaptor(style, model.searchResults.results, this, matchParent = true)
        headerAdaptor = SearchAdapter(this, model.searchResults.type)

        val gridSize = (screenWidth / 120f).toInt()
        val gridLayoutManager = GridLayoutManager(this, gridSize)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (position) {
                    0 -> gridSize
                    concatAdapter.itemCount - 1 -> gridSize
                    else -> when (style) {
                        0 -> 1
                        else -> gridSize
                    }
                }
            }
        }

        concatAdapter = ConcatAdapter(headerAdaptor, mediaAdaptor, progressAdapter)

        binding.searchRecyclerView.layoutManager = gridLayoutManager
        binding.searchRecyclerView.adapter = concatAdapter

        binding.searchRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                if (!v.canScrollVertically(1)) {
                    if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading) {
                        scope.launch(Dispatchers.IO) {
                            model.loadNextPage(model.searchResults)
                        }
                    }
                }
                super.onScrolled(v, dx, dy)
            }
        })

        model.getSearch().observe(this) {
            if (it != null) {
                model.searchResults.apply {
                    onList = it.onList
                    isAdult = it.isAdult
                    perPage = it.perPage
                    search = it.search
                    sort = it.sort
                    genres = it.genres
                    excludedGenres = it.excludedGenres
                    excludedTags = it.excludedTags
                    tags = it.tags
                    season = it.season
                    seasonYear = it.seasonYear
                    format = it.format
                    page = it.page
                    hasNextPage = it.hasNextPage
                }

                val prev = model.searchResults.results.size
                model.searchResults.results.addAll(it.results)
                mediaAdaptor.notifyItemRangeInserted(prev, it.results.size)

                progressAdapter.bar?.visibility = if (it.hasNextPage) View.VISIBLE else View.GONE
            }
        }

        progressAdapter.ready.observe(this) {
            if (it == true) {
                if (!notSet) {
                    if (!model.searched) {
                        model.searched = true
                        headerAdaptor.search?.run()
                    }
                } else
                    headerAdaptor.requestFocus?.run()

                if (intent.getBooleanExtra("search", false)) search()
            }
        }
    }

    fun emptyMediaAdapter() {
        searchTimer.cancel()
        searchTimer.purge()
        mediaAdaptor.notifyItemRangeRemoved(0, model.searchResults.results.size)
        model.searchResults.results.clear()
        progressAdapter.bar?.visibility = View.GONE
    }

    private var searchTimer = Timer()
    private var loading = false
    fun search() {
        headerAdaptor.setHistoryVisibility(false)
        val size = model.searchResults.results.size
        model.searchResults.results.clear()
        binding.searchRecyclerView.post {
            mediaAdaptor.notifyItemRangeRemoved(0, size)
        }

        progressAdapter.bar?.visibility = View.VISIBLE

        searchTimer.cancel()
        searchTimer.purge()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                scope.launch(Dispatchers.IO) {
                    loading = true
                    model.loadSearch(result)
                    loading = false
                }
            }
        }
        searchTimer = Timer()
        searchTimer.schedule(timerTask, 500)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun recycler() {
        mediaAdaptor.type = style
        mediaAdaptor.notifyDataSetChanged()
    }

    var state: Parcelable? = null
    override fun onPause() {
        if (this::headerAdaptor.isInitialized) {
            headerAdaptor.addHistory()
        }
        super.onPause()
        state = binding.searchRecyclerView.layoutManager?.onSaveInstanceState()
    }

    override fun onResume() {
        super.onResume()
        binding.searchRecyclerView.layoutManager?.onRestoreInstanceState(state)
    }
}