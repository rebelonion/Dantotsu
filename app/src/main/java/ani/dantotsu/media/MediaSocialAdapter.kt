package ani.dantotsu.media

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemFollowerGridBinding
import ani.dantotsu.getAppString
import ani.dantotsu.loadImage
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.User
import ani.dantotsu.setAnimation

class MediaSocialAdapter(private val user: ArrayList<User>, private val type: String) :
    RecyclerView.Adapter<MediaSocialAdapter.DeveloperViewHolder>() {

    inner class DeveloperViewHolder(val binding: ItemFollowerGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeveloperViewHolder {
        return DeveloperViewHolder(
            ItemFollowerGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DeveloperViewHolder, position: Int) {
        holder.binding.apply {
            val user = user[position]
            val score = user.score?.div(10.0) ?: 0.0
            setAnimation(root.context, root)
            profileUserName.text = user.name
            profileInfo.apply {
                text = when (user.status) {
                    "CURRENT" -> if (type == "ANIME") getAppString(R.string.watching) else getAppString(R.string.reading)
                    else -> user.status ?: ""
                }
                visibility = View.VISIBLE
            }
            profileCompactUserProgress.text = user.progress.toString()
            profileCompactScore.text = score.toString()
            profileCompactTotal.text = " | ${user.totalEpisodes ?: "~"}"
            profileUserAvatar.loadImage(user.pfp)

            val scoreDrawable = if (score == 0.0) R.drawable.score else R.drawable.user_score
            profileCompactScoreBG.apply {
                visibility = View.VISIBLE
                background = ContextCompat.getDrawable(root.context, scoreDrawable)
            }

            profileCompactProgressContainer.visibility = View.VISIBLE

            profileUserAvatar.setOnClickListener {
                val intent = Intent(root.context, ProfileActivity::class.java).apply {
                    putExtra("userId", user.id)
                }
                ContextCompat.startActivity(root.context, intent, null)
            }
        }
    }

    override fun getItemCount(): Int = user.size
}