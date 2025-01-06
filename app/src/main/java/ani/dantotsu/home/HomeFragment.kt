package ani.dantotsu.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import ani.dantotsu.blurImage
import ani.dantotsu.bottomBar
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistHomeViewModel
import ani.dantotsu.connections.anilist.getUserId
import ani.dantotsu.currContext
import ani.dantotsu.databinding.FragmentHomeBinding
import ani.dantotsu.home.status.UserStatusAdapter
import ani.dantotsu.loadImage
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.MediaListViewActivity
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.setSlideIn
import ani.dantotsu.setSlideUp
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefManager.asLiveBool
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
        Logger.log("HomeFragment")
        fun load() {
            Logger.log("Loading HomeFragment")
            if (activity != null && _binding != null) lifecycleScope.launch(Dispatchers.Main) {
                binding.homeUserName.text = Anilist.username
                binding.homeUserEpisodesWatched.text = Anilist.episodesWatched.toString()
                binding.homeUserChaptersRead.text = Anilist.chapterRead.toString()
                binding.homeUserAvatar.loadImage(Anilist.avatar)
                val bannerAnimations: Boolean = PrefManager.getVal(PrefName.BannerAnimations)
                blurImage(
                    if (bannerAnimations) binding.homeUserBg else binding.homeUserBgNoKen,
                    Anilist.bg
                )
                binding.homeUserDataProgressBar.visibility = View.GONE
                binding.homeNotificationCount.isVisible = Anilist.unreadNotificationCount > 0
                        && PrefManager.getVal<Boolean>(PrefName.ShowNotificationRedDot) == true
                binding.homeNotificationCount.text = Anilist.unreadNotificationCount.toString()

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
        binding.searchImageContainer.setSafeOnClickListener {
            SearchBottomSheet.newInstance().show(
                (it.context as androidx.appcompat.app.AppCompatActivity).supportFragmentManager,
                "search"
            )
        }
        binding.homeUserAvatarContainer.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            ContextCompat.startActivity(
                requireContext(), Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra("userId", Anilist.userid), null
            )
            false
        }

        binding.homeContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        binding.homeUserBg.updateLayoutParams { height += statusBarHeight }
        binding.homeUserBgNoKen.updateLayoutParams { height += statusBarHeight }
        binding.homeTopContainer.updatePadding(top = statusBarHeight)

        var reached = false
        val duration = ((PrefManager.getVal(PrefName.AnimationSpeed) as Float) * 200).toLong()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            title: View,
            more: View,
            string: String
        ) {
            container.visibility = View.VISIBLE
            progress.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            empty.visibility = View.GONE
            title.visibility = View.INVISIBLE
            more.visibility = View.INVISIBLE

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
                        more.setOnClickListener { i ->
                            MediaListViewActivity.passedMedia = it
                            ContextCompat.startActivity(
                                i.context, Intent(i.context, MediaListViewActivity::class.java)
                                    .putExtra("title", string),
                                null
                            )
                        }
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.layoutAnimation =
                            LayoutAnimationController(setSlideIn(), 0.25f)

                    } else {
                        empty.visibility = View.VISIBLE
                    }
                    more.visibility = View.VISIBLE
                    title.visibility = View.VISIBLE
                    more.startAnimation(setSlideUp())
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
            binding.homeContinueWatch,
            binding.homeContinueWatchMore,
            getString(R.string.continue_watching)
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
            binding.homeFavAnime,
            binding.homeFavAnimeMore,
            getString(R.string.fav_anime)
        )

        initRecyclerView(
            model.getAnimePlanned(),
            binding.homePlannedAnimeContainer,
            binding.homePlannedAnimeRecyclerView,
            binding.homePlannedAnimeProgressBar,
            binding.homePlannedAnimeEmpty,
            binding.homePlannedAnime,
            binding.homePlannedAnimeMore,
            getString(R.string.planned_anime)
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
            binding.homeContinueRead,
            binding.homeContinueReadMore,
            getString(R.string.continue_reading)
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
            binding.homeFavManga,
            binding.homeFavMangaMore,
            getString(R.string.fav_manga)
        )

        initRecyclerView(
            model.getMangaPlanned(),
            binding.homePlannedMangaContainer,
            binding.homePlannedMangaRecyclerView,
            binding.homePlannedMangaProgressBar,
            binding.homePlannedMangaEmpty,
            binding.homePlannedManga,
            binding.homePlannedMangaMore,
            getString(R.string.planned_manga)
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
            binding.homeRecommended,
            binding.homeRecommendedMore,
            getString(R.string.recommended)
        )
        binding.homeUserStatusContainer.visibility = View.VISIBLE
        binding.homeUserStatusProgressBar.visibility = View.VISIBLE
        binding.homeUserStatusRecyclerView.visibility = View.GONE
        model.getUserStatus().observe(viewLifecycleOwner) {
            binding.homeUserStatusRecyclerView.visibility = View.GONE
            if (it != null) {
                if (it.isNotEmpty()) {
                    PrefManager.getLiveVal(PrefName.RefreshStatus, false).apply {
                        asLiveBool()
                        observe(viewLifecycleOwner) { _ ->
                            binding.homeUserStatusRecyclerView.adapter = UserStatusAdapter(it)
                        }
                    }
                    binding.homeUserStatusRecyclerView.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    binding.homeUserStatusRecyclerView.visibility = View.VISIBLE
                    binding.homeUserStatusRecyclerView.layoutAnimation =
                        LayoutAnimationController(setSlideIn(), 0.25f)

                } else {
                    binding.homeUserStatusContainer.visibility = View.GONE
                }
                binding.homeUserStatusProgressBar.visibility = View.GONE
            }

        }
        binding.homeHiddenItemsContainer.visibility = View.GONE
        model.getHidden().observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    binding.homeHiddenItemsRecyclerView.adapter =
                        MediaAdaptor(0, it, requireActivity())
                    binding.homeHiddenItemsRecyclerView.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    binding.homeContinueWatch.setOnLongClickListener {
                        binding.homeHiddenItemsContainer.visibility = View.VISIBLE
                        binding.homeHiddenItemsRecyclerView.layoutAnimation =
                            LayoutAnimationController(setSlideIn(), 0.25f)
                        true
                    }
                    binding.homeHiddenItemsMore.setSafeOnClickListener { _ ->
                        MediaListViewActivity.passedMedia = it
                        ContextCompat.startActivity(
                            requireActivity(),
                            Intent(requireActivity(), MediaListViewActivity::class.java)
                                .putExtra("title", getString(R.string.hidden)),
                            null
                        )
                    }
                    binding.homeHiddenItemsTitle.setOnLongClickListener {
                        binding.homeHiddenItemsContainer.visibility = View.GONE
                        true
                    }
                } else {
                    binding.homeContinueWatch.setOnLongClickListener {
                        snackString(getString(R.string.no_hidden_items))
                        true
                    }
                }
            } else {
                binding.homeContinueWatch.setOnLongClickListener {
                    snackString(getString(R.string.no_hidden_items))
                    true
                }
            }
        }

        binding.homeUserAvatarContainer.startAnimation(setSlideUp())

        model.empty.observe(viewLifecycleOwner)
        {
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
            "Recommendation",
            "UserStatus",
        )

        val containers = arrayOf(
            binding.homeContinueWatchingContainer,
            binding.homeFavAnimeContainer,
            binding.homePlannedAnimeContainer,
            binding.homeContinueReadingContainer,
            binding.homeFavMangaContainer,
            binding.homePlannedMangaContainer,
            binding.homeRecommendedContainer,
            binding.homeUserStatusContainer,
        )

        var running = false
        val live = Refresh.activity.getOrPut(1) { MutableLiveData(true) }
        live.observe(viewLifecycleOwner) { shouldRefresh ->
            if (!running && shouldRefresh) {
                running = true
                scope.launch {
                    withContext(Dispatchers.IO) {
                        // Get user data first
                        Anilist.userid =
                            PrefManager.getNullableVal<String>(PrefName.AnilistUserId, null)
                                ?.toIntOrNull()
                        if (Anilist.userid == null) {
                            withContext(Dispatchers.Main) {
                                getUserId(requireContext()) {
                                    load()
                                }
                            }
                        } else {
                            getUserId(requireContext()) {
                                load()
                            }
                        }
                        model.loaded = true
                        model.setListImages()
                    }

                    var empty = true
                    val homeLayoutShow: List<Boolean> = PrefManager.getVal(PrefName.HomeLayout)

                    withContext(Dispatchers.Main) {
                        homeLayoutShow.indices.forEach { i ->
                            if (homeLayoutShow.elementAt(i)) {
                                empty = false
                            } else {
                                containers[i].visibility = View.GONE
                            }
                        }
                    }

                    val initHomePage = async(Dispatchers.IO) { model.initHomePage() }
                    val initUserStatus = async(Dispatchers.IO) { model.initUserStatus() }
                    initHomePage.await()
                    initUserStatus.await()

                    withContext(Dispatchers.Main) {
                        model.empty.postValue(empty)
                        binding.homeHiddenItemsContainer.visibility = View.GONE
                    }

                    live.postValue(false)
                    _binding?.homeRefresh?.isRefreshing = false
                    running = false
                }
            }
        }
    }

    override fun onResume() {
        if (!model.loaded) Refresh.activity[1]!!.postValue(true)
        if (_binding != null) {
            binding.homeNotificationCount.isVisible = Anilist.unreadNotificationCount > 0
                    && PrefManager.getVal<Boolean>(PrefName.ShowNotificationRedDot) == true
            binding.homeNotificationCount.text = Anilist.unreadNotificationCount.toString()
        }
        super.onResume()
    }
}