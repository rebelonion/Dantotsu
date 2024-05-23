package ani.dantotsu.parsers

import android.graphics.drawable.Drawable
import android.view.View
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemExtensionSelectBinding
import com.xwray.groupie.viewbinding.BindableItem

class ExtensionSelectItem(
    private val name: String,
    private val image: Drawable?,
    private var isSelected: Boolean,
    val selectCallback: (String, Boolean) -> Unit
) : BindableItem<ItemExtensionSelectBinding>() {
    private lateinit var binding: ItemExtensionSelectBinding

    override fun bind(viewBinding: ItemExtensionSelectBinding, position: Int) {
        binding = viewBinding
        binding.extensionNameTextView.text = name
        image?.let {
            binding.extensionIconImageView.setImageDrawable(it)
        }
        binding.extensionCheckBox.setOnCheckedChangeListener(null)
        binding.extensionCheckBox.isChecked = isSelected
        binding.extensionCheckBox.setOnCheckedChangeListener { _, isChecked ->
            isSelected = isChecked
            selectCallback(name, isChecked)
        }
    }

    override fun getLayout(): Int {
        return R.layout.item_extension_select
    }

    override fun initializeViewBinding(view: View): ItemExtensionSelectBinding {
        return ItemExtensionSelectBinding.bind(view)
    }
}