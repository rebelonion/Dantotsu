package ani.dantotsu.media

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemFollowerGridBinding
import ani.dantotsu.getAppString
import ani.dantotsu.loadImage
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.User
import ani.dantotsu.setAnimation

class MediaSocialAdapter(
    val user: ArrayList<User>,
    val type: String,
    val activity: FragmentActivity
) : RecyclerView.Adapter<MediaSocialAdapter.FollowerGridViewHolder>() {

    inner class FollowerGridViewHolder(val binding: ItemFollowerGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerGridViewHolder {
        return FollowerGridViewHolder(
            ItemFollowerGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }


    override fun onBindViewHolder(holder: FollowerGridViewHolder, position: Int) {
        holder.binding.apply {
            val user = user[position]
            val score = user.score?.div(10.0) ?: 0.0
            setAnimation(root.context, root)
            profileUserName.text = user.name
            profileInfo.apply {
                text = when (user.status) {
                    "CURRENT" -> if (type == "ANIME") getAppString(R.string.watching) else getAppString(
                        R.string.reading
                    )

                    else -> user.status ?: ""
                }
                visibility = View.VISIBLE
            }
            profileCompactUserProgress.text = user.progress.toString()
            profileCompactScore.text = score.toString()
            " | ${user.totalEpisodes ?: "~"}".also { profileCompactTotal.text = it }
            profileUserAvatar.loadImage(user.pfp)

            val scoreDrawable = if (score == 0.0) R.drawable.score else R.drawable.user_score
            profileCompactScoreBG.apply {
                visibility = View.VISIBLE
                background = ContextCompat.getDrawable(root.context, scoreDrawable)
            }

            profileCompactProgressContainer.visibility = View.VISIBLE

            profileUserAvatar.setOnClickListener {
                ContextCompat.startActivity(
                    root.context,
                    Intent(root.context, ProfileActivity::class.java)
                        .putExtra("userId", user.id),
                    null
                )
            }
            profileUserAvatarContainer.setOnLongClickListener {
                ImageViewDialog.newInstance(
                    activity,
                    activity.getString(R.string.avatar, user.name),
                    user.pfp
                )
            }
        }
    }

    override fun getItemCount(): Int = user.size
}