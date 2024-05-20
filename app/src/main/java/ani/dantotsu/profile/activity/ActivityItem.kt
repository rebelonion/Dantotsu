package ani.dantotsu.profile.activity

import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.ItemActivityBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.User
import ani.dantotsu.profile.UsersDialogFragment
import ani.dantotsu.setAnimation
import ani.dantotsu.snackString
import ani.dantotsu.util.AniMarkdown.Companion.getBasicAniHTML
import ani.dantotsu.util.MarkdownCreatorActivity
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityItem(
    private val activity: Activity,
    val clickCallback: (Int, type: String) -> Unit,
    private val fragActivity: FragmentActivity
) : BindableItem<ItemActivityBinding>() {
    private lateinit var binding: ItemActivityBinding
    private lateinit var repliesAdapter: GroupieAdapter

    override fun bind(viewBinding: ItemActivityBinding, position: Int) {
        binding = viewBinding
        setAnimation(binding.root.context, binding.root)

        repliesAdapter = GroupieAdapter()
        binding.activityReplies.adapter = repliesAdapter
        binding.activityReplies.layoutManager = LinearLayoutManager(
            binding.root.context,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.activityUserName.text = activity.user?.name ?: activity.messenger?.name
        binding.activityUserAvatar.loadImage(
            activity.user?.avatar?.medium ?: activity.messenger?.avatar?.medium
        )
        binding.activityTime.text = ActivityItemBuilder.getDateTime(activity.createdAt)
        val likeColor = ContextCompat.getColor(binding.root.context, R.color.yt_red)
        val notLikeColor = ContextCompat.getColor(binding.root.context, R.color.bg_opp)
        binding.activityLike.setColorFilter(if (activity.isLiked == true) likeColor else notLikeColor)
        binding.commentTotalReplies.isVisible = activity.replyCount > 0
        binding.dot.isVisible = activity.replyCount > 0
        binding.commentTotalReplies.setOnClickListener {
            when (binding.activityReplies.visibility) {
                View.GONE -> {
                    val replyItems = activity.replies?.map {
                        ActivityReplyItem(it,fragActivity) { id, type ->
                            clickCallback(
                                id,
                                type
                            )
                        }
                    } ?: emptyList()
                    repliesAdapter.addAll(replyItems)
                    binding.activityReplies.visibility = View.VISIBLE
                    binding.commentTotalReplies.setText(R.string.hide_replies)
                }

                else -> {
                    repliesAdapter.clear()
                    binding.activityReplies.visibility = View.GONE
                    binding.commentTotalReplies.setText(R.string.view_replies)

                }
            }
        }
        if (activity.isLocked != true) {
            binding.commentReply.setOnClickListener {
                val context = binding.root.context
                ContextCompat.startActivity(
                    context,
                    Intent(context, MarkdownCreatorActivity::class.java)
                        .putExtra("type", "replyActivity")
                        .putExtra("parentId", activity.id),
                    null
                )
            }
        } else {
            binding.commentReply.visibility = View.GONE
            binding.dot.visibility = View.GONE
        }
        val userList = arrayListOf<User>()
        activity.likes?.forEach { i ->
            userList.add(User(i.id, i.name.toString(), i.avatar?.medium, i.bannerImage))
        }
        binding.activityLikeContainer.setOnLongClickListener {
            UsersDialogFragment().apply {
                userList(userList)
                show(fragActivity.supportFragmentManager, "dialog")
            }
            true
        }
        binding.activityLikeCount.text = (activity.likeCount ?: 0).toString()
        binding.activityLikeContainer.setOnClickListener {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                val res = Anilist.query.toggleLike(activity.id, "ACTIVITY")
                withContext(Dispatchers.Main) {
                    if (res != null) {

                        if (activity.isLiked == true) {
                            activity.likeCount = activity.likeCount?.minus(1)
                        } else {
                            activity.likeCount = activity.likeCount?.plus(1)
                        }
                        binding.activityLikeCount.text = (activity.likeCount ?: 0).toString()
                        activity.isLiked = !activity.isLiked!!
                        binding.activityLike.setColorFilter(if (activity.isLiked == true) likeColor else notLikeColor)

                    } else {
                        snackString("Failed to like activity")
                    }
                }
            }
        }
        val context = binding.root.context
        when (activity.typename) {
            "ListActivity" -> {
                val cover = activity.media?.coverImage?.large
                val banner = activity.media?.bannerImage
                binding.activityContent.visibility = View.GONE
                binding.activityBannerContainer.visibility = View.VISIBLE
                binding.activityMediaName.text = activity.media?.title?.userPreferred
                val activityText = "${activity.user!!.name} ${activity.status} ${
                    activity.progress
                        ?: activity.media?.title?.userPreferred
                }"
                binding.activityText.text = activityText
                binding.activityCover.loadImage(cover)
                blurImage(binding.activityBannerImage, banner ?: cover)
                binding.activityAvatarContainer.setOnClickListener {
                    clickCallback(activity.userId ?: -1, "USER")
                }
                binding.activityUserName.setOnClickListener {
                    clickCallback(activity.userId ?: -1, "USER")
                }
                binding.activityCoverContainer.setOnClickListener {
                    clickCallback(activity.media?.id ?: -1, "MEDIA")
                }
                binding.activityMediaName.setOnClickListener {
                    clickCallback(activity.media?.id ?: -1, "MEDIA")
                }
            }

            "TextActivity" -> {
                binding.activityBannerContainer.visibility = View.GONE
                binding.activityContent.visibility = View.VISIBLE
                if (!(context as android.app.Activity).isDestroyed) {
                    val markwon = buildMarkwon(context, false)
                    markwon.setMarkdown(
                        binding.activityContent,
                        getBasicAniHTML(activity.text ?: "")
                    )
                }
                binding.activityAvatarContainer.setOnClickListener {
                    clickCallback(activity.userId ?: -1, "USER")
                }
                binding.activityUserName.setOnClickListener {
                    clickCallback(activity.userId ?: -1, "USER")
                }
            }

            "MessageActivity" -> {
                binding.activityBannerContainer.visibility = View.GONE
                binding.activityContent.visibility = View.VISIBLE
                if (!(context as android.app.Activity).isDestroyed) {
                    val markwon = buildMarkwon(context, false)
                    markwon.setMarkdown(
                        binding.activityContent,
                        getBasicAniHTML(activity.message ?: "")
                    )
                }
                binding.activityAvatarContainer.setOnClickListener {
                    clickCallback(activity.messengerId ?: -1, "USER")
                }
                binding.activityUserName.setOnClickListener {
                    clickCallback(activity.messengerId ?: -1, "USER")
                }
            }
        }
    }

    override fun getLayout(): Int {
        return R.layout.item_activity
    }

    override fun initializeViewBinding(view: View): ItemActivityBinding {
        return ItemActivityBinding.bind(view)
    }
}