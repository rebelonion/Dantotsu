package ani.dantotsu.media.anime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.lifecycle.coroutineScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.connections.updateProgress
import ani.dantotsu.databinding.ItemEpisodeCompactBinding
import ani.dantotsu.databinding.ItemEpisodeGridBinding
import ani.dantotsu.databinding.ItemEpisodeListBinding
import ani.dantotsu.download.DownloadsManager.Companion.getDirSize
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.MediaType
import ani.dantotsu.setAnimation
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.util.customAlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.pow

fun handleProgress(cont: LinearLayout, bar: View, empty: View, mediaId: Int, ep: String) {
    val curr = PrefManager.getNullableCustomVal("${mediaId}_${ep}", null, Long::class.java)
    val max = PrefManager.getNullableCustomVal("${mediaId}_${ep}_max", null, Long::class.java)
    if (curr != null && max != null) {
        cont.visibility = View.VISIBLE
        val div = curr.toFloat() / max.toFloat()
        val barParams = bar.layoutParams as LinearLayout.LayoutParams
        barParams.weight = div
        bar.layoutParams = barParams
        val params = empty.layoutParams as LinearLayout.LayoutParams
        params.weight = 1 - div
        empty.layoutParams = params
    } else {
        cont.visibility = View.GONE
    }
}

