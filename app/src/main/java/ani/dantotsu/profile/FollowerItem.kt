package ani.dantotsu.profile


import android.text.SpannableString
import android.view.View
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.databinding.ItemFollowerBinding
import ani.dantotsu.loadImage
import com.xwray.groupie.viewbinding.BindableItem

class FollowerItem(
    private val id: Int,
    private val name: SpannableString,
    private val avatar: String?,
    private val banner: String?,
    private val altText: String? = null,
    val clickCallback: (Int) -> Unit
) : BindableItem<ItemFollowerBinding>() {
    private lateinit var binding: ItemFollowerBinding

    override fun bind(viewBinding: ItemFollowerBinding, position: Int) {
        binding = viewBinding
        binding.profileUserName.text = name
        avatar?.let { binding.profileUserAvatar.loadImage(it) }
        altText?.let {
            binding.altText.visibility = View.VISIBLE
            binding.altText.text = it
        }
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