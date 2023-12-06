package ani.dantotsu.download.manga

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
import android.view.animation.OvershootInterpolator
import android.widget.AutoCompleteTextView
import android.widget.GridView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import ani.dantotsu.R
import ani.dantotsu.currContext
import ani.dantotsu.download.Download
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.logger
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import kotlin.math.max
import kotlin.math.min

class OfflineMangaFragment : Fragment(), OfflineMangaSearchListener {
    private val downloadManager = Injekt.get<DownloadsManager>()
    private var downloads: List<OfflineMangaModel> = listOf()
    private lateinit var gridView: GridView
    private lateinit var adapter: OfflineMangaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manga_offline, container, false)

        val textInputLayout = view.findViewById<TextInputLayout>(R.id.offlineMangaSearchBar)
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
            SettingsDialogFragment(SettingsDialogFragment.Companion.PageType.HOME).show(
                (it.context as AppCompatActivity).supportFragmentManager,
                "dialog"
            )
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

        gridView = view.findViewById(R.id.gridView)
        getDownloads()
        adapter = OfflineMangaAdapter(requireContext(), downloads, this)
        gridView.adapter = adapter
        gridView.setOnItemClickListener { parent, view, position, id ->
            // Get the OfflineMangaModel that was clicked
            val item = adapter.getItem(position) as OfflineMangaModel
            val media =
                downloadManager.mangaDownloads.firstOrNull { it.title == item.title }
                    ?: downloadManager.novelDownloads.firstOrNull { it.title == item.title }
            media?.let {
                startActivity(
                    Intent(requireContext(), MediaDetailsActivity::class.java)
                        .putExtra("media", getMedia(it))
                        .putExtra("download", true)
                )
            } ?: run {
                snackString("no media found")
            }
        }

        gridView.setOnItemLongClickListener { parent, view, position, id ->
            // Get the OfflineMangaModel that was clicked
            val item = adapter.getItem(position) as OfflineMangaModel
            val type: Download.Type = if (downloadManager.mangaDownloads.any { it.title == item.title }) {
                Download.Type.MANGA
            } else {
                Download.Type.NOVEL
            }
            // Alert dialog to confirm deletion
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.MyPopup)
            builder.setTitle("Delete ${item.title}?")
            builder.setMessage("Are you sure you want to delete ${item.title}?")
            builder.setPositiveButton("Yes") { _, _ ->
                downloadManager.removeMedia(item.title, type)
                getDownloads()
                adapter.setItems(downloads)
            }
            builder.setNegativeButton("No") { _, _ ->
                // Do nothing
            }
             val dialog = builder.show()
            dialog.window?.setDimAmount(0.8f)
            true
        }

        return view
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
        var visible = false
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
            //TODO: scroll to top
        }

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
        val mangaTitles = downloadManager.mangaDownloads.map { it.title }.distinct()
        val newMangaDownloads = mutableListOf<OfflineMangaModel>()
        for (title in mangaTitles) {
            val _downloads = downloadManager.mangaDownloads.filter { it.title == title }
            val download = _downloads.first()
            val offlineMangaModel = loadOfflineMangaModel(download)
            newMangaDownloads += offlineMangaModel
        }
        downloads = newMangaDownloads
        val novelTitles = downloadManager.novelDownloads.map { it.title }.distinct()
        val newNovelDownloads = mutableListOf<OfflineMangaModel>()
        for (title in novelTitles) {
            val _downloads = downloadManager.novelDownloads.filter { it.title == title }
            val download = _downloads.first()
            val offlineMangaModel = loadOfflineMangaModel(download)
            newNovelDownloads += offlineMangaModel
        }
        downloads += newNovelDownloads

    }

    private fun getMedia(download: Download): Media? {
        val type = if (download.type == Download.Type.MANGA) {
            "Manga"
        } else if (download.type == Download.Type.ANIME) {
            "Anime"
        } else {
            "Novel"
        }
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/$type/${download.title}"
        )
        //load media.json and convert to media class with gson
        return try {
            val gson = GsonBuilder()
                .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                    SChapterImpl() // Provide an instance of SChapterImpl
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

    private fun loadOfflineMangaModel(download: Download): OfflineMangaModel {
        val type = if (download.type == Download.Type.MANGA) {
            "Manga"
        } else if (download.type == Download.Type.ANIME) {
            "Anime"
        } else {
            "Novel"
        }
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/$type/${download.title}"
        )
        //load media.json and convert to media class with gson
        try {
            val media = File(directory, "media.json")
            val mediaJson = media.readText()
            val mediaModel = getMedia(download)!!
            val cover = File(directory, "cover.jpg")
            val coverUri: Uri? = if (cover.exists()) {
                Uri.fromFile(cover)
            } else {
                null
            }
            val title = mediaModel.nameMAL ?: mediaModel.nameRomaji
            val score = ((if (mediaModel.userScore == 0) (mediaModel.meanScore
                ?: 0) else mediaModel.userScore) / 10.0).toString()
            val isOngoing = false
            val isUserScored = mediaModel.userScore != 0
            return OfflineMangaModel(title, score, isOngoing, isUserScored, coverUri)
        } catch (e: Exception) {
            logger("Error loading media.json: ${e.message}")
            logger(e.printStackTrace())
            FirebaseCrashlytics.getInstance().recordException(e)
            return OfflineMangaModel("unknown", "0", false, false, null)
        }
    }
}

interface OfflineMangaSearchListener {
    fun onSearchQuery(query: String)
}