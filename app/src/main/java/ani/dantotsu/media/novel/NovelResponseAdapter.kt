package ani.dantotsu.media.novel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemNovelResponseBinding
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.setAnimation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl

class NovelResponseAdapter(val fragment: NovelReadFragment) : RecyclerView.Adapter<NovelResponseAdapter.ViewHolder>() {
    val list: MutableList<ShowResponse> = mutableListOf()

    inner class ViewHolder(val binding: ItemNovelResponseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind = ItemNovelResponseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(bind)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val novel = list[position]
        setAnimation(fragment.requireContext(), holder.binding.root, fragment.uiSettings)

        val cover = GlideUrl(novel.coverUrl.url){ novel.coverUrl.headers }
        Glide.with(binding.itemEpisodeImage).load(cover).override(400,0).into(binding.itemEpisodeImage)

        binding.itemEpisodeTitle.text = novel.name
        binding.itemEpisodeFiller.text = novel.extra?.get("0") ?: ""
        binding.itemEpisodeDesc2.text = novel.extra?.get("1") ?: ""
        val desc = novel.extra?.get("2")
        binding.itemEpisodeDesc.visibility = if (desc != null && desc.trim(' ') != "") View.VISIBLE else View.GONE
        binding.itemEpisodeDesc.text = desc ?: ""

        binding.root.setOnClickListener {
            BookDialog.newInstance(fragment.novelName, novel, fragment.source)
                .show(fragment.parentFragmentManager, "dialog")
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