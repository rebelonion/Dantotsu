package ani.dantotsu.profile.activity

import android.view.View
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemNotificationBinding
import com.xwray.groupie.viewbinding.BindableItem

class ActivityItem(
): BindableItem<ItemNotificationBinding>() {
    private lateinit var binding: ItemNotificationBinding
    override fun bind(viewBinding: ItemNotificationBinding, position: Int) {
        binding = viewBinding
    }

    override fun getLayout(): Int {
        return R.layout.item_notification
    }

    override fun initializeViewBinding(view: View): ItemNotificationBinding {
        return ItemNotificationBinding.bind(view)
    }
}