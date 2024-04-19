package ani.dantotsu.profile

import android.content.Intent
import android.view.View
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemChartBinding
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AAMoveOverEventMessageModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.OnItemLongClickListener
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder

class ChartItem(
    private val title: String,
    private val aaOptions: AAOptions,
    private val activity: ProfileActivity
) : BindableItem<ItemChartBinding>() {
    private lateinit var binding: ItemChartBinding
    override fun bind(viewBinding: ItemChartBinding, position: Int) {
        binding = viewBinding
        binding.typeText.text = title
        binding.root.visibility = View.INVISIBLE
        binding.chartView.clipToPadding = true
        val callback: AAChartView.AAChartViewCallBack = object : AAChartView.AAChartViewCallBack {

            override fun chartViewDidFinishLoad(aaChartView: AAChartView) {
                binding.root.visibility = View.VISIBLE
            }

            override fun chartViewMoveOverEventMessage(
                aaChartView: AAChartView,
                messageModel: AAMoveOverEventMessageModel
            ) {
            }
        }
        binding.chartView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        binding.chartView.callBack = callback
        binding.chartView.reload()
        binding.chartView.aa_drawChartWithChartOptions(aaOptions)
        binding.openButton.setOnClickListener {
            SingleStatActivity.chartOptions = aaOptions
            activity.startActivity(
                Intent(activity, SingleStatActivity::class.java)
            )
        }
    }

    override fun getLayout(): Int {
        return R.layout.item_chart
    }

    override fun initializeViewBinding(view: View): ItemChartBinding {
        return ItemChartBinding.bind(view)
    }

    override fun bind(viewHolder: GroupieViewHolder<ItemChartBinding>, position: Int) {
        viewHolder.setIsRecyclable(false)
        super.bind(viewHolder, position)
    }

    override fun bind(
        viewHolder: GroupieViewHolder<ItemChartBinding>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        viewHolder.setIsRecyclable(false)
        super.bind(viewHolder, position, payloads)
    }

    override fun bind(
        viewHolder: GroupieViewHolder<ItemChartBinding>,
        position: Int,
        payloads: MutableList<Any>,
        onItemClickListener: OnItemClickListener?,
        onItemLongClickListener: OnItemLongClickListener?
    ) {
        viewHolder.setIsRecyclable(false)
        super.bind(viewHolder, position, payloads, onItemClickListener, onItemLongClickListener)
    }

    override fun getViewType(): Int {
        return 0
    }
}