package ani.dantotsu.profile


import android.text.SpannableString
import android.view.View
import androidx.viewbinding.ViewBinding
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.databinding.ItemFollowerBinding
import ani.dantotsu.databinding.ItemFollowerGridBinding
import ani.dantotsu.loadImage
import com.xwray.groupie.viewbinding.BindableItem

class FollowerItem(
    private val grid: Boolean,
    private val id: Int,
    private val name: SpannableString,
    private val avatar: String?,
    private val banner: String?,
    val clickCallback: (Int) -> Unit
) : BindableItem<ViewBinding>() {

    override fun bind(viewBinding: ViewBinding, position: Int) {
        if (grid) {
            val binding = viewBinding as ItemFollowerGridBinding
            binding.profileUserName.text = name
            avatar?.let { binding.profileUserAvatar.loadImage(it) }
            binding.root.setOnClickListener { clickCallback(id) }
        } else {
            val binding = viewBinding as ItemFollowerBinding
            binding.profileUserName.text = name
            avatar?.let { binding.profileUserAvatar.loadImage(it) }
            blurImage(binding.profileBannerImage, banner ?: avatar)
            binding.root.setOnClickListener { clickCallback(id) }
        }
    }

    override fun getLayout(): Int {
        return if (grid) R.layout.item_follower_grid else R.layout.item_follower
    }

    override fun initializeViewBinding(view: View): ViewBinding {
        return if (grid) ItemFollowerGridBinding.bind(view) else ItemFollowerBinding.bind(view)
    }
}