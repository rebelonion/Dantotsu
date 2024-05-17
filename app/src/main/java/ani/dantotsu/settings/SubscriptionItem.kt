package ani.dantotsu.settings

import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemSubscriptionBinding
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.notifications.subscription.SubscriptionHelper
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem

class SubscriptionItem(
    val id: Int,
    private val media: SubscriptionHelper.Companion.SubscribeMedia,
    private val adapter: GroupieAdapter
) : BindableItem<ItemSubscriptionBinding>() {
    private lateinit var binding: ItemSubscriptionBinding
    override fun bind(p0: ItemSubscriptionBinding, p1: Int) {
        val context = p0.root.context
        binding = p0
        val parserName = if (media.isAnime)
            SubscriptionHelper.getAnimeParser(media.id).name
        else
            SubscriptionHelper.getMangaParser(media.id).name
        val mediaName = media.name
        val showName = "$mediaName - $parserName"
        binding.subscriptionName.text = showName
        binding.root.setOnClickListener {
            ContextCompat.startActivity(
                context,
                Intent(context, MediaDetailsActivity::class.java).putExtra(
                    "mediaId", media.id
                ),
                null
            )
        }
        binding.deleteSubscription.setOnClickListener {
            SubscriptionHelper.deleteSubscription(id, true)
            adapter.remove(this)
        }
    }

    override fun getLayout(): Int {
        return R.layout.item_subscription
    }

    override fun initializeViewBinding(p0: View): ItemSubscriptionBinding {
        return ItemSubscriptionBinding.bind(p0)
    }
}