package ani.dantotsu.profile

import android.view.View
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemChartBinding
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions
import com.xwray.groupie.viewbinding.BindableItem

class ChartItem(
    private val title: String,
    private val aaOptions: AAOptions): BindableItem<ItemChartBinding>() {
    private lateinit var binding: ItemChartBinding
    override fun bind(viewBinding: ItemChartBinding, position: Int) {
        binding = viewBinding
        binding.typeText.text = title
        binding.chartView.aa_drawChartWithChartOptions(aaOptions)
    }

    override fun getLayout(): Int {
        return R.layout.item_chart
    }

    override fun initializeViewBinding(view: View): ItemChartBinding {
        return ItemChartBinding.bind(view)
    }
}