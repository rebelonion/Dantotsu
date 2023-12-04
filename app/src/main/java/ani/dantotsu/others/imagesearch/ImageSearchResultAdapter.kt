package ani.dantotsu.others.imagesearch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemSearchByImageBinding
import ani.dantotsu.loadImage

class ImageSearchResultAdapter(private val searchResults: List<ImageSearchViewModel.ImageResult>) :
    RecyclerView.Adapter<ImageSearchResultAdapter.SearchResultViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(searchResult: ImageSearchViewModel.ImageResult)
    }

    private var itemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    inner class SearchResultViewHolder(val binding: ItemSearchByImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding =
            ItemSearchByImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val searchResult = searchResults[position]
        val binding = holder.binding
        binding.root.setOnClickListener {
            itemClickListener?.onItemClick(searchResult)
        }

        binding.root.context.apply {
            binding.itemCompactTitle.text = searchResult.anilist?.title?.romaji
            binding.itemTotal.text = getString(
                R.string.similarity_text, String.format("%.1f", searchResult.similarity?.times(100))
            )

            binding.episodeNumber.text =
                getString(R.string.episode_num, searchResult.episode.toString())
            binding.timeStamp.text = getString(
                R.string.time_range,
                toTimestamp(searchResult.from),
                toTimestamp(searchResult.to)
            )

            binding.itemImage.loadImage(searchResult.image)
        }
    }

    override fun getItemCount(): Int {
        return searchResults.size
    }

    private fun toTimestamp(seconds: Double?): String {
        val minutes = (seconds?.div(60))?.toInt()
        val remainingSeconds = (seconds?.mod(60.0))?.toInt()

        val minutesString = minutes.toString().padStart(2, '0')
        val secondsString = remainingSeconds.toString().padStart(2, '0')

        return "$minutesString:$secondsString"
    }
}
