package ani.dantotsu.download.anime


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.LayoutAnimationController
import android.widget.AbsListView
import android.widget.AutoCompleteTextView
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.R
import ani.dantotsu.bottomBar
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.currActivity
import ani.dantotsu.currContext
import ani.dantotsu.download.DownloadCompat.Companion.loadMediaCompat
import ani.dantotsu.download.DownloadCompat.Companion.loadOfflineAnimeModelCompat
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.DownloadsManager.Companion.compareName
import ani.dantotsu.download.findValidName
import ani.dantotsu.getThemeColor
import ani.dantotsu.initActivity
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import ani.dantotsu.util.customAlertDialog
import com.anggrayudi.storage.file.openInputStream
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeImpl
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class OfflineAnimeFragment : Fragment(), OfflineAnimeSearchListener {

    private val downloadManager = Injekt.get<DownloadsManager>()
    private var downloads: List<OfflineAnimeModel> = listOf()
    private lateinit var gridView: GridView
    private lateinit var adapter: OfflineAnimeAdapter
    private lateinit var total: TextView
    private var downloadsJob: Job = Job()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_offline_page, container, false)

        val textInputLayout = view.findViewById<TextInputLayout>(R.id.offlineMangaSearchBar)
        textInputLayout.hint = "Anime"
        val currentColor = textInputLayout.boxBackgroundColor
        val semiTransparentColor = (currentColor and 0x00FFFFFF) or 0xA8000000.toInt()
        textInputLayout.boxBackgroundColor = semiTransparentColor
        val materialCardView = view.findViewById<MaterialCardView>(R.id.offlineMangaAvatarContainer)
        materialCardView.setCardBackgroundColor(semiTransparentColor)
        val color = requireContext().getThemeColor(android.R.attr.windowBackground)

        val animeUserAvatar = view.findViewById<ShapeableImageView>(R.id.offlineMangaUserAvatar)
        animeUserAvatar.setSafeOnClickListener {
            val dialogFragment =
                SettingsDialogFragment.newInstance(SettingsDialogFragment.Companion.PageType.OfflineANIME)
            dialogFragment.show((it.context as AppCompatActivity).supportFragmentManager, "dialog")
        }
        if (!(PrefManager.getVal(PrefName.ImmersiveMode) as Boolean)) {
            view.rootView.fitsSystemWindows = true
        }

        textInputLayout.boxBackgroundColor = (color and 0x00FFFFFF) or 0x28000000
        materialCardView.setCardBackgroundColor((color and 0x00FFFFFF) or 0x28000000)

        val searchView = view.findViewById<AutoCompleteTextView>(R.id.animeSearchBarText)
        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onSearchQuery(s.toString())
            }
        })
        var style: Int = PrefManager.getVal(PrefName.OfflineView)
        val layoutList = view.findViewById<ImageView>(R.id.downloadedList)
        val layoutCompact = view.findViewById<ImageView>(R.id.downloadedGrid)
        var selected = when (style) {
            0 -> layoutList
            1 -> layoutCompact
            else -> layoutList
        }
        selected.alpha = 1f

        fun selected(it: ImageView) {
            selected.alpha = 0.33f
            selected = it
            selected.alpha = 1f
        }

        layoutList.setOnClickListener {
            selected(it as ImageView)
            style = 0
            PrefManager.setVal(PrefName.OfflineView, style)
            gridView.visibility = View.GONE
            gridView = view.findViewById(R.id.gridView)
            adapter.notifyNewGrid()
            grid()
        }

        layoutCompact.setOnClickListener {
            selected(it as ImageView)
            style = 1
            PrefManager.setVal(PrefName.OfflineView, style)
            gridView.visibility = View.GONE
            gridView = view.findViewById(R.id.gridView1)
            adapter.notifyNewGrid()
            grid()
        }

        gridView =
            if (style == 0) view.findViewById(R.id.gridView) else view.findViewById(R.id.gridView1)
        total = view.findViewById(R.id.total)
        grid()
        return view
    }

    @OptIn(UnstableApi::class)
    private fun grid() {
        gridView.visibility = View.VISIBLE
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 300 // animations  pog
        gridView.layoutAnimation = LayoutAnimationController(fadeIn)
        adapter = OfflineAnimeAdapter(requireContext(), downloads, this)
        getDownloads()
        gridView.adapter = adapter
        gridView.scheduleLayoutAnimation()
        total.text = if (gridView.count > 0) "Anime (${gridView.count})" else "Empty List"
        gridView.setOnItemClickListener { _, _, position, _ ->
            // Get the OfflineAnimeModel that was clicked
            val item = adapter.getItem(position) as OfflineAnimeModel
            val media =
                downloadManager.animeDownloadedTypes.firstOrNull { it.titleName.compareName(item.title) }
            media?.let {
                lifecycleScope.launch {
                    val mediaModel = getMedia(it)
                    if (mediaModel == null) {
                        snackString("Error loading media.json")
                        return@launch
                    }
                    MediaDetailsActivity.mediaSingleton = mediaModel
                    ContextCompat.startActivity(
                        requireActivity(),
                        Intent(requireContext(), MediaDetailsActivity::class.java)
                            .putExtra("download", true),
                        null
                    )
                }
            } ?: run {
                snackString("no media found")
            }
        }
        gridView.setOnItemLongClickListener { _, _, position, _ ->
            // Get the OfflineAnimeModel that was clicked
            val item = adapter.getItem(position) as OfflineAnimeModel
            val type: MediaType = MediaType.ANIME

            // Alert dialog to confirm deletion
            requireContext().customAlertDialog().apply {
                setTitle("Delete ${item.title}?")
                setMessage("Are you sure you want to delete ${item.title}?")
                setPosButton(R.string.yes) {
                    downloadManager.removeMedia(item.title, type)
                    val mediaIds =
                        PrefManager.getAnimeDownloadPreferences().all?.filter { it.key.contains(item.title) }?.values
                            ?: emptySet()
                    if (mediaIds.isEmpty()) {
                        snackString("No media found")  // if this happens, terrible things have happened
                    }
                    getDownloads()
                }
                setNegButton(R.string.no) {
                    // Do nothing
                }
                show()
            }
            true
        }
    }

    override fun onSearchQuery(query: String) {
        adapter.onSearchQuery(query)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollTop = view.findViewById<CardView>(R.id.mangaPageScrollTop)
        scrollTop.setOnClickListener {
            gridView.smoothScrollToPositionFromTop(0, 0)
        }

        // Assuming 'scrollTop' is a view that you want to hide/show
        scrollTop.visibility = View.GONE

        gridView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            }

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val first = view.getChildAt(0)
                val visibility = first != null && first.top < 0
                scrollTop.translationY =
                    -(navBarHeight + bottomBar.height + bottomBar.marginBottom).toFloat()
                scrollTop.isVisible = visibility
            }
        })
        initActivity(requireActivity())

    }

    override fun onResume() {
        super.onResume()
        getDownloads()
    }

    override fun onPause() {
        super.onPause()
        downloads = listOf()
    }

    override fun onDestroy() {
        super.onDestroy()
        downloads = listOf()
    }

    override fun onStop() {
        super.onStop()
        downloads = listOf()
    }

    private fun getDownloads() {
        downloads = listOf()
        if (downloadsJob.isActive) {
            downloadsJob.cancel()
        }
        downloadsJob = Job()
        CoroutineScope(Dispatchers.IO + downloadsJob).launch {
            val animeTitles =
                downloadManager.animeDownloadedTypes.map { it.titleName.findValidName() }.distinct()
            val newAnimeDownloads = mutableListOf<OfflineAnimeModel>()
            for (title in animeTitles) {
                val tDownloads =
                    downloadManager.animeDownloadedTypes.filter { it.titleName.findValidName() == title }
                val download = tDownloads.firstOrNull() ?: continue
                val offlineAnimeModel = loadOfflineAnimeModel(download)
                if (offlineAnimeModel.title == "unknown") offlineAnimeModel.title = title
                newAnimeDownloads += offlineAnimeModel
            }
            downloads = newAnimeDownloads
            withContext(Dispatchers.Main) {
                adapter.setItems(downloads)
                total.text = if (gridView.count > 0) "Anime (${gridView.count})" else "Empty List"
                adapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * Load media.json file from the directory and convert it to Media class
     * @param downloadedType DownloadedType object
     * @return Media object
     */
    private suspend fun getMedia(downloadedType: DownloadedType): Media? {
        return try {
            val directory = DownloadsManager.getSubDirectory(
                context ?: currContext()!!, downloadedType.type,
                false, downloadedType.titleName
            )
            val gson = GsonBuilder()
                .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                    SChapterImpl()
                })
                .registerTypeAdapter(SAnime::class.java, InstanceCreator<SAnime> {
                    SAnimeImpl()
                })
                .registerTypeAdapter(SEpisode::class.java, InstanceCreator<SEpisode> {
                    SEpisodeImpl()
                })
                .create()
            val media = directory?.findFile("media.json")
            if (media == null) {
                Logger.log("No media.json found at ${directory?.uri?.path}")
                return loadMediaCompat(downloadedType)
            }
            val mediaJson =
                media.openInputStream(context ?: currContext()!!)?.bufferedReader().use {
                    it?.readText()
                }
                    ?: return null
            gson.fromJson(mediaJson, Media::class.java)
        } catch (e: Exception) {
            Logger.log("Error loading media.json: ${e.message}")
            Logger.log(e)
            Injekt.get<CrashlyticsInterface>().logException(e)
            null
        }
    }

    /**
     * Load OfflineAnimeModel from the directory
     * @param downloadedType DownloadedType object
     * @return OfflineAnimeModel object
     */
    private suspend fun loadOfflineAnimeModel(downloadedType: DownloadedType): OfflineAnimeModel {
        val type = downloadedType.type.asText()
        try {
            val directory = DownloadsManager.getSubDirectory(
                context ?: currContext()!!, downloadedType.type,
                false, downloadedType.titleName
            )
            val mediaModel = getMedia(downloadedType)!!
            val cover = directory?.findFile("cover.jpg")
            val coverUri: Uri? = if (cover?.exists() == true) {
                cover.uri
            } else null
            val banner = directory?.findFile("banner.jpg")
            val bannerUri: Uri? = if (banner?.exists() == true) {
                banner.uri
            } else null
            if (coverUri == null && bannerUri == null) throw Exception("No cover or banner found, probably compat")
            val title = mediaModel.mainName()
            val score = ((if (mediaModel.userScore == 0) (mediaModel.meanScore
                ?: 0) else mediaModel.userScore) / 10.0).toString()
            val isOngoing =
                mediaModel.status == currActivity()!!.getString(R.string.status_releasing)
            val isUserScored = mediaModel.userScore != 0
            val watchedEpisodes = (mediaModel.userProgress ?: "~").toString()
            val totalEpisode =
                if (mediaModel.anime?.nextAiringEpisode != null) (mediaModel.anime.nextAiringEpisode.toString() + " | " + (mediaModel.anime.totalEpisodes
                    ?: "~").toString()) else (mediaModel.anime?.totalEpisodes ?: "~").toString()
            val chapters = " Chapters"
            val totalEpisodesList =
                if (mediaModel.anime?.nextAiringEpisode != null) (mediaModel.anime.nextAiringEpisode.toString()) else (mediaModel.anime?.totalEpisodes
                    ?: "~").toString()
            return OfflineAnimeModel(
                title,
                score,
                totalEpisode,
                totalEpisodesList,
                watchedEpisodes,
                type,
                chapters,
                isOngoing,
                isUserScored,
                coverUri,
                bannerUri
            )
        } catch (e: Exception) {
            Logger.log(e)
            return try {
                loadOfflineAnimeModelCompat(downloadedType)
            } catch (e: Exception) {
                Logger.log("Error loading media.json: ${e.message}")
                Logger.log(e)
                Injekt.get<CrashlyticsInterface>().logException(e)
                OfflineAnimeModel(
                    downloadedType.titleName,
                    "0",
                    "??",
                    "??",
                    "??",
                    "movie",
                    "hmm",
                    isOngoing = false,
                    isUserScored = false,
                    null,
                    null
                )
            }
        }
    }
}

interface OfflineAnimeSearchListener {
    fun onSearchQuery(query: String)
}
