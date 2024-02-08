package ani.dantotsu.media.anime

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.*
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.databinding.BottomSheetSelectorBinding
import ani.dantotsu.databinding.ItemStreamBinding
import ani.dantotsu.databinding.ItemUrlBinding
import ani.dantotsu.download.video.Helper
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.others.Download.download
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.VideoExtractor
import ani.dantotsu.parsers.VideoType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DecimalFormat


class SelectorDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSelectorBinding? = null
    private val binding get() = _binding!!
    val model: MediaDetailsViewModel by activityViewModels()
    private var scope: CoroutineScope = lifecycleScope
    private var media: Media? = null
    private var episode: Episode? = null
    private var prevEpisode: String? = null
    private var makeDefault = false
    private var selected: String? = null
    private var launch: Boolean? = null
    private var isDownloadMenu: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selected = it.getString("server")
            launch = it.getBoolean("launch", true)
            prevEpisode = it.getString("prev")
            isDownloadMenu = it.getBoolean("isDownload")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectorBinding.inflate(inflater, container, false)
        val window = dialog?.window
        window?.statusBarColor = Color.TRANSPARENT
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window?.navigationBarColor = typedValue.data
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var loaded = false
        model.getMedia().observe(viewLifecycleOwner) { m ->
            media = m
            if (media != null && !loaded) {
                loaded = true
                val ep = media?.anime?.episodes?.get(media?.anime?.selectedEpisode)
                episode = ep
                if (ep != null) {
                    if (isDownloadMenu == true) {
                        binding.selectorMakeDefault.visibility = View.GONE
                    }

                    if (selected != null && isDownloadMenu == false) {
                        binding.selectorListContainer.visibility = View.GONE
                        binding.selectorAutoListContainer.visibility = View.VISIBLE
                        binding.selectorAutoText.text = selected
                        binding.selectorCancel.setOnClickListener {
                            media!!.selected!!.server = null
                            model.saveSelected(media!!.id, media!!.selected!!)
                            tryWith {
                                dismiss()
                            }
                        }
                        fun fail() {
                            snackString(getString(R.string.auto_select_server_error))
                            binding.selectorCancel.performClick()
                        }

                        fun load() {
                            val size =
                                if (model.watchSources!!.isDownloadedSource(media!!.selected!!.sourceIndex)) {
                                    ep.extractors?.firstOrNull()?.videos?.size
                                } else {
                                    ep.extractors?.find { it.server.name == selected }?.videos?.size
                                }

                            if (size != null && size >= media!!.selected!!.video) {
                                media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedExtractor =
                                    selected
                                media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedVideo =
                                    media!!.selected!!.video
                                startExoplayer(media!!)
                            } else fail()
                        }

                        if (ep.extractors.isNullOrEmpty()) {
                            model.getEpisode().observe(this) {
                                if (it != null) {
                                    episode = it
                                    load()
                                }
                            }
                            scope.launch {
                                if (withContext(Dispatchers.IO) {
                                        !model.loadEpisodeSingleVideo(
                                            ep,
                                            media!!.selected!!
                                        )
                                    }) fail()
                            }
                        } else load()
                    } else {
                        binding.selectorRecyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            bottomMargin = navBarHeight
                        }
                        binding.selectorRecyclerView.adapter = null
                        binding.selectorProgressBar.visibility = View.VISIBLE
                        makeDefault = PrefManager.getVal(PrefName.MakeDefault)
                        binding.selectorMakeDefault.isChecked = makeDefault
                        binding.selectorMakeDefault.setOnClickListener {
                            makeDefault = binding.selectorMakeDefault.isChecked
                            PrefManager.setVal(PrefName.MakeDefault, makeDefault)
                        }
                        binding.selectorRecyclerView.layoutManager =
                            LinearLayoutManager(
                                requireActivity(),
                                LinearLayoutManager.VERTICAL,
                                false
                            )
                        val adapter = ExtractorAdapter()
                        binding.selectorRecyclerView.adapter = adapter
                        if (!ep.allStreams) {
                            ep.extractorCallback = {
                                scope.launch {
                                    adapter.add(it)
                                    if (model.watchSources!!.isDownloadedSource(media?.selected!!.sourceIndex)) {
                                        adapter.performClick(0)
                                    }
                                }
                            }
                            model.getEpisode().observe(this) {
                                if (it != null) {
                                    media!!.anime?.episodes?.set(
                                        media!!.anime?.selectedEpisode!!,
                                        ep
                                    )
                                }
                            }
                            scope.launch(Dispatchers.IO) {
                                model.loadEpisodeVideos(ep, media!!.selected!!.sourceIndex)
                                withContext(Dispatchers.Main) {
                                    binding.selectorProgressBar.visibility = View.GONE
                                }
                            }
                        } else {
                            media!!.anime?.episodes?.set(media!!.anime?.selectedEpisode!!, ep)
                            adapter.addAll(ep.extractors)
                            if (model.watchSources!!.isDownloadedSource(media?.selected!!.sourceIndex)) {
                                adapter.performClick(0)
                            }
                            binding.selectorProgressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startExoplayer(media: Media) {
        prevEpisode = null

        dismiss()
        if (launch!! || model.watchSources!!.isDownloadedSource(media.selected!!.sourceIndex)) {
            stopAddingToList()
            val intent = Intent(activity, ExoplayerView::class.java)
            ExoplayerView.media = media
            ExoplayerView.initialized = true
            startActivity(intent)
        } else {
            model.setEpisode(
                media.anime!!.episodes!![media.anime.selectedEpisode!!]!!,
                "startExo no launch"
            )
        }
    }

    private fun stopAddingToList() {
        episode?.extractorCallback = null
        episode?.also {
            it.extractors = it.extractors?.toMutableList()
        }
    }

    private inner class ExtractorAdapter :
        RecyclerView.Adapter<ExtractorAdapter.StreamViewHolder>() {
        val links = mutableListOf<VideoExtractor>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder =
            StreamViewHolder(
                ItemStreamBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
            val extractor = links[position]
            holder.binding.streamName.text = ""//extractor.server.name
            holder.binding.streamName.visibility = View.GONE

            holder.binding.streamRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            holder.binding.streamRecyclerView.adapter = VideoAdapter(extractor)

        }

        override fun getItemCount(): Int = links.size

        fun add(videoExtractor: VideoExtractor) {
            if (videoExtractor.videos.isNotEmpty()) {
                links.add(videoExtractor)
                notifyItemInserted(links.size - 1)
            }
        }

        fun addAll(extractors: List<VideoExtractor>?) {
            links.addAll(extractors ?: return)
            notifyItemRangeInserted(0, extractors.size)
        }

        fun performClick(position: Int) {
            try { //bandaid fix for crash
                val extractor = links[position]
                media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedExtractor =
                    extractor.server.name
                media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedVideo = 0
                startExoplayer(media!!)
            } catch (e: Exception) {
                Injekt.get<CrashlyticsInterface>().logException(e)
            }
        }

        private inner class StreamViewHolder(val binding: ItemStreamBinding) :
            RecyclerView.ViewHolder(binding.root)
    }

    private inner class VideoAdapter(private val extractor: VideoExtractor) :
        RecyclerView.Adapter<VideoAdapter.UrlViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrlViewHolder {
            return UrlViewHolder(
                ItemUrlBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: UrlViewHolder, position: Int) {
            val binding = holder.binding
            val video = extractor.videos[position]
            if (isDownloadMenu == true) {
                binding.urlDownload.visibility = View.VISIBLE
            } else {
                binding.urlDownload.visibility = View.GONE
            }
            binding.urlDownload.setSafeOnClickListener {
                media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedExtractor =
                    extractor.server.name
                media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedVideo =
                    position
                if ((PrefManager.getVal(PrefName.DownloadManager) as Int) != 0) {
                    download(
                        requireActivity(),
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!,
                        media!!.userPreferredName
                    )
                } else {
                    val episode = media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!
                    val selectedVideo =
                        if (extractor.videos.size > episode.selectedVideo) extractor.videos[episode.selectedVideo] else null
                    val subtitles = extractor.subtitles
                    val subtitleNames = subtitles.map { it.language }
                    var subtitleToDownload: Subtitle? = null
                    val activity = currActivity()?:requireActivity()
                    if (subtitles.isNotEmpty()) {
                        val alertDialog = AlertDialog.Builder(context, R.style.MyPopup)
                            .setTitle("Download Subtitle")
                            .setSingleChoiceItems(
                                subtitleNames.toTypedArray(),
                                -1
                            ) { dialog, which ->
                                subtitleToDownload = subtitles[which]
                            }
                            .setPositiveButton("Download") { _, _ ->
                                dialog?.dismiss()
                                if (selectedVideo != null) {
                                    Helper.startAnimeDownloadService(
                                        activity,
                                        media!!.mainName(),
                                        episode.number,
                                        selectedVideo,
                                        subtitleToDownload,
                                        media,
                                        episode.thumb?.url ?: media!!.banner ?: media!!.cover
                                    )
                                    broadcastDownloadStarted(episode.number, activity)
                                } else {
                                    snackString("No Video Selected")
                                }
                            }
                            .setNegativeButton("Skip") { dialog, _ ->
                                subtitleToDownload = null
                                if (selectedVideo != null) {
                                    Helper.startAnimeDownloadService(
                                        currActivity()!!,
                                        media!!.mainName(),
                                        episode.number,
                                        selectedVideo,
                                        subtitleToDownload,
                                        media,
                                        episode.thumb?.url ?: media!!.banner ?: media!!.cover
                                    )
                                    broadcastDownloadStarted(episode.number, activity)
                                } else {
                                    snackString("No Video Selected")
                                }
                                dialog.dismiss()
                            }
                            .setNeutralButton("Cancel") { dialog, _ ->
                                subtitleToDownload = null
                                dialog.dismiss()
                            }
                            .show()
                        alertDialog.window?.setDimAmount(0.8f)

                    } else {
                        if (selectedVideo != null) {
                            Helper.startAnimeDownloadService(
                                requireActivity(),
                                media!!.mainName(),
                                episode.number,
                                selectedVideo,
                                subtitleToDownload,
                                media,
                                episode.thumb?.url ?: media!!.banner ?: media!!.cover
                            )
                            broadcastDownloadStarted(episode.number, activity)
                        } else {
                            snackString("No Video Selected")
                        }
                    }
                }
                dismiss()
            }
            if (video.format == VideoType.CONTAINER) {
                binding.urlSize.visibility = if (video.size != null) View.VISIBLE else View.GONE
                binding.urlSize.text =
                        // if video size is null or 0, show "Unknown Size" else show the size in MB
                    (if (video.extraNote != null) " : " else "") + (if (video.size == 0.0) "Unknown Size" else (DecimalFormat(
                        "#.##"
                    ).format(video.size ?: 0).toString() + " MB"))
            }
            binding.urlNote.visibility = View.VISIBLE
            binding.urlNote.text = video.format.name
            binding.urlQuality.text = extractor.server.name
        }

        private fun broadcastDownloadStarted(episodeNumber: String, activity: Activity) {
            val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_STARTED).apply {
                putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, episodeNumber)
            }
            activity.sendBroadcast(intent)
        }

        override fun getItemCount(): Int = extractor.videos.size

        private inner class UrlViewHolder(val binding: ItemUrlBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setSafeOnClickListener {
                    if (isDownloadMenu == true) {
                        binding.urlDownload.performClick()
                        return@setSafeOnClickListener
                    }
                    tryWith(true) {
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedExtractor =
                            extractor.server.name
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedVideo =
                            bindingAdapterPosition
                        if (makeDefault) {
                            media!!.selected!!.server = extractor.server.name
                            media!!.selected!!.video = bindingAdapterPosition
                            model.saveSelected(media!!.id, media!!.selected!!)
                        }
                        startExoplayer(media!!)
                    }
                }
                itemView.setOnLongClickListener {
                    val video = extractor.videos[bindingAdapterPosition]
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(video.file.url), "video/*")
                    }
                    copyToClipboard(video.file.url, true)
                    dismiss()
                    startActivity(Intent.createChooser(intent, "Open Video in :"))
                    true
                }
            }
        }
    }

    companion object {
        fun newInstance(
            server: String? = null,
            la: Boolean = true,
            prev: String? = null,
            isDownload: Boolean
        ): SelectorDialogFragment =
            SelectorDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("server", server)
                    putBoolean("launch", la)
                    putString("prev", prev)
                    putBoolean("isDownload", isDownload)
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {}

    override fun onDismiss(dialog: DialogInterface) {
        if (launch == false) {
            activity?.hideSystemBars()
            model.epChanged.postValue(true)
            if (prevEpisode != null) {
                media?.anime?.selectedEpisode = prevEpisode
                model.setEpisode(media?.anime?.episodes?.get(prevEpisode) ?: return, "prevEp")
            }
        }
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
