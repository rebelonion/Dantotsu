package ani.dantotsu.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.bottomBar
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistHomeViewModel
import ani.dantotsu.connections.anilist.getUserId
import ani.dantotsu.currContext
import ani.dantotsu.databinding.FragmentHomeBinding
import ani.dantotsu.loadImage
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.setSlideIn
import ani.dantotsu.setSlideUp
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    val model: AnilistHomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val scope = lifecycleScope
        fun load() {
            if (activity != null && _binding != null) lifecycleScope.launch(Dispatchers.Main) {
                binding.homeUserName.text = Anilist.username
                binding.homeUserEpisodesWatched.text = Anilist.episodesWatched.toString()
                binding.homeUserChaptersRead.text = Anilist.chapterRead.toString()
                binding.homeUserAvatar.loadImage(Anilist.avatar)
                if (!(PrefManager.getVal(PrefName.BannerAnimations) as Boolean)) binding.homeUserBg.pause()
                binding.homeUserBg.loadImage(Anilist.bg)
                binding.homeUserDataProgressBar.visibility = View.GONE

                binding.homeAnimeList.setOnClickListener {
                    ContextCompat.startActivity(
                        requireActivity(), Intent(requireActivity(), ListActivity::class.java)
                            .putExtra("anime", true)
                            .putExtra("userId", Anilist.userid)
                            .putExtra("username", Anilist.username), null
                    )
                }
                binding.homeMangaList.setOnClickListener {
                    ContextCompat.startActivity(
                        requireActivity(), Intent(requireActivity(), ListActivity::class.java)
                            .putExtra("anime", false)
                            .putExtra("userId", Anilist.userid)
                            .putExtra("username", Anilist.username), null
                    )
                }

                binding.homeUserAvatarContainer.startAnimation(setSlideUp())
                binding.homeUserDataContainer.visibility = View.VISIBLE
                binding.homeUserDataContainer.layoutAnimation =
                    LayoutAnimationController(setSlideUp(), 0.25f)
                binding.homeAnimeList.visibility = View.VISIBLE
                binding.homeMangaList.visibility = View.VISIBLE
                binding.homeListContainer.layoutAnimation =
                    LayoutAnimationController(setSlideIn(), 0.25f)
            }
            else {
                snackString(currContext()?.getString(R.string.please_reload))
            }
        }
        binding.homeUserAvatarContainer.setSafeOnClickListener {
            val dialogFragment =
                SettingsDialogFragment.newInstance(SettingsDialogFragment.Companion.PageType.HOME)
            dialogFragment.show(
                (it.context as androidx.appcompat.app.AppCompatActivity).supportFragmentManager,
                "dialog"
            )
        }

        binding.homeContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        binding.homeUserBg.updateLayoutParams { height += statusBarHeight }
        binding.homeTopContainer.updatePadding(top = statusBarHeight)

        var reached = false
        val duration = ((PrefManager.getVal(PrefName.AnimationSpeed) as Float) * 200).toLong()
        binding.homeScroll.setOnScrollChangeListener { _, _, _, _, _ ->
            if (!binding.homeScroll.canScrollVertically(1)) {
                reached = true
                bottomBar.animate().translationZ(0f).setDuration(duration).start()
                ObjectAnimator.ofFloat(bottomBar, "elevation", 4f, 0f).setDuration(duration)
                    .start()
            } else {
                if (reached) {
                    bottomBar.animate().translationZ(12f).setDuration(duration).start()
                    ObjectAnimator.ofFloat(bottomBar, "elevation", 0f, 4f).setDuration(duration)
                        .start()
                }
            }
        }
        var height = statusBarHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    height =
                        max(
                            statusBarHeight,
                            min(
                                displayCutout.boundingRects[0].width(),
                                displayCutout.boundingRects[0].height()
                            )
                        )
                }
            }
        }
        binding.homeRefresh.setSlingshotDistance(height + 128)
        binding.homeRefresh.setProgressViewEndTarget(false, height + 128)
        binding.homeRefresh.setOnRefreshListener {
            Refresh.activity[1]!!.postValue(true)
        }

        //UserData
        binding.homeUserDataProgressBar.visibility = View.VISIBLE
        binding.homeUserDataContainer.visibility = View.GONE
        if (model.loaded) {
            load()
        }
        //List Images
        model.getListImages().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.homeAnimeListImage.loadImage(it[0] ?: "https://bit.ly/31bsIHq")
                binding.homeMangaListImage.loadImage(it[1] ?: "https://bit.ly/2ZGfcuG")
            }
        }

        //Function For Recycler Views
        fun initRecyclerView(
            mode: LiveData<ArrayList<Media>>,
            container: View,
            recyclerView: RecyclerView,
            progress: View,
            empty: View,
            title: View
        ) {
            container.visibility = View.VISIBLE
            progress.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            empty.visibility = View.GONE
            title.visibility = View.INVISIBLE

            mode.observe(viewLifecycleOwner) {
                recyclerView.visibility = View.GONE
                empty.visibility = View.GONE
                if (it != null) {
                    if (it.isNotEmpty()) {
                        recyclerView.adapter = MediaAdaptor(0, it, requireActivity())
                        recyclerView.layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.layoutAnimation =
                            LayoutAnimationController(setSlideIn(), 0.25f)

                    } else {
                        empty.visibility = View.VISIBLE
                    }
                    title.visibility = View.VISIBLE
                    title.startAnimation(setSlideUp())
                    progress.visibility = View.GONE
                }
            }

        }

        // Recycler Views
        initRecyclerView(
            model.getAnimeContinue(),
            binding.homeContinueWatchingContainer,
            binding.homeWatchingRecyclerView,
            binding.homeWatchingProgressBar,
            binding.homeWatchingEmpty,
            binding.homeContinueWatch
        )
        binding.homeWatchingBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(0)
        }

        initRecyclerView(
            model.getAnimeFav(),
            binding.homeFavAnimeContainer,
            binding.homeFavAnimeRecyclerView,
            binding.homeFavAnimeProgressBar,
            binding.homeFavAnimeEmpty,
            binding.homeFavAnime
        )

        initRecyclerView(
            model.getAnimePlanned(),
            binding.homePlannedAnimeContainer,
            binding.homePlannedAnimeRecyclerView,
            binding.homePlannedAnimeProgressBar,
            binding.homePlannedAnimeEmpty,
            binding.homePlannedAnime
        )
        binding.homePlannedAnimeBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(0)
        }

        initRecyclerView(
            model.getMangaContinue(),
            binding.homeContinueReadingContainer,
            binding.homeReadingRecyclerView,
            binding.homeReadingProgressBar,
            binding.homeReadingEmpty,
            binding.homeContinueRead
        )
        binding.homeReadingBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(2)
        }

        initRecyclerView(
            model.getMangaFav(),
            binding.homeFavMangaContainer,
            binding.homeFavMangaRecyclerView,
            binding.homeFavMangaProgressBar,
            binding.homeFavMangaEmpty,
            binding.homeFavManga
        )

        initRecyclerView(
            model.getMangaPlanned(),
            binding.homePlannedMangaContainer,
            binding.homePlannedMangaRecyclerView,
            binding.homePlannedMangaProgressBar,
            binding.homePlannedMangaEmpty,
            binding.homePlannedManga
        )
        binding.homePlannedMangaBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(2)
        }

        initRecyclerView(
            model.getRecommendation(),
            binding.homeRecommendedContainer,
            binding.homeRecommendedRecyclerView,
            binding.homeRecommendedProgressBar,
            binding.homeRecommendedEmpty,
            binding.homeRecommended
        )

        binding.homeUserAvatarContainer.startAnimation(setSlideUp())

        model.empty.observe(viewLifecycleOwner) {
            binding.homeDantotsuContainer.visibility = if (it == true) View.VISIBLE else View.GONE
            (binding.homeDantotsuIcon.drawable as Animatable).start()
            binding.homeDantotsuContainer.startAnimation(setSlideUp())
            binding.homeDantotsuIcon.setSafeOnClickListener {
                (binding.homeDantotsuIcon.drawable as Animatable).start()
            }
        }

        val array = arrayOf(
            "AnimeContinue",
            "AnimeFav",
            "AnimePlanned",
            "MangaContinue",
            "MangaFav",
            "MangaPlanned",
            "Recommendation"
        )

        val containers = arrayOf(
            binding.homeContinueWatchingContainer,
            binding.homeFavAnimeContainer,
            binding.homePlannedAnimeContainer,
            binding.homeContinueReadingContainer,
            binding.homeFavMangaContainer,
            binding.homePlannedMangaContainer,
            binding.homeRecommendedContainer
        )

        val live = Refresh.activity.getOrPut(1) { MutableLiveData(false) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        //Get userData First
                        getUserId(requireContext()) {
                            load()
                        }
                        model.loaded = true
                        model.setListImages()
                        var empty = true
                        val homeLayoutShow: List<Boolean> =
                            PrefManager.getVal(PrefName.HomeLayoutShow)
                        runBlocking {
                            model.initHomePage()
                        }
                        (array.indices).forEach { i ->
                            if (homeLayoutShow.elementAt(i)) {
                                empty = false
                            } else withContext(Dispatchers.Main) {
                                containers[i].visibility = View.GONE
                            }
                        }
                        model.empty.postValue(empty)
                    }
                    live.postValue(false)
                    _binding?.homeRefresh?.isRefreshing = false
                }
            }
        }
    }

    override fun onResume() {
        if (!model.loaded) Refresh.activity[1]!!.postValue(true)
        super.onResume()
    }
}