package ani.dantotsu.media

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.currActivity
import ani.dantotsu.databinding.ItemCharacterDetailsBinding

class CharacterDetailsAdapter(private val character: Character, private val activity: Activity) :
    RecyclerView.Adapter<CharacterDetailsAdapter.GenreViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val binding =
            ItemCharacterDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val binding = holder.binding
        val desc =
            (if (character.id == 4004)
                "![za wardo](https://media1.tenor.com/m/_z1tmCJnL2wAAAAd/za-warudo.gif) \n" else "") +
                    (if (character.age != "null") "${currActivity()!!.getString(R.string.age)} ${character.age}" else "") +
                    (if (character.dateOfBirth.toString() != "")
                        "${currActivity()!!.getString(R.string.birthday)} ${character.dateOfBirth.toString()}" else "") +
                    (if (character.gender != "null")
                        currActivity()!!.getString(R.string.gender) + " " + when (character.gender) {
                            currActivity()!!.getString(R.string.male) -> currActivity()!!.getString(
                                R.string.male
                            )

                            currActivity()!!.getString(R.string.female) -> currActivity()!!.getString(
                                R.string.female
                            )

                            else -> character.gender
                        } else "") + "\n" + character.description

        binding.characterDesc.isTextSelectable
        val markWon = buildMarkwon(activity)
        markWon.setMarkdown(binding.characterDesc, desc.replace("~!", "||").replace("!~", "||"))
        binding.voiceActorRecycler.adapter = AuthorAdapter(character.voiceActor ?: arrayListOf())
        binding.voiceActorRecycler.layoutManager = LinearLayoutManager(
            activity, LinearLayoutManager.HORIZONTAL, false
        )
        if (binding.voiceActorRecycler.adapter!!.itemCount == 0) {
            binding.voiceActorContainer.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = 1
    inner class GenreViewHolder(val binding: ItemCharacterDetailsBinding) :
        RecyclerView.ViewHolder(binding.root)
}