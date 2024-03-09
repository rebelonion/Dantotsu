package ani.dantotsu.profile.activity

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.ItemActivityBinding
import ani.dantotsu.loadImage
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.xwray.groupie.viewbinding.BindableItem
import jp.wasabeef.glide.transformations.BlurTransformation

class ActivityItem(
    private val activity: Activity,
    val clickCallback: (Int) -> Unit
): BindableItem<ItemActivityBinding>() {
    private lateinit var binding: ItemActivityBinding
    @SuppressLint("SetTextI18n")
    override fun bind(viewBinding: ItemActivityBinding, position: Int) {
        binding = viewBinding

        binding.activityUserName.text = activity.user?.name
        binding.activityUserAvatar.loadImage(activity.user?.avatar?.medium)
        binding.activityTime.text = ActivityItemBuilder.getDateTime(activity.createdAt)
        val color = if (activity.isLiked == true)
            ContextCompat.getColor(binding.root.context, R.color.yt_red)
        else
            ContextCompat.getColor(binding.root.context, R.color.bg_opp)
        binding.activityFavorite.setColorFilter(color)
        binding.commentRepliesContainer.visibility = if (activity.replyCount > 0) View.VISIBLE else View.GONE

        val context = binding.root.context

        when (activity.typename) {
            "ListActivity" ->{
                binding.activityContent.visibility = View.GONE
                binding.activityBannerContainer.visibility = View.VISIBLE

                binding.activityMediaName.text = activity.media?.title?.userPreferred
                binding.activityText.text = "${activity.user!!.name} ${activity.status} ${activity.media!!.title!!.userPreferred}"
                binding.activityCover.loadImage(activity.media.coverImage?.medium)
                val banner = activity.media.bannerImage
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
                    markwon.setMarkdown(binding.activityContent, activity.text ?: "")
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