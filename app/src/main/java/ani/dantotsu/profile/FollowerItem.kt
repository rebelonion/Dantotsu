package ani.dantotsu.profile

import android.app.Activity
import android.content.Context
import android.view.View
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.databinding.ItemFollowerBinding
import ani.dantotsu.loadImage
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.xwray.groupie.viewbinding.BindableItem
import jp.wasabeef.glide.transformations.BlurTransformation

class FollowerItem(
    private val id: Int,
    private val name: String,
    private val avatar: String?,
    private val banner: String?,
    val clickCallback: (Int) -> Unit
): BindableItem<ItemFollowerBinding>() {
    private lateinit var binding: ItemFollowerBinding

    override fun bind(viewBinding: ItemFollowerBinding, position: Int) {
        binding = viewBinding
        binding.profileUserName.text = name
        avatar?.let { binding.profileUserAvatar.loadImage(it) }
        blurImage(binding.profileBannerImage, banner ?: avatar)
        binding.root.setOnClickListener { clickCallback(id) }
    }

    override fun getLayout(): Int {
        return R.layout.item_follower
    }

    override fun initializeViewBinding(view: View): ItemFollowerBinding {
        return ItemFollowerBinding.bind(view)
    }
}