package ani.dantotsu.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemCharacterBinding
import ani.dantotsu.loadImage
import ani.dantotsu.parsers.ShowResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class SourceAdapter(
    private val sources: List<ShowResponse>,
    private val dialogFragment: SourceSearchDialogFragment,
    private val scope: CoroutineScope
) : RecyclerView.Adapter<SourceAdapter.SourceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val binding =
            ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        val binding = holder.binding
        val character = sources[position]
        binding.itemCompactImage.loadImage(character.coverUrl, 200)
        binding.itemCompactTitle.isSelected = true
        binding.itemCompactTitle.text = character.name
    }

    override fun getItemCount(): Int = sources.size

    abstract suspend fun onItemClick(source: ShowResponse)

    inner class SourceViewHolder(val binding: ItemCharacterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                dialogFragment.dismiss()
                scope.launch(Dispatchers.IO) { onItemClick(sources[bindingAdapterPosition]) }
            }
            var a = true
            itemView.setOnLongClickListener {
                a = !a
                binding.itemCompactTitle.isSingleLine = a
                true
            }
        }
    }
}