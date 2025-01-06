package ani.dantotsu.profile.activity

import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import ani.dantotsu.util.ActivityMarkdownCreator
import ani.dantotsu.util.AniMarkdown.Companion.getBasicAniHTML
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityReplyItem(
    private val reply: ActivityReply,
    private val parentId: Int,
    private val fragActivity: FragmentActivity,
    private val parentAdapter: GroupieAdapter,
    private val clickCallback: (Int, type: String) -> Unit,
) : BindableItem<ItemActivityReplyBinding>() {
    private lateinit var binding: ItemActivityReplyBinding

    override fun bind(viewBinding: ItemActivityReplyBinding, position: Int) {
        binding = viewBinding
        val context = binding.root.context
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        binding.activityUserAvatar.loadImage(reply.user.avatar?.medium)
        binding.activityUserName.text = reply.user.name
        binding.activityTime.text = ActivityItemBuilder.getDateTime(reply.createdAt)
        binding.activityLikeCount.text = reply.likeCount.toString()
        val likeColor = ContextCompat.getColor(context, R.color.yt_red)
        val notLikeColor = ContextCompat.getColor(context, R.color.bg_opp)
        binding.activityLike.setColorFilter(if (reply.isLiked) likeColor else notLikeColor)
        val markwon = buildMarkwon(context)
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
            scope.launch {
                val res = Anilist.mutation.toggleLike(reply.id, "ACTIVITY_REPLY")
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
        binding.activityReply.setOnClickListener {
            ContextCompat.startActivity(
                context,
                Intent(context, ActivityMarkdownCreator::class.java)
                    .putExtra("type", "replyActivity")
                    .putExtra("parentId", parentId)
                    .putExtra("other", "@${reply.user.name} "),
                null
            )
        }
        binding.activityEdit.isVisible = reply.userId == Anilist.userid
        binding.activityEdit.setOnClickListener {
            ContextCompat.startActivity(
                context,
                Intent(context, ActivityMarkdownCreator::class.java)
                    .putExtra("type", "replyActivity")
                    .putExtra("parentId", parentId)
                    .putExtra("other", reply.text)
                    .putExtra("edit", reply.id),
                null
            )
        }
        binding.activityDelete.isVisible = reply.userId == Anilist.userid
        binding.activityDelete.setOnClickListener {
            scope.launch {
                val res = Anilist.mutation.deleteActivityReply(reply.id)
                withContext(Dispatchers.Main) {
                    if (res) {
                        snackString("Deleted")
                        parentAdapter.remove(this@ActivityReplyItem)
                    } else {
                        snackString("Failed to delete")
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