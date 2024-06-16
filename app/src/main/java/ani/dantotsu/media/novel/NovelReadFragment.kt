package ani.dantotsu.media.novel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.currContext
import ani.dantotsu.databinding.FragmentMediaSourceBinding
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.novel.NovelDownloaderService
import ani.dantotsu.download.novel.NovelServiceDataSingleton
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.novel.novelreader.NovelReaderActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import ani.dantotsu.util.StoragePermissions
import ani.dantotsu.util.StoragePermissions.Companion.accessAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NovelReadFragment : Fragment(),
    DownloadTriggerCallback,
    DownloadedCheckCallback {

    private var _binding: FragmentMediaSourceBinding? = null
    private val binding get() = _binding!!
    private val model: MediaDetailsViewModel by activityViewModels()

    private lateinit var media: Media
    var source = 0
    lateinit var novelName: String

    private lateinit var headerAdapter: NovelReadAdapter
    private lateinit var novelResponseAdapter: NovelResponseAdapter
    private var progress = View.VISIBLE

    private var continueEp: Boolean = false
    var loaded = false

    override fun downloadTrigger(novelDownloadPackage: NovelDownloadPackage) {
        Logger.log("novel link: ${novelDownloadPackage.link}")
        activity?.let {
            fun continueDownload() {
                val downloadTask = NovelDownloaderService.DownloadTask(
                    title = media.mainName(),
                    chapter = novelDownloadPackage.novelName,
                    downloadLink = novelDownloadPackage.link,
                    originalLink = novelDownloadPackage.originalLink,
                    sourceMedia = media,
                    coverUrl = novelDownloadPackage.coverUrl,
                    retries = 2,
                )
                NovelServiceDataSingleton.downloadQueue.offer(downloadTask)
                CoroutineScope(Dispatchers.IO).launch {

                    if (!NovelServiceDataSingleton.isServiceRunning) {
                        val intent = Intent(context, NovelDownloaderService::class.java)
                        withContext(Dispatchers.Main) {
                            ContextCompat.startForegroundService(requireContext(), intent)
                        }
                        NovelServiceDataSingleton.isServiceRunning = true
                    }
                }
            }
            if (!StoragePermissions.hasDirAccess(it)) {
                (it as MediaDetailsActivity).accessAlertDialog(it.launcher) { success ->
                    if (success) {
                        continueDownload()
                    } else {
                        snackString(getString(R.string.download_permission_required))
                    }
                }
            } else {
                continueDownload()
            }
        }
    }

    override fun downloadedCheckWithStart(novel: ShowResponse): Boolean {
        val downloadsManager = Injekt.get<DownloadsManager>()
        if (downloadsManager.queryDownload(
                DownloadedType(
                    media.mainName(),
                    novel.name,
                    MediaType.NOVEL
                )
            )
        ) {
            try {
                val directory =
                    DownloadsManager.getSubDirectory(
                        context ?: currContext()!!,
                        MediaType.NOVEL,
                        false,
                        media.mainName(),
                        novel.name
                    )
                val file = directory?.findFile("0.epub")
                if (file?.exists() == false) return false
                val fileUri = file?.uri ?: return false
                val intent = Intent(context, NovelReaderActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(fileUri, "application/epub+zip")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(intent)
                return true
            } catch (e: Exception) {
                Logger.log(e)
                return false
            }
        } else {
            return false
        }
    }

    override fun downloadedCheck(novel: ShowResponse): Boolean {
        val downloadsManager = Injekt.get<DownloadsManager>()
        return downloadsManager.queryDownload(
            DownloadedType(
                media.mainName(),
                novel.name,
                MediaType.NOVEL
            )
        )
    }

    override fun deleteDownload(novel: ShowResponse) {
        val downloadsManager = Injekt.get<DownloadsManager>()
        downloadsManager.removeDownload(
            DownloadedType(
                media.mainName(),
                novel.name,
                MediaType.NOVEL
            )
        ) {}
    }

    private val downloadStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!this@NovelReadFragment::novelResponseAdapter.isInitialized) return
            when (intent.action) {
                ACTION_DOWNLOAD_STARTED -> {
                    val link = intent.getStringExtra(EXTRA_NOVEL_LINK)
                    link?.let {
                        novelResponseAdapter.startDownload(it)
                    }
                }

                ACTION_DOWNLOAD_FINISHED -> {
                    val link = intent.getStringExtra(EXTRA_NOVEL_LINK)
                    link?.let {
                        novelResponseAdapter.stopDownload(it)
                    }
                }

                ACTION_DOWNLOAD_FAILED -> {
                    val link = intent.getStringExtra(EXTRA_NOVEL_LINK)
                    link?.let {
                        novelResponseAdapter.purgeDownload(it)
                    }
                }

                ACTION_DOWNLOAD_PROGRESS -> {
                    val link = intent.getStringExtra(EXTRA_NOVEL_LINK)
                    val progress = intent.getIntExtra("progress", 0)
                    link?.let {
                        novelResponseAdapter.updateDownloadProgress(it, progress)
                    }
                }
            }
        }
    }

    var response: List<ShowResponse>? = null
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

        binding.mediaSourceRecycler.updatePadding(bottom = binding.mediaSourceRecycler.paddingBottom + navBarHeight)

        binding.mediaSourceRecycler.layoutManager = LinearLayoutManager(requireContext())
        model.scrolledToTop.observe(viewLifecycleOwner) {
            if (it) binding.mediaSourceRecycler.scrollToPosition(0)
        }

        continueEp = model.continueMedia ?: false
        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it
                novelName = media.userPreferredName
                progress = View.GONE
                binding.mediaInfoProgressBar.visibility = progress
                if (!loaded) {
                    val sel = media.selected
                    searchQuery = sel?.server ?: media.name ?: media.nameRomaji
                    headerAdapter = NovelReadAdapter(media, this, model.novelSources)
                    novelResponseAdapter = NovelResponseAdapter(
                        this,
                        this,
                        this
                    )  // probably a better way to do this but it works
                    binding.mediaSourceRecycler.adapter =
                        ConcatAdapter(headerAdapter, novelResponseAdapter)
                    loaded = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        search(searchQuery, sel?.sourceIndex ?: 0, auto = sel?.server == null)
                    }, 100)
                }
            }
        }
        model.novelResponses.observe(viewLifecycleOwner) {
            if (it != null) {
                response = it
                searching = false
                novelResponseAdapter.submitList(it)
                headerAdapter.progress?.visibility = View.GONE
            }
        }
    }

    lateinit var searchQuery: String
    private var searching = false
    fun search(query: String, source: Int, save: Boolean = false, auto: Boolean = false) {
        if (!searching) {
            novelResponseAdapter.clear()
            searchQuery = query
            headerAdapter.progress?.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                if (auto || query == "") model.autoSearchNovels(media)
                else model.searchNovels(query, source)
            }
            searching = true
            if (save) {
                val selected = model.loadSelected(media)
                selected.server = query
                model.saveSelected(media.id, selected)
            }
        }
    }

    fun onSourceChange(i: Int) {
        val selected = model.loadSelected(media)
        selected.sourceIndex = i
        source = i
        selected.server = null
        model.saveSelected(media.id, selected)
        media.selected = selected
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMediaSourceBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onDestroy() {
        model.mangaReadSources?.flushText()
        requireContext().unregisterReceiver(downloadStatusReceiver)
        super.onDestroy()
    }

    private var state: Parcelable? = null
    override fun onResume() {
        super.onResume()
        binding.mediaInfoProgressBar.visibility = progress
        binding.mediaSourceRecycler.layoutManager?.onRestoreInstanceState(state)
    }

    override fun onPause() {
        super.onPause()
        state = binding.mediaSourceRecycler.layoutManager?.onSaveInstanceState()
    }

    companion object {
        const val ACTION_DOWNLOAD_STARTED = "ani.dantotsu.ACTION_DOWNLOAD_STARTED"
        const val ACTION_DOWNLOAD_FINISHED = "ani.dantotsu.ACTION_DOWNLOAD_FINISHED"
        const val ACTION_DOWNLOAD_FAILED = "ani.dantotsu.ACTION_DOWNLOAD_FAILED"
        const val ACTION_DOWNLOAD_PROGRESS = "ani.dantotsu.ACTION_DOWNLOAD_PROGRESS"
        const val EXTRA_NOVEL_LINK = "extra_novel_link"
    }
}

interface DownloadTriggerCallback {
    fun downloadTrigger(novelDownloadPackage: NovelDownloadPackage)
}

interface DownloadedCheckCallback {
    fun downloadedCheck(novel: ShowResponse): Boolean
    fun downloadedCheckWithStart(novel: ShowResponse): Boolean
    fun deleteDownload(novel: ShowResponse)
}