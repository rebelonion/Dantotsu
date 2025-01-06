package ani.dantotsu.media

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ItemCharacterBinding
import ani.dantotsu.loadImage
import ani.dantotsu.setAnimation
import java.io.Serializable

class CharacterAdapter(
    private val characterList: MutableList<Character>
) : RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val binding =
            ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CharacterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(binding.root.context, holder.binding.root)
        val character = characterList.getOrNull(position) ?: return
        val whitespace = "${if (character.role.lowercase() == "null") "" else character.role}  "
        binding.itemCompactRelation.text = whitespace
        binding.itemCompactImage.loadImage(character.image)
        binding.itemCompactTitle.text = character.name
    }

    override fun getItemCount(): Int = characterList.size
    inner class CharacterViewHolder(val binding: ItemCharacterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val char = characterList[bindingAdapterPosition]
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(
                        itemView.context,
                        CharacterDetailsActivity::class.java
                    ).putExtra("character", char as Serializable),
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
                    characterList[bindingAdapterPosition].name ?: ""
                ); true
            }
        }
    }
}