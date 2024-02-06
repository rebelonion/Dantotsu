package ani.dantotsu.media.manga

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils.clamp
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.*
import ani.dantotsu.databinding.FragmentAnimeWatchBinding
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.manga.MangaDownloaderService
import ani.dantotsu.download.manga.MangaServiceDataSingleton
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.manga.mangareader.ChapterLoaderDialog
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.DynamicMangaParser
import ani.dantotsu.parsers.HMangaSources
import ani.dantotsu.parsers.MangaParser
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.settings.extensionprefs.MangaSourcePreferencesFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.subcriptions.Notifications
import ani.dantotsu.subcriptions.Notifications.Group.MANGA_GROUP
import ani.dantotsu.subcriptions.Subscription.Companion.getChannelId
import ani.dantotsu.subcriptions.SubscriptionHelper
import ani.dantotsu.subcriptions.SubscriptionHelper.Companion.saveSubscription
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigationrail.NavigationRailView
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.source.ConfigurableSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

open class MangaReadFragment : Fragment(), ScanlatorSelectionListener {
    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!
    private val model: MediaDetailsViewModel by activityViewModels()

    private lateinit var media: Media

    private var start = 0
    private var end: Int? = null
    private var style: Int? = null
    private var reverse = false

    private lateinit var headerAdapter: MangaReadAdapter
    private lateinit var chapterAdapter: MangaChapterAdapter

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
                val style = chapterAdapter.getItemViewType(position)

                return when (position) {
                    0 -> maxGridSize
                    else -> when (style) {
                        0 -> maxGridSize
                        1 -> 1
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
                progress = View.GONE
                binding.mediaInfoProgressBar.visibility = progress

                if (media.format == "MANGA" || media.format == "ONE SHOT") {
                    media.selected = model.loadSelected(media)

                    subscribed =
                        SubscriptionHelper.getSubscriptions().containsKey(media.id)

                    style = media.selected!!.recyclerStyle
                    reverse = media.selected!!.recyclerReversed

                    if (!loaded) {
                        model.mangaReadSources = if (media.isAdult) HMangaSources else MangaSources

                        headerAdapter = MangaReadAdapter(it, this, model.mangaReadSources!!)
                        headerAdapter.scanlatorSelectionListener = this
                        chapterAdapter =
                            MangaChapterAdapter(
                                style ?: PrefManager.getVal(PrefName.MangaDefaultView), media, this
                            )

                        for (download in downloadManager.mangaDownloadedTypes) {
                            if (download.title == media.mainName()) {
                                chapterAdapter.stopDownload(download.chapter)
                            }
                        }

                        binding.animeSourceRecycler.adapter =
                            ConcatAdapter(headerAdapter, chapterAdapter)

                        lifecycleScope.launch(Dispatchers.IO) {
                            model.loadMangaChapters(media, media.selected!!.sourceIndex)
                        }
                        loaded = true
                    } else {
                        reload()
                    }
                } else {
                    binding.animeNotSupported.visibility = View.VISIBLE
                    binding.animeNotSupported.text =
                        getString(R.string.not_supported, media.format ?: "")
                }
            }
        }

