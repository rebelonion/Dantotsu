package ani.dantotsu.media

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.currActivity
import ani.dantotsu.databinding.ItemCharacterDetailsBinding
import ani.dantotsu.others.SpoilerPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin

class CharacterDetailsAdapter(private val character: Character, private val activity: Activity) :
    RecyclerView.Adapter<CharacterDetailsAdapter.GenreViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val binding =
            ItemCharacterDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GenreViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val binding = holder.binding
        val desc =
            (if (character.age != "null") currActivity()!!.getString(R.string.age) + " " + character.age else "") +
                    (if (character.dateOfBirth.toString() != "") currActivity()!!.getString(R.string.birthday) + " " + character.dateOfBirth.toString() else "") +
                    (if (character.gender != "null") currActivity()!!.getString(R.string.gender) + " " + when (character.gender) {
                        "Male" -> currActivity()!!.getString(R.string.male)
                        "Female" -> currActivity()!!.getString(R.string.female)
                        else -> character.gender
                    } else "") + "\n" + character.description

        binding.characterDesc.isTextSelectable
        val markWon = Markwon.builder(activity).usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(SpoilerPlugin()).build()
        markWon.setMarkdown(binding.characterDesc, desc)

    }

    override fun getItemCount(): Int = 1
    inner class GenreViewHolder(val binding: ItemCharacterDetailsBinding) :
        RecyclerView.ViewHolder(binding.root)
}