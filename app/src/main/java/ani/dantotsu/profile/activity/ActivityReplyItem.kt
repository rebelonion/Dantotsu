package ani.dantotsu.profile.activity

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.ActivityReply
import ani.dantotsu.databinding.ItemActivityReplyBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.User
import ani.dantotsu.profile.UsersDialogFragment
import ani.dantotsu.snackString
import ani.dantotsu.util.AniMarkdown.Companion.getBasicAniHTML
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityReplyItem(
    private val reply: ActivityReply,
    private val fragActivity: FragmentActivity,
    private val clickCallback: (Int, type: String) -> Unit,
) : BindableItem<ItemActivityReplyBinding>() {
    private lateinit var binding: ItemActivityReplyBinding

    override fun bind(viewBinding: ItemActivityReplyBinding, position: Int) {
        binding = viewBinding

        binding.activityUserAvatar.loadImage(reply.user.avatar?.medium)
        binding.activityUserName.text = reply.user.name
        binding.activityTime.text = ActivityItemBuilder.getDateTime(reply.createdAt)
        binding.activityLikeCount.text = reply.likeCount.toString()
        val likeColor = ContextCompat.getColor(binding.root.context, R.color.yt_red)
        val notLikeColor = ContextCompat.getColor(binding.root.context, R.color.bg_opp)
        binding.activityLike.setColorFilter(if (reply.isLiked) likeColor else notLikeColor)
        val markwon = buildMarkwon(binding.root.context)
        markwon.setMarkdown(binding.activityContent, getBasicAniHTML(reply.text))
        val userList = arrayListOf<User>()
        reply.likes?.forEach { i ->
            userList.add(User(i.id, i.name.toString(), i.avatar?.medium, i.bannerImage))
        }
        binding.activityLikeContainer.setOnLongClickListener {
            UsersDialogFragment().apply {
                userList(userList)
                show(fragActivity.supportFragmentManager, "dialog")
            }
            true
        }
        binding.activityLikeContainer.setOnClickListener {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                val res = Anilist.query.toggleLike(reply.id, "ACTIVITY_REPLY")
                withContext(Dispatchers.Main) {
                    if (res != null) {
                        if (reply.isLiked) {
                            reply.likeCount = reply.likeCount.minus(1)
                        } else {
                            reply.likeCount = reply.likeCount.plus(1)
                        }
                        binding.activityLikeCount.text = (reply.likeCount).toString()
                        reply.isLiked = !reply.isLiked
                        binding.activityLike.setColorFilter(if (reply.isLiked) likeColor else notLikeColor)

                    } else {
                        snackString("Failed to like activity")
                    }
                }
            }
        }

        binding.activityAvatarContainer.setOnClickListener {
            clickCallback(reply.userId, "USER")
        }
        binding.activityUserName.setOnClickListener {
            clickCallback(reply.userId, "USER")
        }
    }

    override fun getLayout(): Int {
        return R.layout.item_activity_reply
    }

    override fun initializeViewBinding(view: View): ItemActivityReplyBinding {
        return ItemActivityReplyBinding.bind(view)
    }
}