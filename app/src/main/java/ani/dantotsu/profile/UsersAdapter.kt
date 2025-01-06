package ani.dantotsu.profile

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import ani.dantotsu.databinding.ItemFollowerBinding
import ani.dantotsu.databinding.ItemFollowerGridBinding
import ani.dantotsu.loadImage
import ani.dantotsu.setAnimation


class UsersAdapter(private val user: MutableList<User>, private val grid: Boolean = false) :
    RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    inner class UsersViewHolder(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                ContextCompat.startActivity(
                    binding.root.context, Intent(binding.root.context, ProfileActivity::class.java)
                        .putExtra("userId", user[bindingAdapterPosition].id), null
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        return UsersViewHolder(
            if (grid) ItemFollowerGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ) else
                ItemFollowerBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        setAnimation(holder.binding.root.context, holder.binding.root)
        val user = user.getOrNull(position) ?: return
        if (grid) {
            val b = holder.binding as ItemFollowerGridBinding
            b.profileUserAvatar.loadImage(user.pfp)
            b.profileUserName.text = user.name
            b.profileCompactScoreBG.isVisible = false
            b.profileInfo.isVisible = false
            b.profileCompactProgressContainer.isVisible = false
        } else {
            val b = holder.binding as ItemFollowerBinding
            b.profileUserAvatar.loadImage(user.pfp)
            b.profileBannerImage.loadImage(user.banner ?: user.pfp)
            b.profileUserName.text = user.name
        }
    }

    override fun getItemCount(): Int = user.size
}
