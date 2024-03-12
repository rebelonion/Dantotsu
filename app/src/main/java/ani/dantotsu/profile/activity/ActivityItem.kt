package ani.dantotsu.profile.activity

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.ItemActivityBinding
import ani.dantotsu.loadImage
import ani.dantotsu.snackString
import ani.dantotsu.util.AniMarkdown.Companion.getBasicAniHTML
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityItem(
    private val activity: Activity,
    val clickCallback: (Int, type: String) -> Unit
) : BindableItem<ItemActivityBinding>() {
    private lateinit var binding: ItemActivityBinding
    private lateinit var repliesAdapter: GroupieAdapter

    @SuppressLint("SetTextI18n")
    override fun bind(viewBinding: ItemActivityBinding, position: Int) {
        binding = viewBinding

        repliesAdapter = GroupieAdapter()
        binding.activityReplies.adapter = repliesAdapter
        binding.activityReplies.layoutManager = LinearLayoutManager(
            binding.root.context,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.activityUserName.text = activity.user?.name ?: activity.messenger?.name
        binding.activityUserAvatar.loadImage(activity.user?.avatar?.medium ?: activity.messenger?.avatar?.medium)
        binding.activityTime.text = ActivityItemBuilder.getDateTime(activity.createdAt)
        val likeColor = ContextCompat.getColor(binding.root.context, R.color.yt_red)
        val notLikeColor = ContextCompat.getColor(binding.root.context, R.color.bg_opp)
        binding.activityLike.setColorFilter(if (activity.isLiked == true) likeColor else notLikeColor)
        binding.commentRepliesContainer.visibility =
            if (activity.replyCount > 0) View.VISIBLE else View.GONE
        binding.commentRepliesContainer.setOnClickListener {
            when (binding.activityReplies.visibility) {
                View.GONE -> {
                    repliesAdapter.addAll(
                        activity.replies?.map { ActivityReplyItem(it) } ?: emptyList()
                    )
                    binding.activityReplies.visibility = View.VISIBLE
                    binding.commentTotalReplies.text = "Hide replies"
                }
                else -> {
                    repliesAdapter.clear()
                    binding.activityReplies.visibility = View.GONE
                    binding.commentTotalReplies.text = "View replies"

                }
            }
        }
        binding.activityLikeCount.text = (activity.likeCount?:0).toString()
        binding.activityLike.setOnClickListener {
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
                        binding.activityLikeCount.text = (activity.likeCount?:0).toString()
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
                binding.activityText.text =
                    """${activity.user!!.name} ${activity.status} ${activity.progress ?: ""}"""
                binding.activityCover.loadImage(cover)
                blurImage(binding.activityBannerImage, banner ?: cover)

            }

            "TextActivity" -> {
                binding.activityBannerContainer.visibility = View.GONE
                binding.activityContent.visibility = View.VISIBLE
                if (!(context as android.app.Activity).isDestroyed) {
                    val markwon = buildMarkwon(context, false)
                    markwon.setMarkdown(binding.activityContent, getBasicAniHTML(activity.text ?: ""))
                }
            }

            "MessageActivity" -> {
                binding.activityBannerContainer.visibility = View.GONE
                binding.activityContent.visibility = View.VISIBLE
                if (!(context as android.app.Activity).isDestroyed) {
                    val markwon = buildMarkwon(context, false)
                    markwon.setMarkdown(binding.activityContent, getBasicAniHTML(activity.message ?: ""))
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