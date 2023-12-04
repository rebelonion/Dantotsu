package ani.dantotsu.media.manga

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.connections.updateProgress
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ItemChapterListBinding
import ani.dantotsu.databinding.ItemEpisodeCompactBinding
import ani.dantotsu.media.Media
import ani.dantotsu.setAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MangaChapterAdapter(
    private var type: Int,
    private val media: Media,
    private val fragment: MangaReadFragment,
    var arr: ArrayList<MangaChapter> = arrayListOf(),
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> ChapterCompactViewHolder(
                ItemEpisodeCompactBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            0 -> ChapterListViewHolder(
                ItemChapterListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return type
    }

    override fun getItemCount(): Int = arr.size

    inner class ChapterCompactViewHolder(val binding: ItemEpisodeCompactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (0 <= bindingAdapterPosition && bindingAdapterPosition < arr.size)
                    fragment.onMangaChapterClick(arr[bindingAdapterPosition].number)
            }
        }
    }

    private val activeDownloads = mutableSetOf<String>()
    private val downloadedChapters = mutableSetOf<String>()

    fun startDownload(chapterNumber: String) {
        activeDownloads.add(chapterNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == chapterNumber }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun stopDownload(chapterNumber: String) {
        activeDownloads.remove(chapterNumber)
        downloadedChapters.add(chapterNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == chapterNumber }
        if (position != -1) {
            arr[position].progress = "Downloaded"
            notifyItemChanged(position)
        }
    }

    fun deleteDownload(chapterNumber: String) {
        downloadedChapters.remove(chapterNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == chapterNumber }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun purgeDownload(chapterNumber: String) {
        activeDownloads.remove(chapterNumber)
        downloadedChapters.remove(chapterNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == chapterNumber }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun updateDownloadProgress(chapterNumber: String, progress: Int) {
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == chapterNumber }
        if (position != -1) {
            arr[position].progress = "Downloading: ${progress}%"

            notifyItemChanged(position)
        }
    }

    inner class ChapterListViewHolder(val binding: ItemChapterListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val activeCoroutines = mutableSetOf<String>()
        val typedValue1 = TypedValue()
        val typedValue2 = TypedValue()
        fun bind(chapterNumber: String, progress: String?) {
            if (progress != null) {
                binding.itemChapterTitle.visibility = View.VISIBLE
                binding.itemChapterTitle.text = "$progress"
            } else {
                binding.itemChapterTitle.visibility = View.GONE
                binding.itemChapterTitle.text = ""
            }
            if (activeDownloads.contains(chapterNumber)) {
                // Show spinner
                binding.itemDownload.setImageResource(R.drawable.ic_sync)
                startOrContinueRotation(chapterNumber)
            } else if (downloadedChapters.contains(chapterNumber)) {
                // Show checkmark
                binding.itemDownload.setImageResource(R.drawable.ic_circle_check)
                binding.itemDownload.setColorFilter(typedValue2.data)
                binding.itemDownload.postDelayed({
                    binding.itemDownload.setImageResource(R.drawable.ic_circle_cancel)
                    binding.itemDownload.setColorFilter(typedValue2.data)
                }, 1000)
            } else {
                // Show download icon
                binding.itemDownload.setImageResource(R.drawable.ic_circle_add)
            }

        }

        private fun startOrContinueRotation(chapterNumber: String) {
            if (!isRotationCoroutineRunningFor(chapterNumber)) {
                val scope = fragment.lifecycle.coroutineScope
                scope.launch {
                    // Add chapter number to active coroutines set
                    activeCoroutines.add(chapterNumber)
                    while (activeDownloads.contains(chapterNumber)) {
                        binding.itemDownload.animate().rotationBy(360f).setDuration(1000)
                            .setInterpolator(
                                LinearInterpolator()
                            ).start()
                        delay(1000)
                    }
                    // Remove chapter number from active coroutines set
                    activeCoroutines.remove(chapterNumber)
                }
            }
        }

        private fun isRotationCoroutineRunningFor(chapterNumber: String): Boolean {
            return chapterNumber in activeCoroutines
        }

        init {
            val theme = currContext()?.theme
            theme?.resolveAttribute(
                com.google.android.material.R.attr.colorError,
                typedValue1,
                true
            )
            theme?.resolveAttribute(
                com.google.android.material.R.attr.colorPrimary,
                typedValue2,
                true
            )
            itemView.setOnClickListener {
                if (0 <= bindingAdapterPosition && bindingAdapterPosition < arr.size)
                    fragment.onMangaChapterClick(arr[bindingAdapterPosition].number)
            }
            binding.itemDownload.setOnClickListener {
                if (0 <= bindingAdapterPosition && bindingAdapterPosition < arr.size) {
                    val chapterNumber = arr[bindingAdapterPosition].number
                    if (activeDownloads.contains(chapterNumber)) {
                        fragment.onMangaChapterStopDownloadClick(chapterNumber)
                        return@setOnClickListener
                    } else if (downloadedChapters.contains(chapterNumber)) {
                        fragment.onMangaChapterRemoveDownloadClick(chapterNumber)
                        return@setOnClickListener
                    } else {
                        fragment.onMangaChapterDownloadClick(chapterNumber)
                        startDownload(chapterNumber)
                    }
                }
            }

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChapterCompactViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root, fragment.uiSettings)
                val ep = arr[position]
                val parsedNumber = MangaNameAdapter.findChapterNumber(ep.number)?.toInt()
                binding.itemEpisodeNumber.text = parsedNumber?.toString() ?: ep.number
                if (media.userProgress != null) {
                    if ((MangaNameAdapter.findChapterNumber(ep.number)
                            ?: 9999f) <= media.userProgress!!.toFloat()
                    )
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                    else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeCont.setOnLongClickListener {
                            updateProgress(
                                media,
                                MangaNameAdapter.findChapterNumber(ep.number).toString()
                            )
                            true
                        }
                    }
                }
            }

            is ChapterListViewHolder -> {
                val binding = holder.binding
                val ep = arr[position]
                holder.bind(ep.number, ep.progress)
                setAnimation(fragment.requireContext(), holder.binding.root, fragment.uiSettings)
                binding.itemChapterNumber.text = ep.number
                if (ep.progress.isNullOrEmpty()) {
                    binding.itemChapterTitle.visibility = View.GONE
                } else binding.itemChapterTitle.visibility = View.VISIBLE

                if (media.userProgress != null) {
                    if ((MangaNameAdapter.findChapterNumber(ep.number)
                            ?: 9999f) <= media.userProgress!!.toFloat()
                    ) {
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                        binding.itemEpisodeViewed.visibility = View.VISIBLE
                    } else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeViewed.visibility = View.GONE
                        binding.root.setOnLongClickListener {
                            updateProgress(
                                media,
                                MangaNameAdapter.findChapterNumber(ep.number).toString()
                            )
                            true
                        }
                    }
                } else {
                    binding.itemEpisodeViewedCover.visibility = View.GONE
                    binding.itemEpisodeViewed.visibility = View.GONE
                }
            }
        }
    }

    fun updateType(t: Int) {
        type = t
    }


}