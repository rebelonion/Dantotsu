package ani.dantotsu.download.anime


import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.LayoutAnimationController
import android.view.animation.OvershootInterpolator
import android.widget.AbsListView
import android.widget.AutoCompleteTextView
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.R
import ani.dantotsu.bottomBar
import ani.dantotsu.currActivity
import ani.dantotsu.currContext
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.initActivity
import ani.dantotsu.loadData
import ani.dantotsu.logger
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.settings.UserInterfaceSettings
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeImpl
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import kotlin.math.max
import kotlin.math.min

class OfflineAnimeFragment : Fragment(), OfflineAnimeSearchListener {

    private val downloadManager = Injekt.get<DownloadsManager>()
    private var downloads: List<OfflineAnimeModel> = listOf()
    private lateinit var gridView: GridView
    private lateinit var adapter: OfflineAnimeAdapter
    private lateinit var total : TextView
    private var uiSettings: UserInterfaceSettings =
        loadData("ui_settings") ?: UserInterfaceSettings()

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
        val typedValue = TypedValue()
        requireContext().theme?.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        val color = typedValue.data

        val animeUserAvatar = view.findViewById<ShapeableImageView>(R.id.offlineMangaUserAvatar)
        animeUserAvatar.setSafeOnClickListener {
            val dialogFragment =
                SettingsDialogFragment.newInstance(SettingsDialogFragment.Companion.PageType.OfflineANIME)
            dialogFragment.show((it.context as AppCompatActivity).supportFragmentManager, "dialog")
        }
        if (!uiSettings.immersiveMode) {
            view.rootView.fitsSystemWindows = true
        }
        val colorOverflow = currContext()?.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            ?.getBoolean("colorOverflow", false) ?: false
        if (!colorOverflow) {
            textInputLayout.boxBackgroundColor = (color and 0x00FFFFFF) or 0x28000000.toInt()
            materialCardView.setCardBackgroundColor((color and 0x00FFFFFF) or 0x28000000.toInt())
        }

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
        var style = context?.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            ?.getInt("offline_view", 0)
        val layoutList = view.findViewById<ImageView>(R.id.downloadedList)
        val layoutcompact = view.findViewById<ImageView>(R.id.downloadedGrid)
        var selected = when (style) {
            0 -> layoutList
            1 -> layoutcompact
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
            context?.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)?.edit()
                ?.putInt("offline_view", style!!)?.apply()
            gridView.visibility = View.GONE
            gridView = view.findViewById(R.id.gridView)
            adapter.notifyNewGrid()
            grid()
        }

        layoutcompact.setOnClickListener {
            selected(it as ImageView)
            style = 1
            context?.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)?.edit()
                ?.putInt("offline_view", style!!)?.apply()
            gridView.visibility = View.GONE
            gridView = view.findViewById(R.id.gridView1)
            adapter.notifyNewGrid()
            grid()
        }

        gridView = if (style == 0) view.findViewById(R.id.gridView) else view.findViewById(R.id.gridView1)
        total = view.findViewById(R.id.total)
        grid()
        return view
    }

    @OptIn(UnstableApi::class)
    private fun grid() {
        gridView.visibility = View.VISIBLE
        getDownloads()
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 300 // animations  pog
        gridView.layoutAnimation = LayoutAnimationController(fadeIn)
        adapter = OfflineAnimeAdapter(requireContext(), downloads, this)
        gridView.adapter = adapter
        gridView.scheduleLayoutAnimation()
        total.text = if (gridView.count > 0) "Anime (${gridView.count})" else "Empty List"
        gridView.setOnItemClickListener { _, _, position, _ ->
            // Get the OfflineAnimeModel that was clicked
            val item = adapter.getItem(position) as OfflineAnimeModel
            val media =
                downloadManager.animeDownloadedTypes.firstOrNull { it.title == item.title }
            media?.let {
                val mediaModel = getMedia(it)
                if (mediaModel == null) {
                    snackString("Error loading media.json")
                    return@let
                }
                MediaDetailsActivity.mediaSingleton = mediaModel
                startActivity(
                    Intent(requireContext(), MediaDetailsActivity::class.java)
                        .putExtra("download", true)
                )
            } ?: run {
                snackString("no media found")
            }
        }
        gridView.setOnItemLongClickListener { _, _, position, _ ->
            // Get the OfflineAnimeModel that was clicked
            val item = adapter.getItem(position) as OfflineAnimeModel
            val type: DownloadedType.Type =
                DownloadedType.Type.ANIME

            // Alert dialog to confirm deletion
            val builder =
                androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.MyPopup)
            builder.setTitle("Delete ${item.title}?")
            builder.setMessage("Are you sure you want to delete ${item.title}?")
            builder.setPositiveButton("Yes") { _, _ ->
                downloadManager.removeMedia(item.title, type)
                val mediaIds = requireContext().getSharedPreferences(
                    getString(R.string.anime_downloads),
                    Context.MODE_PRIVATE
                )
                    ?.all?.filter { it.key.contains(item.title) }?.values ?: emptySet()
                if (mediaIds.isEmpty()) {
                    snackString("No media found")  // if this happens, terrible things have happened
                }
                for (mediaId in mediaIds) {
                    ani.dantotsu.download.video.Helper.downloadManager(requireContext())
                        .removeDownload(mediaId.toString())
                }
                getDownloads()
                adapter.setItems(downloads)
                total.text = if (gridView.count > 0) "Anime (${gridView.count})" else "Empty List"
            }
            builder.setNegativeButton("No") { _, _ ->
                // Do nothing
            }
            val dialog = builder.show()
            dialog.window?.setDimAmount(0.8f)
            true
        }
    }

    override fun onSearchQuery(query: String) {
        adapter.onSearchQuery(query)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        val scrollTop = view.findViewById<CardView>(R.id.mangaPageScrollTop)
        scrollTop.translationY =
            -(navBarHeight + bottomBar.height + bottomBar.marginBottom).toFloat()
        val visible = false

        fun animate() {
            val start = if (visible) 0f else 1f
            val end = if (!visible) 0f else 1f
            ObjectAnimator.ofFloat(scrollTop, "scaleX", start, end).apply {
                duration = 300
                interpolator = OvershootInterpolator(2f)
                start()
            }
            ObjectAnimator.ofFloat(scrollTop, "scaleY", start, end).apply {
                duration = 300
                interpolator = OvershootInterpolator(2f)
                start()
            }
        }

        scrollTop.setOnClickListener {
            gridView.smoothScrollToPositionFromTop(0, 0)
        }

        // Assuming 'scrollTop' is a view that you want to hide/show
        scrollTop.visibility = View.GONE

        gridView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                // Implement behavior for different scroll states if needed
            }

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val first = view.getChildAt(0)
                val visibility = first != null && first.top < -height
                scrollTop.visibility = if (visibility) View.VISIBLE else View.GONE
            }
        })
        initActivity(requireActivity())

    }

    override fun onResume() {
        super.onResume()
        getDownloads()
        adapter.notifyDataSetChanged()
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
        val animeTitles = downloadManager.animeDownloadedTypes.map { it.title }.distinct()
        val newAnimeDownloads = mutableListOf<OfflineAnimeModel>()
        for (title in animeTitles) {
            val _downloads = downloadManager.animeDownloadedTypes.filter { it.title == title }
            val download = _downloads.first()
            val offlineAnimeModel = loadOfflineAnimeModel(download)
            newAnimeDownloads += offlineAnimeModel
        }
        downloads = newAnimeDownloads
    }

    private fun getMedia(downloadedType: DownloadedType): Media? {
        val type = if (downloadedType.type == DownloadedType.Type.ANIME) {
            "Anime"
        } else if (downloadedType.type == DownloadedType.Type.MANGA) {
            "Manga"
        } else {
            "Novel"
        }
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/$type/${downloadedType.title}"
        )
        //load media.json and convert to media class with gson
        return try {
            val gson = GsonBuilder()
                .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                    SChapterImpl() // Provide an instance of SChapterImpl
                })
                .registerTypeAdapter(SAnime::class.java, InstanceCreator<SAnime> {
                    SAnimeImpl() // Provide an instance of SAnimeImpl
                })
                .registerTypeAdapter(SEpisode::class.java, InstanceCreator<SEpisode> {
                    SEpisodeImpl() // Provide an instance of SEpisodeImpl
                })
                .create()
            val media = File(directory, "media.json")
            val mediaJson = media.readText()
            gson.fromJson(mediaJson, Media::class.java)
        } catch (e: Exception) {
            logger("Error loading media.json: ${e.message}")
            logger(e.printStackTrace())
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }

    private fun loadOfflineAnimeModel(downloadedType: DownloadedType): OfflineAnimeModel {
        val type = if (downloadedType.type == DownloadedType.Type.MANGA) {
            "Manga"
        } else if (downloadedType.type == DownloadedType.Type.ANIME) {
            "Anime"
        } else {
            "Novel"
        }
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/$type/${downloadedType.title}"
        )
        //load media.json and convert to media class with gson
        try {
            val media = File(directory, "media.json")
            val mediaJson = media.readText()
            val mediaModel = getMedia(downloadedType)!!
            val cover = File(directory, "cover.jpg")
            val coverUri: Uri? = if (cover.exists()) {
                Uri.fromFile(cover)
            } else null
            val banner = File(directory, "banner.jpg")
            val bannerUri: Uri? = if (banner.exists()) {
                Uri.fromFile(banner)
            } else null
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
            logger("Error loading media.json: ${e.message}")
            logger(e.printStackTrace())
            FirebaseCrashlytics.getInstance().recordException(e)
            return OfflineAnimeModel(
                "unknown",
                "0",
                "??",
                "??",
                "??",
                "movie",
                "hmm",
                false,
                false,
                null,
                null
            )
        }
    }
}

interface OfflineAnimeSearchListener {
    fun onSearchQuery(query: String)
}
