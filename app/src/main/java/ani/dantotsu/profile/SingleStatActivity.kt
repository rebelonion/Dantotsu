package ani.dantotsu.profile

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.databinding.ActivitySingleStatBinding
import ani.dantotsu.initActivity
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions

class SingleStatActivity : AppCompatActivity()
{
    private lateinit var binding: ActivitySingleStatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivitySingleStatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val chartOptions = chartOptions
        if (chartOptions != null) {
            val typedvalue = TypedValue()
            theme.resolveAttribute(android.R.attr.windowBackground, typedvalue, true)
            chartOptions.chart?.backgroundColor = typedvalue.data
            binding.chartView.aa_drawChartWithChartOptions(chartOptions)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            toast("No chart data")
            finish()
        }
    }

    companion object {
        var chartOptions: AAOptions? = null  // I cba to pass this through an intent
    }
}