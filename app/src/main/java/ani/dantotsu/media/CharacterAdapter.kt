package ani.dantotsu.media

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemCharacterBinding
import ani.dantotsu.loadImage
import ani.dantotsu.setAnimation
import java.io.Serializable

class CharacterAdapter(
    private val characterList: ArrayList<Character>
) : RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {

    interface CharacterClickListener {
        fun onFavoriteClicked(character: Character)
    }

    var listener: CharacterClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val binding = ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CharacterViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(binding.root.context, holder.binding.root)
        val character = characterList[position]
        binding.itemCompactRelation.text = character.role + "  "
        binding.itemCompactImage.loadImage(character.image)
        binding.itemCompactTitle.text = character.name

        // Set the listener for the favorite button
        binding.charFav.setOnClickListener {
            listener?.onFavoriteClicked(character)
        }
    }

    override fun getItemCount(): Int = characterList.size

    inner class CharacterViewHolder(val binding: ItemCharacterBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val character = characterList[bindingAdapterPosition]
                listener?.onFavoriteClicked(character)
            }
        }
    }
}
