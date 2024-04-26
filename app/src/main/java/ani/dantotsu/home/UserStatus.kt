package ani.dantotsu.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemUserStatusBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.User
import ani.dantotsu.setAnimation
import ani.dantotsu.settings.saving.PrefManager
import java.io.Serializable

class UserStatus(private val user: ArrayList<User>) :
    RecyclerView.Adapter< UserStatus.UsersViewHolder>() {

    inner class UsersViewHolder(val binding: ItemUserStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
               ContextCompat.startActivity(
                    itemView.context,
                    Intent(
                        itemView.context,
                        StatusActivity::class.java
                    ).putExtra("user", user as Serializable)
                    .putExtra("position", bindingAdapterPosition),
                    null
                )
            }
            itemView.setOnLongClickListener {
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(
                        itemView.context,
                        ProfileActivity::class.java
                    ).putExtra("userId", user[bindingAdapterPosition].id),
                    null
                )
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        return UsersViewHolder(
            ItemUserStatusBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val b = holder.binding
        setAnimation(b.root.context, b.root)
        val user = user[position]
        b.profileUserAvatar.loadImage(user.pfp)
        b.profileUserName.text = user.name

        val watchedActivityIds = PrefManager.getCustomVal<Set<Int>>("${user.id}_activities", setOf())
        val activityIdToStatusList = user.activity.map { watchedActivityIds.contains(it.id) }
        b.profileUserStatusIndicator.setParts(user.activity.size, activityIdToStatusList)

    }

    override fun getItemCount(): Int = user.size
}
