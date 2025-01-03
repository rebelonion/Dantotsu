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
import ani.dantotsu.databinding.ItemCharacterBinding
import ani.dantotsu.loadImage
import ani.dantotsu.setAnimation
import java.io.Serializable

class AuthorAdapter(
    private val authorList: MutableList<Author>,
) : RecyclerView.Adapter<AuthorAdapter.AuthorViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthorViewHolder {
        val binding =
            ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AuthorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuthorViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(binding.root.context, holder.binding.root)
        val author = authorList.getOrNull(position) ?: return
        binding.itemCompactRelation.text = author.role
        binding.itemCompactImage.loadImage(author.image)
        binding.itemCompactTitle.text = author.name
    }

    override fun getItemCount(): Int = authorList.size
    inner class AuthorViewHolder(val binding: ItemCharacterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val author = authorList[bindingAdapterPosition]
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(
                        itemView.context,
                        AuthorActivity::class.java
                    ).putExtra("author", author as Serializable),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        itemView.context as Activity,
                        Pair.create(
                            binding.itemCompactImage,
                            ViewCompat.getTransitionName(binding.itemCompactImage)!!
                        ),
                    ).toBundle()
                )
            }
        }
    }
}