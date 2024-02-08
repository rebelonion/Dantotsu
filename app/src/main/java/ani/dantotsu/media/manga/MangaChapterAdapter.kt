package ani.dantotsu.media.manga

import android.app.AlertDialog
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.NumberPicker
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
            arr[position].progress = ""
            notifyItemChanged(position)
        }
    }

    fun purgeDownload(chapterNumber: String) {
        activeDownloads.remove(chapterNumber)
        downloadedChapters.remove(chapterNumber)
        // Find the position of the chapter and notify only that item
        val position = arr.indexOfFirst { it.number == chapterNumber }
        if (position != -1) {
            arr[position].progress = ""
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

    fun downloadNChaptersFrom(position: Int, n: Int) {
        //download next n chapters
        if (position < 0 || position >= arr.size) return
        for (i in 0..<n) {
            if (position + i < arr.size) {
                val chapterNumber = arr[position + i].number
                if (activeDownloads.contains(chapterNumber)) {
                    //do nothing
                    continue
                } else if (downloadedChapters.contains(chapterNumber)) {
                    //do nothing
                    continue
                } else {
                    fragment.onMangaChapterDownloadClick(chapterNumber)
                    startDownload(chapterNumber)
                }
            }
        }
    }

    inner class ChapterListViewHolder(val binding: ItemChapterListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val activeCoroutines = mutableSetOf<String>()
        private val typedValue1 = TypedValue()
        private val typedValue2 = TypedValue()
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
                startOrContinueRotation(chapterNumber) {
                    binding.itemDownload.rotation = 0f
                }
            } else if (downloadedChapters.contains(chapterNumber)) {
                // Show checkmark
                binding.itemDownload.setImageResource(R.drawable.ic_circle_check)
                binding.itemDownload.postDelayed({
                    binding.itemDownload.setImageResource(R.drawable.ic_round_delete_24)
                    binding.itemDownload.rotation = 0f
                }, 1000)
            } else {
                // Show download icon
                binding.itemDownload.setImageResource(R.drawable.ic_download_24)
                binding.itemDownload.rotation = 0f
            }

        }

        private fun startOrContinueRotation(chapterNumber: String, resetRotation: () -> Unit) {
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
                    resetRotation()
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
                        val builder = AlertDialog.Builder(currContext(), R.style.MyPopup)
                        builder.setTitle("Delete Chapter")
                        builder.setMessage("Are you sure you want to delete ${chapterNumber}?")
                        builder.setPositiveButton("Yes") { _, _ ->
                            fragment.onMangaChapterRemoveDownloadClick(chapterNumber)
                        }
                        builder.setNegativeButton("No") { _, _ ->
                        }
                        val dialog = builder.show()
                        dialog.window?.setDimAmount(0.8f)
                        return@setOnClickListener
                    } else {
                        fragment.onMangaChapterDownloadClick(chapterNumber)
                        startDownload(chapterNumber)
                    }
                }
            }
            binding.itemDownload.setOnLongClickListener {
                //Alert dialog asking for the number of chapters to download
                val alertDialog = AlertDialog.Builder(currContext(), R.style.MyPopup)
                alertDialog.setTitle("Multi Chapter Downloader")
                alertDialog.setMessage("Enter the number of chapters to download")
                val input = NumberPicker(currContext())
                input.minValue = 1
                input.maxValue = itemCount - bindingAdapterPosition
                input.value = 1
                alertDialog.setView(input)
                alertDialog.setPositiveButton("OK") { _, _ ->
                    downloadNChaptersFrom(bindingAdapterPosition, input.value)
                }
                alertDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                val dialog = alertDialog.show()
                dialog.window?.setDimAmount(0.8f)
                true
            }

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChapterCompactViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root)
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
                setAnimation(fragment.requireContext(), holder.binding.root)
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