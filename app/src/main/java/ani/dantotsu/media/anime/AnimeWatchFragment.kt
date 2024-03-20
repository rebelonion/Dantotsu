package ani.dantotsu.media.anime

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.*
import ani.dantotsu.databinding.FragmentAnimeWatchBinding
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.anime.AnimeDownloaderService
import ani.dantotsu.download.video.ExoplayerDownloadService
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.AnimeParser
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.HAnimeSources
import ani.dantotsu.settings.extensionprefs.AnimeSourcePreferencesFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.notifications.subscription.SubscriptionHelper
import ani.dantotsu.notifications.subscription.SubscriptionHelper.Companion.saveSubscription
import com.google.android.material.appbar.AppBarLayout
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class AnimeWatchFragment : Fragment() {
    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!
    private val model: MediaDetailsViewModel by activityViewModels()

    private lateinit var media: Media

    private var start = 0
    private var end: Int? = null
    private var style: Int? = null
    private var reverse = false

    private lateinit var headerAdapter: AnimeWatchAdapter
    private lateinit var episodeAdapter: EpisodeAdapter

    val downloadManager = Injekt.get<DownloadsManager>()

    var screenWidth = 0f
    private var progress = View.VISIBLE

    var continueEp: Boolean = false
    var loaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAnimeWatchBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_DOWNLOAD_STARTED)
            addAction(ACTION_DOWNLOAD_FINISHED)
            addAction(ACTION_DOWNLOAD_FAILED)
            addAction(ACTION_DOWNLOAD_PROGRESS)
        }

        ContextCompat.registerReceiver(
            requireContext(),
            downloadStatusReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )


        binding.animeSourceRecycler.updatePadding(bottom = binding.animeSourceRecycler.paddingBottom + navBarHeight)
        screenWidth = resources.displayMetrics.widthPixels.dp

        var maxGridSize = (screenWidth / 100f).roundToInt()
        maxGridSize = max(4, maxGridSize - (maxGridSize % 2))

        val gridLayoutManager = GridLayoutManager(requireContext(), maxGridSize)

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val style = episodeAdapter.getItemViewType(position)

                return when (position) {
                    0 -> maxGridSize
                    else -> when (style) {
                        0 -> maxGridSize
                        1 -> 2
                        2 -> 1
                        else -> maxGridSize
                    }
                }
            }
        }

        binding.animeSourceRecycler.layoutManager = gridLayoutManager

        binding.ScrollTop.setOnClickListener {
            binding.animeSourceRecycler.scrollToPosition(10)
            binding.animeSourceRecycler.smoothScrollToPosition(0)
        }
        binding.animeSourceRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val position = gridLayoutManager.findFirstVisibleItemPosition()
                if (position > 2) {
                    binding.ScrollTop.translationY = -navBarHeight.toFloat()
                    binding.ScrollTop.visibility = View.VISIBLE
                } else {
                    binding.ScrollTop.visibility = View.GONE
                }
            }
        })
        model.scrolledToTop.observe(viewLifecycleOwner) {
            if (it) binding.animeSourceRecycler.scrollToPosition(0)
        }

        continueEp = model.continueMedia ?: false
        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it
                media.selected = model.loadSelected(media)

                subscribed =
                    SubscriptionHelper.getSubscriptions().containsKey(media.id)

                style = media.selected!!.recyclerStyle
                reverse = media.selected!!.recyclerReversed

                progress = View.GONE
                binding.mediaInfoProgressBar.visibility = progress

                if (!loaded) {
                    model.watchSources = if (media.isAdult) HAnimeSources else AnimeSources

                    val offlineMode =
                        model.watchSources!!.isDownloadedSource(media.selected!!.sourceIndex)

                    headerAdapter = AnimeWatchAdapter(it, this, model.watchSources!!)
                    episodeAdapter =
                        EpisodeAdapter(
                            style ?: PrefManager.getVal(PrefName.AnimeDefaultView),
                            media,
                            this,
                            offlineMode = offlineMode
                        )

                    binding.animeSourceRecycler.adapter =
                        ConcatAdapter(headerAdapter, episodeAdapter)

                    lifecycleScope.launch(Dispatchers.IO) {
                        awaitAll(
                            async { model.loadKitsuEpisodes(media) },
                            async { model.loadFillerEpisodes(media) }
                        )
                        model.loadEpisodes(media, media.selected!!.sourceIndex)
                    }
                    loaded = true
                } else {
                    reload()
                }
            }
        }
        model.getEpisodes().observe(viewLifecycleOwner) { loadedEpisodes ->
            if (loadedEpisodes != null) {
                val episodes = loadedEpisodes[media.selected!!.sourceIndex]
                if (episodes != null) {
                    episodes.forEach { (i, episode) ->
                        if (media.anime?.fillerEpisodes != null) {
                            if (media.anime!!.fillerEpisodes!!.containsKey(i)) {
                                episode.title =
                                    episode.title ?: media.anime!!.fillerEpisodes!![i]?.title
                                episode.filler = media.anime!!.fillerEpisodes!![i]?.filler ?: false
                            }
                        }
                        if (media.anime?.kitsuEpisodes != null) {
                            if (media.anime!!.kitsuEpisodes!!.containsKey(i)) {
                                episode.desc =
                                    media.anime!!.kitsuEpisodes!![i]?.desc ?: episode.desc
                                episode.title = if (AnimeNameAdapter.removeEpisodeNumberCompletely(
                                        episode.title ?: ""
                                    ).isBlank()
                                ) media.anime!!.kitsuEpisodes!![i]?.title
                                    ?: episode.title else episode.title
                                    ?: media.anime!!.kitsuEpisodes!![i]?.title ?: episode.title
                                episode.thumb = media.anime!!.kitsuEpisodes!![i]?.thumb
                                    ?: FileUrl[media.cover]
                            }
                        }
                    }
                    media.anime?.episodes = episodes

                    //CHIP GROUP
                    val total = episodes.size
                    val divisions = total.toDouble() / 10
                    start = 0
                    end = null
                    val limit = when {
                        (divisions < 25) -> 25
                        (divisions < 50) -> 50
                        else -> 100
                    }
                    headerAdapter.clearChips()
                    if (total > limit) {
                        val arr = media.anime!!.episodes!!.keys.toTypedArray()
                        val stored = ceil((total).toDouble() / limit).toInt()
                        val position = MathUtils.clamp(media.selected!!.chip, 0, stored - 1)
                        val last = if (position + 1 == stored) total else (limit * (position + 1))
                        start = limit * (position)
                        end = last - 1
                        headerAdapter.updateChips(
                            limit,
                            arr,
                            (1..stored).toList().toTypedArray(),
                            position
                        )
                    }

                    headerAdapter.subscribeButton(true)
                    reload()
                }
            }
        }

        model.getKitsuEpisodes().observe(viewLifecycleOwner) { i ->
            if (i != null)
                media.anime?.kitsuEpisodes = i
        }

        model.getFillerEpisodes().observe(viewLifecycleOwner) { i ->
            if (i != null)
                media.anime?.fillerEpisodes = i
        }
    }

    fun onSourceChange(i: Int): AnimeParser {
        media.anime?.episodes = null
        reload()
        val selected = model.loadSelected(media)
        model.watchSources?.get(selected.sourceIndex)?.showUserTextListener = null
        selected.sourceIndex = i
        selected.server = null
        model.saveSelected(media.id, selected)
        media.selected = selected
        return model.watchSources?.get(i)!!
    }

    fun onLangChange(i: Int) {
        val selected = model.loadSelected(media)
        selected.langIndex = i
        model.saveSelected(media.id, selected)
        media.selected = selected
    }

    fun onDubClicked(checked: Boolean) {
        val selected = model.loadSelected(media)
        model.watchSources?.get(selected.sourceIndex)?.selectDub = checked
        selected.preferDub = checked
        model.saveSelected(media.id, selected)
        media.selected = selected
        lifecycleScope.launch(Dispatchers.IO) {
            model.forceLoadEpisode(
                media,
                selected.sourceIndex
            )
        }
    }

    fun loadEpisodes(i: Int, invalidate: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) { model.loadEpisodes(media, i, invalidate) }
    }

    fun onIconPressed(viewType: Int, rev: Boolean) {
        style = viewType
        reverse = rev
        media.selected!!.recyclerStyle = style
        media.selected!!.recyclerReversed = reverse
        model.saveSelected(media.id, media.selected!!)
        reload()
    }

    fun onChipClicked(i: Int, s: Int, e: Int) {
        media.selected!!.chip = i
        start = s
        end = e
        model.saveSelected(media.id, media.selected!!)
        reload()
    }

    var subscribed = false
    fun onNotificationPressed(subscribed: Boolean, source: String) {
        this.subscribed = subscribed
        saveSubscription(media, subscribed)
        snackString(
            if (subscribed) getString(R.string.subscribed_notification, source)
            else getString(R.string.unsubscribed_notification)
        )
    }

    fun openSettings(pkg: AnimeExtension.Installed) {
        val changeUIVisibility: (Boolean) -> Unit = { show ->
            val activity = activity
            if (activity is MediaDetailsActivity && isAdded) {
                val visibility = if (show) View.VISIBLE else View.GONE
                activity.findViewById<AppBarLayout>(R.id.mediaAppBar).visibility = visibility
                activity.findViewById<ViewPager2>(R.id.mediaViewPager).visibility = visibility
                activity.findViewById<CardView>(R.id.mediaCover).visibility = visibility
                activity.findViewById<CardView>(R.id.mediaClose).visibility = visibility

                activity.tabLayout.setVisibility(visibility)

                activity.findViewById<FrameLayout>(R.id.fragmentExtensionsContainer).visibility =
                    if (show) View.GONE else View.VISIBLE
            }
        }
        var itemSelected = false
        val allSettings = pkg.sources.filterIsInstance<ConfigurableAnimeSource>()
        if (allSettings.isNotEmpty()) {
            var selectedSetting = allSettings[0]
            if (allSettings.size > 1) {
                val names =
                    allSettings.map { LanguageMapper.mapLanguageCodeToName(it.lang) }.toTypedArray()
                val dialog = AlertDialog.Builder(requireContext(), R.style.MyPopup)
                    .setTitle("Select a Source")
                    .setSingleChoiceItems(names, -1) { dialog, which ->
                        selectedSetting = allSettings[which]
                        itemSelected = true
                        dialog.dismiss()

                        // Move the fragment transaction here
                        requireActivity().runOnUiThread {
                            val fragment =
                                AnimeSourcePreferencesFragment().getInstance(selectedSetting.id) {
                                    changeUIVisibility(true)
                                    loadEpisodes(media.selected!!.sourceIndex, true)
                                }
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                                .replace(R.id.fragmentExtensionsContainer, fragment)
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                    .setOnDismissListener {
                        if (!itemSelected) {
                            changeUIVisibility(true)
                        }
                    }
                    .show()
                dialog.window?.setDimAmount(0.8f)
            } else {
                // If there's only one setting, proceed with the fragment transaction
                requireActivity().runOnUiThread {
                    val fragment =
                        AnimeSourcePreferencesFragment().getInstance(selectedSetting.id) {
                            changeUIVisibility(true)
                            loadEpisodes(media.selected!!.sourceIndex, true)
                        }
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                        .replace(R.id.fragmentExtensionsContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }

            changeUIVisibility(false)
        } else {
            Toast.makeText(requireContext(), "Source is not configurable", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun onEpisodeClick(i: String) {
        model.continueMedia = false
        model.saveSelected(media.id, media.selected!!)
        model.onEpisodeClick(media, i, requireActivity().supportFragmentManager)
    }

    fun onAnimeEpisodeDownloadClick(i: String) {
        model.onEpisodeClick(media, i, requireActivity().supportFragmentManager, isDownload = true)
    }

    fun onAnimeEpisodeStopDownloadClick(i: String) {
        val cancelIntent = Intent().apply {
            action = AnimeDownloaderService.ACTION_CANCEL_DOWNLOAD
            putExtra(
                AnimeDownloaderService.EXTRA_TASK_NAME,
                AnimeDownloaderService.AnimeDownloadTask.getTaskName(media.mainName(), i)
            )
        }
        requireContext().sendBroadcast(cancelIntent)

        // Remove the download from the manager and update the UI
        downloadManager.removeDownload(
            DownloadedType(
                media.mainName(),
                i,
                DownloadedType.Type.ANIME
            )
        )
        episodeAdapter.purgeDownload(i)
    }

    @OptIn(UnstableApi::class)
    fun onAnimeEpisodeRemoveDownloadClick(i: String) {
        downloadManager.removeDownload(
            DownloadedType(
                media.mainName(),
                i,
                DownloadedType.Type.ANIME
            )
        )
        val taskName = AnimeDownloaderService.AnimeDownloadTask.getTaskName(media.mainName(), i)
        val id = PrefManager.getAnimeDownloadPreferences().getString(
            taskName,
            ""
        ) ?: ""
        PrefManager.getAnimeDownloadPreferences().edit().remove(taskName).apply()
        DownloadService.sendRemoveDownload(
            requireContext(),
            ExoplayerDownloadService::class.java,
            id,
            true
        )
        episodeAdapter.deleteDownload(i)
    }

    private val downloadStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!this@AnimeWatchFragment::episodeAdapter.isInitialized) return
            when (intent.action) {
                ACTION_DOWNLOAD_STARTED -> {
                    val chapterNumber = intent.getStringExtra(EXTRA_EPISODE_NUMBER)
                    chapterNumber?.let { episodeAdapter.startDownload(it) }
                }

                ACTION_DOWNLOAD_FINISHED -> {
                    val chapterNumber = intent.getStringExtra(EXTRA_EPISODE_NUMBER)
                    chapterNumber?.let { episodeAdapter.stopDownload(it) }
                }

                ACTION_DOWNLOAD_FAILED -> {
                    val chapterNumber = intent.getStringExtra(EXTRA_EPISODE_NUMBER)
                    chapterNumber?.let {
                        episodeAdapter.purgeDownload(it)
                    }
                }

                ACTION_DOWNLOAD_PROGRESS -> {
                    val chapterNumber = intent.getStringExtra(EXTRA_EPISODE_NUMBER)
                    val progress = intent.getIntExtra("progress", 0)
                    chapterNumber?.let {
                        episodeAdapter.updateDownloadProgress(it, progress)
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun reload() {
        val selected = model.loadSelected(media)

        //Find latest episode for subscription
        selected.latest =
            media.anime?.episodes?.values?.maxOfOrNull { it.number.toFloatOrNull() ?: 0f } ?: 0f
        selected.latest =
            media.userProgress?.toFloat()?.takeIf { selected.latest < it } ?: selected.latest

        model.saveSelected(media.id, selected)
        headerAdapter.handleEpisodes()
        val isDownloaded = model.watchSources!!.isDownloadedSource(media.selected!!.sourceIndex)
        episodeAdapter.offlineMode = isDownloaded
        episodeAdapter.notifyItemRangeRemoved(0, episodeAdapter.arr.size)
        var arr: ArrayList<Episode> = arrayListOf()
        if (media.anime!!.episodes != null) {
            val end = if (end != null && end!! < media.anime!!.episodes!!.size) end else null
            arr.addAll(
                media.anime!!.episodes!!.values.toList()
                    .slice(start..(end ?: (media.anime!!.episodes!!.size - 1)))
            )
            if (reverse)
                arr = (arr.reversed() as? ArrayList<Episode>) ?: arr
        }
        episodeAdapter.arr = arr
        episodeAdapter.updateType(style ?: PrefManager.getVal(PrefName.AnimeDefaultView))
        episodeAdapter.notifyItemRangeInserted(0, arr.size)
        for (download in downloadManager.animeDownloadedTypes) {
            if (download.title == media.mainName()) {
                episodeAdapter.stopDownload(download.chapter)
            }
        }
    }

    override fun onDestroy() {
        model.watchSources?.flushText()
        super.onDestroy()
        try {
            requireContext().unregisterReceiver(downloadStatusReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    var state: Parcelable? = null
    override fun onResume() {
        super.onResume()
        binding.mediaInfoProgressBar.visibility = progress
        binding.animeSourceRecycler.layoutManager?.onRestoreInstanceState(state)

        requireActivity().setNavigationTheme()
    }

    override fun onPause() {
        super.onPause()
        state = binding.animeSourceRecycler.layoutManager?.onSaveInstanceState()
    }

    companion object {
        const val ACTION_DOWNLOAD_STARTED = "ani.dantotsu.ACTION_DOWNLOAD_STARTED"
        const val ACTION_DOWNLOAD_FINISHED = "ani.dantotsu.ACTION_DOWNLOAD_FINISHED"
        const val ACTION_DOWNLOAD_FAILED = "ani.dantotsu.ACTION_DOWNLOAD_FAILED"
        const val ACTION_DOWNLOAD_PROGRESS = "ani.dantotsu.ACTION_DOWNLOAD_PROGRESS"
        const val EXTRA_EPISODE_NUMBER = "extra_episode_number"
    }

}
