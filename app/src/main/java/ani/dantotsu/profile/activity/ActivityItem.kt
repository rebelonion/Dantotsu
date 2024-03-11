package ani.dantotsu.profile.activity

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import ani.dantotsu.R
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

    @SuppressLint("SetTextI18n")
    override fun bind(viewBinding: ItemActivityBinding, position: Int) {
        binding = viewBinding

        binding.activityUserName.text = activity.user?.name ?: activity.messenger?.name
        binding.activityUserAvatar.loadImage(activity.user?.avatar?.medium ?: activity.messenger?.avatar?.medium)
        binding.activityTime.text = ActivityItemBuilder.getDateTime(activity.createdAt)
        val likeColor = ContextCompat.getColor(binding.root.context, R.color.yt_red)
        val notLikeColor = ContextCompat.getColor(binding.root.context, R.color.bg_opp)
        binding.activityLike.setColorFilter(if (activity.isLiked == true) likeColor else notLikeColor)
        binding.commentRepliesContainer.visibility =
            if (activity.replyCount > 0) View.VISIBLE else View.GONE
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
                binding.activityContent.visibility = View.GONE
                binding.activityBannerContainer.visibility = View.VISIBLE

                binding.activityMediaName.text = activity.media?.title?.userPreferred
                binding.activityText.text =
                    """${activity.user!!.name} ${activity.status} ${activity.progress ?: ""}"""
                binding.activityCover.loadImage(activity.media?.coverImage?.medium)
                val banner = activity.media?.bannerImage
                if (banner != null) {
                    if (!(context as android.app.Activity).isDestroyed) {
                        Glide.with(context as Context)
                            .load(GlideUrl(banner))
                            .diskCacheStrategy(DiskCacheStrategy.ALL).override(400)
                            .apply(RequestOptions.bitmapTransform(BlurTransformation(2, 2)))
                            .into(binding.activityBannerImage)
                    }
                } else {
                    binding.activityBannerImage.setImageResource(R.drawable.linear_gradient_bg)
                }
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
                    markwon.setMarkdown(binding.activityContent, activity.message ?: "")
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