        model.getMangaChapters().observe(viewLifecycleOwner) { _ ->
            updateChapters()
        }
    }

    override fun onScanlatorsSelected() {
        updateChapters()
    }

    fun multiDownload(n: Int) {
        //get last viewed chapter
        val selected = media.userProgress
        val chapters = media.manga?.chapters?.values?.toList()
        //filter by selected language
        val progressChapterIndex = (chapters?.indexOfFirst {
            MangaNameAdapter.findChapterNumber(it.number)?.toInt() == selected
        } ?: 0) + 1

        if (progressChapterIndex < 0 || n < 1 || chapters == null) return

        // Calculate the end index
        val endIndex = minOf(progressChapterIndex + n, chapters.size)

        //make sure there are enough chapters
        val chaptersToDownload = chapters.subList(progressChapterIndex, endIndex)


        for (chapter in chaptersToDownload) {
            onMangaChapterDownloadClick(chapter.title!!)
        }
    }


    private fun updateChapters() {
        val loadedChapters = model.getMangaChapters().value
        if (loadedChapters != null) {
            val chapters = loadedChapters[media.selected!!.sourceIndex]
            if (chapters != null) {
                headerAdapter.options = getScanlators(chapters)
                val filteredChapters = chapters.filterNot { (_, chapter) ->
                    chapter.scanlator in headerAdapter.hiddenScanlators
                }

                media.manga?.chapters = filteredChapters.toMutableMap()

                //CHIP GROUP
                val total = filteredChapters.size
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
                    val arr = filteredChapters.keys.toTypedArray()
                    val stored = ceil((total).toDouble() / limit).toInt()
                    val position = clamp(media.selected!!.chip, 0, stored - 1)
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

    private fun getScanlators(chap: MutableMap<String, MangaChapter>?): List<String> {
        val scanlators = mutableListOf<String>()
        if (chap != null) {
            val chapters = chap.values
            for (chapter in chapters) {
                scanlators.add(chapter.scanlator ?: "Unknown")
            }
        }
        return scanlators.distinct()
    }

    fun onSourceChange(i: Int): MangaParser {
        media.manga?.chapters = null
        reload()
        val selected = model.loadSelected(media)
        model.mangaReadSources?.get(selected.sourceIndex)?.showUserTextListener = null
        selected.sourceIndex = i
        selected.server = null
        model.saveSelected(media.id, selected)
        media.selected = selected
        return model.mangaReadSources?.get(i)!!
    }

    fun onLangChange(i: Int) {
        val selected = model.loadSelected(media)
        selected.langIndex = i
        model.saveSelected(media.id, selected)
        media.selected = selected
    }

    fun onScanlatorChange(list: List<String>) {
        val selected = model.loadSelected(media)
        selected.scanlators = list
        model.saveSelected(media.id, selected)
        media.selected = selected
    }

    fun loadChapters(i: Int, invalidate: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) { model.loadMangaChapters(media, i, invalidate) }
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
        saveSubscription(requireContext(), media, subscribed)
        if (!subscribed)
            Notifications.deleteChannel(requireContext(), getChannelId(true, media.id))
        else
            Notifications.createChannel(
                requireContext(),
                MANGA_GROUP,
                getChannelId(true, media.id),
                media.userPreferredName
            )
        snackString(
            if (subscribed) getString(R.string.subscribed_notification, source)
            else getString(R.string.unsubscribed_notification)
        )
    }

    fun openSettings(pkg: MangaExtension.Installed) {
        val changeUIVisibility: (Boolean) -> Unit = { show ->
            val activity = activity
            if (activity is MediaDetailsActivity && isAdded) {
                val visibility = if (show) View.VISIBLE else View.GONE
                activity.findViewById<AppBarLayout>(R.id.mediaAppBar).visibility = visibility
                activity.findViewById<ViewPager2>(R.id.mediaViewPager).visibility = visibility
                activity.findViewById<CardView>(R.id.mediaCover).visibility = visibility
                activity.findViewById<CardView>(R.id.mediaClose).visibility = visibility
                try {
                    activity.findViewById<CustomBottomNavBar>(R.id.mediaTab).visibility = visibility
                } catch (e: ClassCastException) {
                    activity.findViewById<NavigationRailView>(R.id.mediaTab).visibility = visibility
                }
                activity.findViewById<FrameLayout>(R.id.fragmentExtensionsContainer).visibility =
                    if (show) View.GONE else View.VISIBLE
            }
        }
        var itemSelected = false
        val allSettings = pkg.sources.filterIsInstance<ConfigurableSource>()
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
                        val fragment =
                            MangaSourcePreferencesFragment().getInstance(selectedSetting.id) {
                                changeUIVisibility(true)
                                loadChapters(media.selected!!.sourceIndex, true)
                            }
                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                            .replace(R.id.fragmentExtensionsContainer, fragment)
                            .addToBackStack(null)
                            .commit()
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
                val fragment = MangaSourcePreferencesFragment().getInstance(selectedSetting.id) {
                    changeUIVisibility(true)
                    loadChapters(media.selected!!.sourceIndex, true)
                }
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                    .replace(R.id.fragmentExtensionsContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            changeUIVisibility(false)
        } else {
            Toast.makeText(requireContext(), "Source is not configurable", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun onMangaChapterClick(i: String) {
        model.continueMedia = false
        media.manga?.chapters?.get(i)?.let {
            media.manga?.selectedChapter = i
            model.saveSelected(media.id, media.selected!!)
            ChapterLoaderDialog.newInstance(it, true)
                .show(requireActivity().supportFragmentManager, "dialog")
        }
    }

    fun onMangaChapterDownloadClick(i: String) {
        if (!isNotificationPermissionGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        model.continueMedia = false
        media.manga?.chapters?.get(i)?.let { chapter ->
            val parser =
                model.mangaReadSources?.get(media.selected!!.sourceIndex) as? DynamicMangaParser
            parser?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    val images = parser.imageList("", chapter.sChapter)

                    // Create a download task
                    val downloadTask = MangaDownloaderService.DownloadTask(
                        title = media.mainName(),
                        chapter = chapter.title!!,
                        imageData = images,
                        sourceMedia = media,
                        retries = 2,
                        simultaneousDownloads = 2
                    )

                    MangaServiceDataSingleton.downloadQueue.offer(downloadTask)

                    // If the service is not already running, start it
                    if (!MangaServiceDataSingleton.isServiceRunning) {
                        val intent = Intent(context, MangaDownloaderService::class.java)
                        withContext(Dispatchers.Main) {
                            ContextCompat.startForegroundService(requireContext(), intent)
                        }
                        MangaServiceDataSingleton.isServiceRunning = true
                    }

                    // Inform the adapter that the download has started
                    withContext(Dispatchers.Main) {
                        chapterAdapter.startDownload(i)
                    }
                }
            }
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }


    fun onMangaChapterRemoveDownloadClick(i: String) {
        downloadManager.removeDownload(
            DownloadedType(
                media.mainName(),
                i,
                DownloadedType.Type.MANGA
            )
        )
        chapterAdapter.deleteDownload(i)
    }

    fun onMangaChapterStopDownloadClick(i: String) {
        val cancelIntent = Intent().apply {
            action = MangaDownloaderService.ACTION_CANCEL_DOWNLOAD
            putExtra(MangaDownloaderService.EXTRA_CHAPTER, i)
        }
        requireContext().sendBroadcast(cancelIntent)

        // Remove the download from the manager and update the UI
        downloadManager.removeDownload(
            DownloadedType(
                media.mainName(),
                i,
                DownloadedType.Type.MANGA
            )
        )
        chapterAdapter.purgeDownload(i)
    }

    private val downloadStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!this@MangaReadFragment::chapterAdapter.isInitialized) return
            when (intent.action) {
                ACTION_DOWNLOAD_STARTED -> {
                    val chapterNumber = intent.getStringExtra(EXTRA_CHAPTER_NUMBER)
                    chapterNumber?.let { chapterAdapter.startDownload(it) }
                }

                ACTION_DOWNLOAD_FINISHED -> {
                    val chapterNumber = intent.getStringExtra(EXTRA_CHAPTER_NUMBER)
                    chapterNumber?.let { chapterAdapter.stopDownload(it) }
                }

                ACTION_DOWNLOAD_FAILED -> {
                    val chapterNumber = intent.getStringExtra(EXTRA_CHAPTER_NUMBER)
                    chapterNumber?.let {
                        chapterAdapter.purgeDownload(it)
                    }
                }

                ACTION_DOWNLOAD_PROGRESS -> {
                    val chapterNumber = intent.getStringExtra(EXTRA_CHAPTER_NUMBER)
                    val progress = intent.getIntExtra("progress", 0)
                    chapterNumber?.let {
                        chapterAdapter.updateDownloadProgress(it, progress)
                    }
                }
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun reload() {
        val selected = model.loadSelected(media)

        //Find latest chapter for subscription
        selected.latest =
            media.manga?.chapters?.values?.maxOfOrNull { it.number.toFloatOrNull() ?: 0f } ?: 0f
        selected.latest =
            media.userProgress?.toFloat()?.takeIf { selected.latest < it } ?: selected.latest

        model.saveSelected(media.id, selected)
        headerAdapter.handleChapters()
        chapterAdapter.notifyItemRangeRemoved(0, chapterAdapter.arr.size)
        var arr: ArrayList<MangaChapter> = arrayListOf()
        if (media.manga!!.chapters != null) {
            val end = if (end != null && end!! < media.manga!!.chapters!!.size) end else null
            arr.addAll(
                media.manga!!.chapters!!.values.toList()
                    .slice(start..(end ?: (media.manga!!.chapters!!.size - 1)))
            )
            if (reverse)
                arr = (arr.reversed() as? ArrayList<MangaChapter>) ?: arr
        }
        chapterAdapter.arr = arr
        chapterAdapter.updateType(style ?: PrefManager.getVal(PrefName.MangaDefaultView))
        chapterAdapter.notifyItemRangeInserted(0, arr.size)
    }

    override fun onDestroy() {
        model.mangaReadSources?.flushText()
        super.onDestroy()
        requireContext().unregisterReceiver(downloadStatusReceiver)
    }

    private var state: Parcelable? = null
    override fun onResume() {
        super.onResume()
        binding.mediaInfoProgressBar.visibility = progress
        binding.animeSourceRecycler.layoutManager?.onRestoreInstanceState(state)
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
        const val EXTRA_CHAPTER_NUMBER = "extra_chapter_number"
    }
}