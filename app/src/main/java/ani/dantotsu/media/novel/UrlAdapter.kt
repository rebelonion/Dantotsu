package ani.dantotsu.media.novel

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.FileUrl
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ItemUrlBinding
import ani.dantotsu.others.Download.download
import ani.dantotsu.parsers.Book
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.tryWith

class UrlAdapter(private val urls: List<FileUrl>, val book: Book, val novel: String) :
    RecyclerView.Adapter<UrlAdapter.UrlViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrlViewHolder {
        return UrlViewHolder(ItemUrlBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UrlViewHolder, position: Int) {
        val binding = holder.binding
        val url = urls[position]
        binding.urlQuality.text = url.url
        binding.urlDownload.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int = urls.size

    inner class UrlViewHolder(val binding: ItemUrlBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setSafeOnClickListener {
                tryWith(true) {
                    binding.urlDownload.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    download(
                        itemView.context,
                        book,
                        bindingAdapterPosition,
                        novel
                    )
                }
            }
            itemView.setOnLongClickListener {
                val file = urls[bindingAdapterPosition]
                copyToClipboard(file.url, true)
                true
            }
        }
    }
}