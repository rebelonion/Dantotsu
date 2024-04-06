package ani.dantotsu.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemFollowerGridBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.User
import ani.dantotsu.setAnimation

class MediaSocialAdapter(private val user: ArrayList<User>) :
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

    override fun onBindViewHolder(holder: DeveloperViewHolder, position: Int) {
        val b = holder.binding
        setAnimation(b.root.context, b.root)
        val user = user[position]
        b.profileUserName.text = user.name
        b.profileUserAvatar.loadImage(user.pfp)
        b.profileInfo.text = user.info
        b.profileInfo.visibility = View.VISIBLE
        b.profileUserAvatar.setOnClickListener {
            val intent = Intent(b.root.context, ProfileActivity::class.java)
            intent.putExtra("userId", user.id)
            ContextCompat.startActivity(b.root.context, intent, null)
        }
    }

    override fun getItemCount(): Int = user.size
}