@OptIn(UnstableApi::class)
class EpisodeAdapter(
    private var type: Int,
    private val media: Media,
    private val fragment: AnimeWatchFragment,
    var arr: List<Episode> = arrayListOf(),
    var offlineMode: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val context = fragment.requireContext()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return (when (viewType) {
            0 -> EpisodeListViewHolder(
                ItemEpisodeListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            1 -> EpisodeGridViewHolder(
                ItemEpisodeGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            2 -> EpisodeCompactViewHolder(
                ItemEpisodeCompactBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> throw IllegalArgumentException()
        })
    }

    override fun getItemViewType(position: Int): Int {
        return type
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val ep = arr[position]
        val title = if (!ep.title.isNullOrEmpty() && ep.title != "null") {
            ep.title?.let { MediaNameAdapter.removeEpisodeNumber(it) }
        } else {
            ep.number
        } ?: ""

        when (holder) {
            is EpisodeListViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root)

                val thumb =
                    ep.thumb?.let { if (it.url.isNotEmpty()) GlideUrl(it.url) { it.headers } else null }
                Glide.with(binding.itemMediaImage).load(thumb ?: media.cover).override(400, 0)
                    .into(binding.itemMediaImage)
                binding.itemEpisodeNumber.text = ep.number
                binding.itemEpisodeTitle.text = if (ep.number == title) "Episode $title" else title

                if (ep.filler) {
                    binding.itemEpisodeFiller.visibility = View.VISIBLE
                    binding.itemEpisodeFillerView.visibility = View.VISIBLE
                } else {
                    binding.itemEpisodeFiller.visibility = View.GONE
                    binding.itemEpisodeFillerView.visibility = View.GONE
                }
                binding.itemEpisodeDesc.isVisible = !ep.desc.isNullOrBlank()
                binding.itemEpisodeDesc.text = ep.desc ?: ""
                holder.bind(ep.number, ep.downloadProgress, ep.desc)

                if (media.userProgress != null) {
                    if ((ep.number.toFloatOrNull() ?: 9999f) <= media.userProgress!!.toFloat()) {
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                        binding.itemEpisodeViewed.visibility = View.VISIBLE
                    } else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeViewed.visibility = View.GONE
                        binding.itemEpisodeCont.setOnLongClickListener {
                            updateProgress(media, ep.number)
                            true
                        }
                    }
                } else {
                    binding.itemEpisodeViewedCover.visibility = View.GONE
                    binding.itemEpisodeViewed.visibility = View.GONE
                }

                handleProgress(
                    binding.itemMediaProgressCont,
                    binding.itemMediaProgress,
                    binding.itemMediaProgressEmpty,
                    media.id,
                    ep.number
                )
            }

            is EpisodeGridViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root)

                val thumb =
                    ep.thumb?.let { if (it.url.isNotEmpty()) GlideUrl(it.url) { it.headers } else null }
                Glide.with(binding.itemMediaImage).load(thumb ?: media.cover).override(400, 0)
                    .into(binding.itemMediaImage)

                binding.itemEpisodeNumber.text = ep.number
                binding.itemEpisodeTitle.text = title
                if (ep.filler) {
                    binding.itemEpisodeFiller.visibility = View.VISIBLE
                    binding.itemEpisodeFillerView.visibility = View.VISIBLE
                } else {
                    binding.itemEpisodeFiller.visibility = View.GONE
                    binding.itemEpisodeFillerView.visibility = View.GONE
                }
                if (media.userProgress != null) {
                    if ((ep.number.toFloatOrNull() ?: 9999f) <= media.userProgress!!.toFloat()) {
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                        binding.itemEpisodeViewed.visibility = View.VISIBLE
                    } else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeViewed.visibility = View.GONE
                        binding.itemEpisodeCont.setOnLongClickListener {
                            updateProgress(media, ep.number)
                            true
                        }
                    }
                } else {
                    binding.itemEpisodeViewedCover.visibility = View.GONE
                    binding.itemEpisodeViewed.visibility = View.GONE
                }
                handleProgress(
                    binding.itemMediaProgressCont,
                    binding.itemMediaProgress,
                    binding.itemMediaProgressEmpty,
                    media.id,
                    ep.number
                )
            }

            is EpisodeCompactViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root)
                binding.itemEpisodeNumber.text = ep.number
                binding.itemEpisodeFillerView.isVisible = ep.filler
                if (media.userProgress != null) {
                    if ((ep.number.toFloatOrNull() ?: 9999f) <= media.userProgress!!.toFloat())
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                    else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeCont.setOnLongClickListener {
                            updateProgress(media, ep.number)
                            true
                        }
                    }
                }
                handleProgress(
                    binding.itemMediaProgressCont,
                    binding.itemMediaProgress,
                    binding.itemMediaProgressEmpty,
                    media.id,
                    ep.number
                )
            }
        }
    }

    override fun getItemCount(): Int = arr.size

    private val activeDownloads = mutableSetOf<String>()
    private val downloadedEpisodes = mutableSetOf<String>()

    fun startDownload(episodeNumber: String) {
        activeDownloads.add(episodeNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == episodeNumber }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    @OptIn(UnstableApi::class)
    fun stopDownload(episodeNumber: String) {
        activeDownloads.remove(episodeNumber)
        downloadedEpisodes.add(episodeNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == episodeNumber }
        if (position != -1) {
            val size = try {
                bytesToHuman(getDirSize(context, MediaType.ANIME, media.mainName(), episodeNumber))
            } catch (e: Exception) {
                null
            }

            arr[position].downloadProgress = "Downloaded" + if (size != null) ": ($size)" else ""
            notifyItemChanged(position)
        }
    }

    fun deleteDownload(episodeNumber: String) {
        downloadedEpisodes.remove(episodeNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == episodeNumber }
        if (position != -1) {
            arr[position].downloadProgress = null
            notifyItemChanged(position)
        }
    }

    fun purgeDownload(episodeNumber: String) {
        activeDownloads.remove(episodeNumber)
        downloadedEpisodes.remove(episodeNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == episodeNumber }
        if (position != -1) {
            arr[position].downloadProgress = "Failed"
            notifyItemChanged(position)
        }
    }

    fun updateDownloadProgress(episodeNumber: String, progress: Int) {
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == episodeNumber }
        if (position != -1) {
            arr[position].downloadProgress = "Downloading: $progress%"

            notifyItemChanged(position)
        }
    }


    inner class EpisodeCompactViewHolder(val binding: ItemEpisodeCompactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition < arr.size && bindingAdapterPosition >= 0)
                    fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
        }
    }

    inner class EpisodeGridViewHolder(val binding: ItemEpisodeGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition < arr.size && bindingAdapterPosition >= 0)
                    fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
        }
    }

    inner class EpisodeListViewHolder(val binding: ItemEpisodeListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val activeCoroutines = mutableSetOf<String>()

        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition < arr.size && bindingAdapterPosition >= 0)
                    fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
            binding.itemDownload.setOnClickListener {
                if (0 <= bindingAdapterPosition && bindingAdapterPosition < arr.size) {
                    val episodeNumber = arr[bindingAdapterPosition].number
                    if (activeDownloads.contains(episodeNumber)) {
                        fragment.onAnimeEpisodeStopDownloadClick(episodeNumber)
                        return@setOnClickListener
                    } else if (downloadedEpisodes.contains(episodeNumber)) {
                        binding.root.context.customAlertDialog().apply {
                            setTitle("Delete Episode")
                            setMessage("Are you sure you want to delete Episode $episodeNumber?")
                            setPosButton(R.string.yes) {
                                fragment.onAnimeEpisodeRemoveDownloadClick(episodeNumber)
                            }
                            setNegButton(R.string.no)
                        }.show()
                        return@setOnClickListener
                    } else {
                        fragment.onAnimeEpisodeDownloadClick(episodeNumber)
                    }
                }
            }
            binding.itemDownload.setOnLongClickListener {
                if (0 <= bindingAdapterPosition && bindingAdapterPosition < arr.size) {
                    val episodeNumber = arr[bindingAdapterPosition].number
                    if (downloadedEpisodes.contains(episodeNumber)) {
                        fragment.fixDownload(episodeNumber)
                    }
                }

                true
            }
            binding.itemEpisodeDesc.setOnClickListener {
                if (binding.itemEpisodeDesc.maxLines == 3)
                    binding.itemEpisodeDesc.maxLines = 100
                else
                    binding.itemEpisodeDesc.maxLines = 3
            }
        }

        fun bind(episodeNumber: String, progress: String?, desc: String?) {
            if (progress != null) {
                binding.itemEpisodeDesc.visibility = View.GONE
                binding.itemDownloadStatus.visibility = View.VISIBLE
                binding.itemDownloadStatus.text = progress
            } else {
                binding.itemDownloadStatus.visibility = View.GONE
                binding.itemDownloadStatus.text = ""
            }
            if (activeDownloads.contains(episodeNumber)) {
                // Show spinner
                binding.itemDownload.setImageResource(R.drawable.ic_sync)
                startOrContinueRotation(episodeNumber) {
                    binding.itemDownload.rotation = 0f
                }
                binding.itemEpisodeDesc.visibility = View.GONE
            } else if (downloadedEpisodes.contains(episodeNumber)) {
                binding.itemEpisodeDesc.visibility = View.GONE
                binding.itemDownloadStatus.visibility = View.VISIBLE
                // Show checkmark
                binding.itemDownload.setImageResource(R.drawable.ic_circle_check)
                binding.itemDownload.postDelayed({
                    binding.itemDownload.setImageResource(R.drawable.ic_round_delete_24)
                    binding.itemDownload.rotation = 0f
                }, 1000)
            } else {
                binding.itemDownloadStatus.visibility = View.GONE
                binding.itemEpisodeDesc.visibility =
                    if (desc != null && desc.trim(' ') != "") View.VISIBLE else View.GONE
                // Show download icon
                binding.itemDownload.setImageResource(R.drawable.ic_download_24)
                binding.itemDownload.rotation = 0f
            }

        }

        private fun startOrContinueRotation(episodeNumber: String, resetRotation: () -> Unit) {
            if (!isRotationCoroutineRunningFor(episodeNumber)) {
                val scope = fragment.lifecycle.coroutineScope
                scope.launch {
                    // Add chapter number to active coroutines set
                    activeCoroutines.add(episodeNumber)
                    while (activeDownloads.contains(episodeNumber)) {
                        binding.itemDownload.animate().rotationBy(360f).setDuration(1000)
                            .setInterpolator(
                                LinearInterpolator()
                            ).start()
                        delay(1000)
                    }
                    // Remove chapter number from active coroutines set
                    activeCoroutines.remove(episodeNumber)
                    resetRotation()
                }
            }
        }

        private fun isRotationCoroutineRunningFor(episodeNumber: String): Boolean {
            return episodeNumber in activeCoroutines
        }
    }

    fun updateType(t: Int) {
        type = t
    }

    private fun bytesToHuman(bytes: Long): String? {
        if (bytes < 0) return null
        val unit = 1000
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = ("KMGTPE")[exp - 1]
        return String.format("%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }
}

