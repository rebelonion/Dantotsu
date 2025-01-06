package ani.dantotsu.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemExtensionBinding
import ani.dantotsu.notifications.subscription.SubscriptionHelper
import ani.dantotsu.util.customAlertDialog
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem

class SubscriptionSource(
    private val parserName: String,
    private val subscriptions: MutableList<SubscriptionHelper.Companion.SubscribeMedia>,
    private val adapter: GroupieAdapter,
    private var parserIcon: Drawable? = null,
    private val onGroupRemoved: (SubscriptionSource) -> Unit
) : BindableItem<ItemExtensionBinding>() {
    private lateinit var binding: ItemExtensionBinding
    private var isExpanded = false

    override fun bind(viewBinding: ItemExtensionBinding, position: Int) {
        binding = viewBinding
        binding.extensionNameTextView.text = parserName
        updateSubscriptionCount()
        binding.extensionSubscriptions.visibility = View.VISIBLE
        binding.root.setOnClickListener {
            isExpanded = !isExpanded
            toggleSubscriptions()
        }
        binding.root.setOnLongClickListener {
            showRemoveAllSubscriptionsDialog(it.context)
            true
        }
        binding.extensionIconImageView.visibility = View.VISIBLE
        val layoutParams =
            binding.extensionIconImageView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.leftMargin = 28
        binding.extensionIconImageView.layoutParams = layoutParams

        parserIcon?.let {
            binding.extensionIconImageView.setImageDrawable(it)
        } ?: run {
            binding.extensionIconImageView.setImageResource(R.drawable.control_background_40dp)
        }

        binding.extensionPinImageView.visibility = View.GONE
        binding.extensionVersionTextView.visibility = View.GONE
        binding.deleteTextView.visibility = View.GONE
        binding.updateTextView.visibility = View.GONE
        binding.settingsImageView.visibility = View.GONE
    }

    private fun updateSubscriptionCount() {
        binding.subscriptionCount.text = subscriptions.size.toString()
        binding.subscriptionCount.visibility =
            if (subscriptions.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showRemoveAllSubscriptionsDialog(context: Context) {
        context.customAlertDialog().apply {
            setTitle(R.string.remove_all_subscriptions)
            setMessage(R.string.remove_all_subscriptions_desc, parserName)
            setPosButton(R.string.ok) { removeAllSubscriptions() }
            setNegButton(R.string.cancel)
            show()
        }
    }

    private fun removeAllSubscriptions() {
        subscriptions.forEach { subscription ->
            SubscriptionHelper.deleteSubscription(subscription.id, false)
        }
        if (isExpanded) {
            val startPosition = adapter.getAdapterPosition(this) + 1
            repeat(subscriptions.size) {
                adapter.removeGroupAtAdapterPosition(startPosition)
            }
        }
        subscriptions.clear()
        onGroupRemoved(this)
    }

    private fun removeSubscription(id: Any?) {
        subscriptions.removeAll { it.id == id }
        updateSubscriptionCount()
        if (subscriptions.isEmpty()) {
            onGroupRemoved(this)
        } else {
            adapter.notifyItemChanged(adapter.getAdapterPosition(this))
        }
    }

    private fun toggleSubscriptions() {
        val startPosition = adapter.getAdapterPosition(this) + 1
        if (isExpanded) {
            subscriptions.forEachIndexed { index, subscribeMedia ->
                adapter.add(
                    startPosition + index,
                    SubscriptionItem(subscribeMedia.id, subscribeMedia, adapter) { removedId ->
                        removeSubscription(removedId)
                    })
            }
        } else {
            repeat(subscriptions.size) {
                adapter.removeGroupAtAdapterPosition(startPosition)
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_extension

    override fun initializeViewBinding(view: View): ItemExtensionBinding =
        ItemExtensionBinding.bind(view)
}