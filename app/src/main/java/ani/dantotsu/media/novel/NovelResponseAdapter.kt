package ani.dantotsu.media.novel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemNovelResponseBinding
import ani.dantotsu.getThemeColor
import ani.dantotsu.loadImage
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.setAnimation
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import ani.dantotsu.util.customAlertDialog

class NovelResponseAdapter(
    val fragment: NovelReadFragment,
    val downloadTriggerCallback: DownloadTriggerCallback,
    val downloadedCheckCallback: DownloadedCheckCallback
) : RecyclerView.Adapter<NovelResponseAdapter.ViewHolder>() {
    val list: MutableList<ShowResponse> = mutableListOf()

    inner class ViewHolder(val binding: ItemNovelResponseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind =
            ItemNovelResponseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(bind)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val novel = list[position]
        setAnimation(fragment.requireContext(), holder.binding.root)
        binding.itemMediaImage.loadImage(novel.coverUrl, 400, 0)

        val color = fragment.requireContext()
            .getThemeColor(com.google.android.material.R.attr.colorOnBackground)
        binding.itemEpisodeTitle.text = novel.name
        binding.itemEpisodeFiller.text =
            if (downloadedCheckCallback.downloadedCheck(novel)) {
                "Downloaded"
            } else {
                novel.extra?.get("0") ?: ""
            }
        if (binding.itemEpisodeFiller.text.contains("Downloading")) {
            binding.itemEpisodeFiller.setTextColor(
                ContextCompat.getColor(fragment.requireContext(), android.R.color.holo_blue_light)
            )
        } else if (binding.itemEpisodeFiller.text.contains("Downloaded")) {
            binding.itemEpisodeFiller.setTextColor(
                ContextCompat.getColor(fragment.requireContext(), android.R.color.holo_green_light)
            )
        } else {
            binding.itemEpisodeFiller.setTextColor(color)
        }
        binding.itemEpisodeDesc2.text = novel.extra?.get("1") ?: ""
        val desc = novel.extra?.get("2")
        binding.itemEpisodeDesc.isVisible = !desc.isNullOrBlank()
        binding.itemEpisodeDesc.text = desc ?: ""

        binding.root.setOnClickListener {
            //make sure the file is not downloading
            if (activeDownloads.contains(novel.link)) {
                return@setOnClickListener
            }
            if (downloadedCheckCallback.downloadedCheckWithStart(novel)) {
                return@setOnClickListener
            }

            val bookDialog = BookDialog.newInstance(fragment.novelName, novel, fragment.source)

            bookDialog.setCallback(object : BookDialog.Callback {
                override fun onDownloadTriggered(link: String) {
                    downloadTriggerCallback.downloadTrigger(
                        NovelDownloadPackage(
                            link,
                            novel.coverUrl.url,
                            novel.name,
                            novel.link
                        )
                    )
                    bookDialog.dismiss()
                }
            })
            bookDialog.show(fragment.parentFragmentManager, "dialog")

        }

        binding.root.setOnLongClickListener {
            it.context.customAlertDialog().apply {
                setTitle("Delete ${novel.name}?")
                setMessage("Are you sure you want to delete ${novel.name}?")
                setPosButton(R.string.yes) {
                    downloadedCheckCallback.deleteDownload(novel)
                    deleteDownload(novel.link)
                    snackString("Deleted ${novel.name}")
                    if (binding.itemEpisodeFiller.text.toString()
                            .contains("Download", ignoreCase = true)
                    ) {
                        binding.itemEpisodeFiller.text = ""
                    }
                }
                setNegButton(R.string.no)
                show()
            }
            true
        }
    }

    private val activeDownloads = mutableSetOf<String>()
    private val downloadedChapters = mutableSetOf<String>()

    fun startDownload(link: String) {
        activeDownloads.add(link)
        val position = list.indexOfFirst { it.link == link }
        if (position != -1) {
            list[position].extra?.remove("0")
            list[position].extra?.set("0", "Downloading: 0%")
            notifyItemChanged(position)
        }

    }

    fun stopDownload(link: String) {
        activeDownloads.remove(link)
        downloadedChapters.add(link)
        val position = list.indexOfFirst { it.link == link }
        if (position != -1) {
            list[position].extra?.remove("0")
            list[position].extra?.set("0", "Downloaded")
            notifyItemChanged(position)
        }
    }

    fun deleteDownload(link: String) { //TODO:
        downloadedChapters.remove(link)
        val position = list.indexOfFirst { it.link == link }
        if (position != -1) {
            list[position].extra?.remove("0")
            list[position].extra?.set("0", "")
            notifyItemChanged(position)
        }
    }

    fun purgeDownload(link: String) {
        activeDownloads.remove(link)
        downloadedChapters.remove(link)
        val position = list.indexOfFirst { it.link == link }
        if (position != -1) {
            list[position].extra?.remove("0")
            list[position].extra?.set("0", "Failed")
            notifyItemChanged(position)
        }
    }

    fun updateDownloadProgress(link: String, progress: Int) {
        if (!activeDownloads.contains(link)) {
            activeDownloads.add(link)
        }
        val position = list.indexOfFirst { it.link == link }
        if (position != -1) {
            list[position].extra?.remove("0")
            list[position].extra?.set("0", "Downloading: $progress%")
            Logger.log("updateDownloadProgress: $progress, position: $position")
            notifyItemChanged(position)
        }
    }

    fun submitList(it: List<ShowResponse>) {
        val old = list.size
        list.addAll(it)
        notifyItemRangeInserted(old, it.size)
    }

    fun clear() {
        val size = list.size
        list.clear()
        notifyItemRangeRemoved(0, size)
    }
}

data class NovelDownloadPackage(
    val link: String,
    val coverUrl: String,
    val novelName: String,
    val originalLink: String
)