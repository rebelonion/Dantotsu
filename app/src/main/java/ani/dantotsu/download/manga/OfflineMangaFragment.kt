package ani.dantotsu.download.manga

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.GridView
import androidx.cardview.widget.CardView
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.currContext
import ani.dantotsu.databinding.FragmentMangaBinding
import ani.dantotsu.download.Download
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.logger
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.px
import ani.dantotsu.statusBarHeight
import com.google.firebase.crashlytics.FirebaseCrashlytics
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import kotlin.math.max
import kotlin.math.min

class OfflineMangaFragment: Fragment() {
    private val downloadManager = Injekt.get<DownloadsManager>()
    private var downloads: List<OfflineMangaModel> = listOf()
    private lateinit var gridView: GridView
    private lateinit var adapter: OfflineMangaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_manga_offline, container, false)
        gridView = view.findViewById(R.id.gridView)
        getDownloads()
        adapter = OfflineMangaAdapter(requireContext(), downloads)
        gridView.adapter = adapter
        gridView.setOnItemClickListener { parent, view, position, id ->
            // Get the OfflineMangaModel that was clicked
            val item = adapter.getItem(position) as OfflineMangaModel
            val media = downloadManager.mangaDownloads.filter { it.title == item.title }.first()
            startActivity(
                Intent(requireContext(), MediaDetailsActivity::class.java)
                    .putExtra("media", getMedia(media))
            )
        }

        return view
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
        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.mangaRefresh)
        refreshLayout.setSlingshotDistance(height + 128)
        refreshLayout.setProgressViewEndTarget(false, height + 128)
        refreshLayout.setOnRefreshListener {
            Refresh.activity[this.hashCode()]!!.postValue(true)
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val titles = downloadManager.mangaDownloads.map { it.title }.distinct()
        val newDownloads = mutableListOf<OfflineMangaModel>()
        for (title in titles) {
            val _downloads = downloadManager.mangaDownloads.filter { it.title == title }
            val download = _downloads.first()
            val offlineMangaModel = loadOfflineMangaModel(download)
            newDownloads += offlineMangaModel
        }
        downloads = newDownloads
    }

    private fun getMedia(download: Download): Media? {
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/Manga/${download.title}"
        )
        //load media.json and convert to media class with gson
        try {
            val gson = GsonBuilder()
                .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                    SChapterImpl() // Provide an instance of SChapterImpl
                })
                .create()
            val media = File(directory, "media.json")
            val mediaJson = media.readText()
            return gson.fromJson(mediaJson, Media::class.java)
        }
        catch (e: Exception){
            logger("Error loading media.json: ${e.message}")
            logger(e.printStackTrace())
            FirebaseCrashlytics.getInstance().recordException(e)
            return null
        }
    }

    private fun loadOfflineMangaModel(download: Download): OfflineMangaModel{
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/Manga/${download.title}"
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
            val title = mediaModel.nameMAL?:"unknown"
            val score = if (mediaModel.userScore != 0) mediaModel.userScore.toString() else
                if (mediaModel.meanScore == null) "?" else mediaModel.meanScore.toString()
            val isOngoing = false
            val isUserScored = mediaModel.userScore != 0
            return OfflineMangaModel(title, score, isOngoing, isUserScored, coverUri)
        }
        catch (e: Exception){
            logger("Error loading media.json: ${e.message}")
            logger(e.printStackTrace())
            FirebaseCrashlytics.getInstance().recordException(e)
            return OfflineMangaModel("unknown", "0", false, false, null)
        }
    }
}