package ani.dantotsu.home

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.marginBottom
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.bottomBar
import ani.dantotsu.connections.anilist.AniMangaSearchResults
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistMangaViewModel
import ani.dantotsu.connections.anilist.getUserId
import ani.dantotsu.databinding.FragmentMangaBinding
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.ProgressAdapter
import ani.dantotsu.navBarHeight
import ani.dantotsu.px
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class MangaFragment : Fragment() {
    private var _binding: FragmentMangaBinding? = null
    private val binding get() = _binding!!
    private lateinit var mangaPageAdapter: MangaPageAdapter

    val model: AnilistMangaViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMangaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val scope = viewLifecycleOwner.lifecycleScope

        var height = statusBarHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    height = max(
                        statusBarHeight,
                        min(
                            displayCutout.boundingRects[0].width(),
                            displayCutout.boundingRects[0].height()
                        )
                    )
                }
            }
        }
        binding.mangaRefresh.setSlingshotDistance(height + 128)
        binding.mangaRefresh.setProgressViewEndTarget(false, height + 128)
        binding.mangaRefresh.setOnRefreshListener {
            Refresh.activity[this.hashCode()]!!.postValue(true)
        }

        binding.mangaPageRecyclerView.updatePaddingRelative(bottom = navBarHeight + 160f.px)

        mangaPageAdapter = MangaPageAdapter()
        var loading = true
        if (model.notSet) {
            model.notSet = false
            model.aniMangaSearchResults = AniMangaSearchResults(
                "MANGA",
                isAdult = false,
                onList = false,
                results = arrayListOf(),
                hasNextPage = true,
                sort = Anilist.sortBy[1]
            )
        }
        val popularAdaptor = MediaAdaptor(1, model.aniMangaSearchResults.results, requireActivity())
        val progressAdaptor = ProgressAdapter(searched = model.searched)
        binding.mangaPageRecyclerView.adapter =
            ConcatAdapter(mangaPageAdapter, popularAdaptor, progressAdaptor)
        val layout = LinearLayoutManager(requireContext())
        binding.mangaPageRecyclerView.layoutManager = layout

        var visible = false
        fun animate() {
            val start = if (visible) 0f else 1f
            val end = if (!visible) 0f else 1f
            ObjectAnimator.ofFloat(binding.mangaPageScrollTop, "scaleX", start, end).apply {
                duration = 300
                interpolator = OvershootInterpolator(2f)
                start()
            }
            ObjectAnimator.ofFloat(binding.mangaPageScrollTop, "scaleY", start, end).apply {
                duration = 300
                interpolator = OvershootInterpolator(2f)
                start()
            }
        }

        binding.mangaPageScrollTop.setOnClickListener {
            binding.mangaPageRecyclerView.scrollToPosition(4)
            binding.mangaPageRecyclerView.smoothScrollToPosition(0)
        }

        binding.mangaPageRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                if (!v.canScrollVertically(1)) {
                    if (model.aniMangaSearchResults.hasNextPage && model.aniMangaSearchResults.results.isNotEmpty() && !loading) {
                        scope.launch(Dispatchers.IO) {
                            loading = true
                            model.loadNextPage(model.aniMangaSearchResults)
                        }
                    }
                }
                if (layout.findFirstVisibleItemPosition() > 1 && !visible) {
                    binding.mangaPageScrollTop.visibility = View.VISIBLE
                    visible = true
                    animate()
                }

                if (!v.canScrollVertically(-1)) {
                    visible = false
                    animate()
                    scope.launch {
                        delay(300)
                        binding.mangaPageScrollTop.visibility = View.GONE
                    }
                }

                super.onScrolled(v, dx, dy)
            }
        })
        mangaPageAdapter.ready.observe(viewLifecycleOwner) { i ->
            if (i == true) {
                model.getPopularNovel().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateNovel(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                model.getPopularManga().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateTrendingManga(
                            MediaAdaptor(0, it, requireActivity()),
                            it
                        )
                    }
                }
                model.getPopularManhwa().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateTrendingManhwa(
                            MediaAdaptor(
                                0,
                                it,
                                requireActivity()
                            ), it
                        )
                    }
                }
                model.getTopRated().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateTopRated(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                model.getMostFav().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateMostFav(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                if (mangaPageAdapter.trendingViewPager != null) {
                    mangaPageAdapter.updateHeight()
                    model.getTrending().observe(viewLifecycleOwner) {
                        if (it != null) {
                            mangaPageAdapter.updateTrending(
                                MediaAdaptor(
                                    if (PrefManager.getVal(PrefName.SmallView)) 3 else 2,
                                    it,
                                    requireActivity(),
                                    viewPager = mangaPageAdapter.trendingViewPager
                                )
                            )
                            mangaPageAdapter.updateAvatar()
                        }
                    }
                }
                binding.mangaPageScrollTop.translationY =
                    -(navBarHeight + bottomBar.height + bottomBar.marginBottom).toFloat()

            }
        }

        var oldIncludeList = true

        mangaPageAdapter.onIncludeListClick = { checked ->
            oldIncludeList = !checked
            loading = true
            model.aniMangaSearchResults.results.clear()
            popularAdaptor.notifyDataSetChanged()
            scope.launch(Dispatchers.IO) {
                model.loadPopular("MANGA", sort = Anilist.sortBy[1], onList = checked)
            }
        }

        model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {
                if (oldIncludeList == (it.onList != false)) {
                    val prev = model.aniMangaSearchResults.results.size
                    model.aniMangaSearchResults.results.addAll(it.results)
                    popularAdaptor.notifyItemRangeInserted(prev, it.results.size)
                } else {
                    model.aniMangaSearchResults.results.addAll(it.results)
                    popularAdaptor.notifyDataSetChanged()
                    oldIncludeList = it.onList ?: true
                }
                model.aniMangaSearchResults.onList = it.onList
                model.aniMangaSearchResults.hasNextPage = it.hasNextPage
                model.aniMangaSearchResults.page = it.page
                if (it.hasNextPage)
                    progressAdaptor.bar?.visibility = View.VISIBLE
                else {
                    snackString(getString(R.string.jobless_message))
                    progressAdaptor.bar?.visibility = View.GONE
                }
                loading = false
            }
        }

        fun load() = scope.launch(Dispatchers.Main) {
            mangaPageAdapter.updateAvatar()
        }

        var running = false
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(viewLifecycleOwner) {
            if (!running && it) {
                running = true
                scope.launch {
                    withContext(Dispatchers.IO) {
                        Anilist.userid =
                            PrefManager.getNullableVal<String>(PrefName.AnilistUserId, null)
                                ?.toIntOrNull()
                        if (Anilist.userid == null) {
                            getUserId(requireContext()) {
                                load()
                            }
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                getUserId(requireContext()) {
                                    load()
                                }
                            }
                        }
                    }
                    model.loaded = true
                    val loadTrending = async(Dispatchers.IO) { model.loadTrending() }
                    val loadAll = async(Dispatchers.IO) { model.loadAll() }
                    val loadPopular = async(Dispatchers.IO) {
                        model.loadPopular(
                            "MANGA",
                            sort = Anilist.sortBy[1],
                            onList = PrefManager.getVal(PrefName.PopularAnimeList)
                        )
                    }

                    loadTrending.await()
                    loadAll.await()
                    loadPopular.await()

                    live.postValue(false)
                    _binding?.mangaRefresh?.isRefreshing = false
                    running = false
                }
            }
        }
    }

    override fun onResume() {
        if (!model.loaded) Refresh.activity[this.hashCode()]!!.postValue(true)
        //make sure mangaPageAdapter is initialized
        if (mangaPageAdapter.trendingViewPager != null) {
            binding.root.requestApplyInsets()
            binding.root.requestLayout()
        }
        if (this::mangaPageAdapter.isInitialized && _binding != null) {
            mangaPageAdapter.updateNotificationCount()
        }
        super.onResume()
    }

}