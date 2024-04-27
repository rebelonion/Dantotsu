package ani.dantotsu.home.status

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ItemUserStatusBinding
import ani.dantotsu.getAppString
import ani.dantotsu.loadImage
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.User
import ani.dantotsu.setAnimation
import ani.dantotsu.settings.saving.PrefManager

class UserStatusAdapter(private val user: ArrayList<User>) :
    RecyclerView.Adapter<UserStatusAdapter.UsersViewHolder>() {

    inner class UsersViewHolder(val binding: ItemUserStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                StatusActivity.user = user
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(
                        itemView.context,
                        StatusActivity::class.java
                    ).putExtra("position", bindingAdapterPosition),
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
        b.profileUserName.text = if (Anilist.userid == user.id) getAppString(R.string.your_story) else user.name

        val watchedActivity = PrefManager.getCustomVal<Set<Int>>("activities", setOf())
        val booleanList = user.activity.map { watchedActivity.contains(it.id) }
        b.profileUserStatusIndicator.setParts(user.activity.size, booleanList, user.id == Anilist.userid)
        val newList = watchedActivity.sorted().takeLast(100)
        PrefManager.setCustomVal("activities",newList.toSet())
    }

    override fun getItemCount(): Int = user.size
}
