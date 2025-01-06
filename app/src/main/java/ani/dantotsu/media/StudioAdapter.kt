package ani.dantotsu.media

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ItemCharacterBinding
import ani.dantotsu.loadImage
import ani.dantotsu.setAnimation
import java.io.Serializable

class StudioAdapter(
    private val studioList: MutableList<Studio>
) : RecyclerView.Adapter<StudioAdapter.StudioViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudioViewHolder {
        val binding =
            ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudioViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(binding.root.context, holder.binding.root)
        val studio = studioList.getOrNull(position) ?: return
        binding.itemCompactRelation.isVisible = false
        binding.itemCompactImage.loadImage(studio.imageUrl)
        binding.itemCompactTitle.text = studio.name
    }

    override fun getItemCount(): Int = studioList.size
    inner class StudioViewHolder(val binding: ItemCharacterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val studio = studioList[bindingAdapterPosition]
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(
                        itemView.context,
                        StudioActivity::class.java
                    ).putExtra("studio", studio as Serializable),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        itemView.context as Activity,
                        Pair.create(
                            binding.itemCompactImage,
                            ViewCompat.getTransitionName(binding.itemCompactImage)!!
                        ),
                    ).toBundle()
                )
            }
            itemView.setOnLongClickListener {
                copyToClipboard(
                    studioList[bindingAdapterPosition].name
                ); true
            }
        }
    }
}