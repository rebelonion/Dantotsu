package ani.dantotsu.profile

import android.content.Intent
import android.view.View
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemChartBinding
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions
import com.xwray.groupie.viewbinding.BindableItem

class ChartItem(
    private val title: String,
    private val aaOptions: AAOptions,
    private val activity: ProfileActivity): BindableItem<ItemChartBinding>() {
    private lateinit var binding: ItemChartBinding
    override fun bind(viewBinding: ItemChartBinding, position: Int) {
        binding = viewBinding
        binding.typeText.text = title
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
}