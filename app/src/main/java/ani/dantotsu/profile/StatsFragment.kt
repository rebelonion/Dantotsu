package ani.dantotsu.profile

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.FragmentStatisticsBinding
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartAlignType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartLayoutType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartVerticalAlignType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartZoomType
import com.github.aachartmodel.aainfographics.aachartcreator.AADataElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.aa_toAAOptions
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAItemStyle
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.github.aachartmodel.aainfographics.aatools.AAColor
import com.github.aachartmodel.aainfographics.aatools.AAGradientColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class StatsFragment(private val user: Query.UserProfile, private val activity: ProfileActivity) :
    Fragment() {
    private lateinit var binding: FragmentStatisticsBinding
    private var selected: Int = 0
    private var stats: Query.StatisticsResponse? = null
    private var type: MediaType = MediaType.ANIME
    private var statType: StatType = StatType.COUNT
    var chartType: AAChartType = AAChartType.Pie

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sourceType.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown,
                MediaType.entries.map { it.name.uppercase(Locale.ROOT) }
            )
        )
        binding.sourceFilter.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown,
                AAChartType.entries.map { it.name.uppercase(Locale.ROOT) }
            )
        )

        binding.filterContainer.visibility = View.GONE
        activity.lifecycleScope.launch {
            stats = Anilist.query.getUserStatistics(user.id)
            withContext(Dispatchers.Main) {
                binding.filterContainer.visibility = View.VISIBLE
                binding.sourceType.setOnItemClickListener { _, _, i, _ ->
                    type = MediaType.entries.toTypedArray()[i]
                    updateStats()
                }
                binding.sourceFilter.setOnItemClickListener { _, _, i, _ ->
                    //statType = StatType.entries.toTypedArray()[i]
                    chartType = AAChartType.entries.toTypedArray()[i]
                    updateStats()
                }
                updateStats()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }


    private fun updateStats() {
        when (type) {
            MediaType.ANIME -> loadAnimeStats()
            MediaType.MANGA -> loadMangaStats()
        }
    }

    private fun loadAnimeStats() {
        val formatChartModel = getFormatChartModel(true)
        if (formatChartModel != null) {
            binding.formatChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(formatChartModel)
            binding.formatChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.formatChartView.visibility = View.GONE
        }
        val statusChartModel = getStatusChartModel(true)
        if (statusChartModel != null) {
            binding.statusChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(statusChartModel)
            binding.statusChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.statusChartView.visibility = View.GONE
        }
    }

    private fun loadMangaStats() {
        val formatChartModel = getFormatChartModel(false)
        if (formatChartModel != null) {
            binding.formatChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(formatChartModel)
            binding.formatChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.formatChartView.visibility = View.GONE
        }
        val statusChartModel = getStatusChartModel(false)
        if (statusChartModel != null) {
            binding.statusChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(statusChartModel)
            binding.statusChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.statusChartView.visibility = View.GONE
        }
    }

    private fun buildOptions(aaChartModel: AAChartModel): AAOptions {
        val aaOptions = aaChartModel.aa_toAAOptions()
        aaOptions.chart?.zoomType = "xy"
        aaOptions.chart?.pinchType = "xy"
        aaOptions.chart?.polar = true
        aaOptions.tooltip?.apply {
            shared = true
            formatter(////I want to show {name}: {y}, {percentage:.2f}%
                """
        function () {
        return this.series.name + ': <br/> '
        + '<b> '
        +  this.y
        + ', '
        + (this.percentage).toFixed(2)
        + '%'
        }
             """.trimIndent()
            )
        }
        aaOptions.legend?.apply {
            enabled(true)
                //.verticalAlign(AAChartVerticalAlignType.Top)
                //.layout(AAChartLayoutType.Vertical)
                //.align(AAChartAlignType.Right)
                //.itemMarginTop(10f)
                .labelFormat = "{name}: {y}"
        }
        aaOptions.plotOptions?.series?.connectNulls(true)
        setColors(aaOptions)
        return aaOptions
    }

    private fun getFormatChartModel(anime: Boolean): AAChartModel? {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.formats?.map { it.format } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.countries?.map { it.country } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.formats?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.formats?.map { it.minutesWatched }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.formats?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.formats?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.formats?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.formats?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isEmpty() || values.isEmpty())
            return null
        val primaryColor = getBaseColor(activity)
        val palette = generateColorPalette(primaryColor, names.size)
        return AAChartModel()
            .chartType(AAChartType.Pie)
            .title("Format")
            .subtitle(statType.name.lowercase(Locale.ROOT))
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(true)
            .series(getElements(names, values, palette))
    }

    private fun getStatusChartModel(anime: Boolean): AAChartModel? {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.statuses?.map { it.status } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.statuses?.map { it.status } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.statuses?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.statuses?.map { it.minutesWatched }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.statuses?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.statuses?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.statuses?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.statuses?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isEmpty() || values.isEmpty())
            return null
        val primaryColor = getBaseColor(activity)
        val palette = generateColorPalette(primaryColor, names.size)
        return AAChartModel()
            .chartType(chartType)
            .title("Status")
            .subtitle(statType.name.lowercase(Locale.ROOT))
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(true)
            .series(getElements(names, values, palette))
    }

    enum class StatType {
        COUNT, TIME, MEAN_SCORE
    }

    enum class MediaType {
        ANIME, MANGA
    }

    private fun getElements(
        names: List<String>,
        statData: List<Number>,
        colors: List<Int>
    ): Array<Any> {
        val statDataElements = mutableListOf<AADataElement>()
        for (i in statData.indices) {
            statDataElements.add(
                AADataElement().name(names[i]).y(statData[i]).color(
                    AAColor.rgbaColor(
                        Color.red(colors[i]),
                        Color.green(colors[i]),
                        Color.blue(colors[i]),
                        0.9f
                    )
                )
            )
        }
        return arrayOf(
            AASeriesElement().name("Count").data(statDataElements.toTypedArray()),
        )
    }

    private fun getBaseColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimary,
            typedValue,
            true
        )
        return typedValue.data
    }

    private fun setColors(aaOptions: AAOptions) {
        val backgroundColor = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.windowBackground, backgroundColor, true)
        val backgroundStyle = AAStyle().color(
            AAColor.rgbaColor(
                Color.red(backgroundColor.data),
                Color.green(backgroundColor.data),
                Color.blue(backgroundColor.data),
                1f
            )
        )
        val colorOnBackground = TypedValue()
        activity.theme.resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface,
            colorOnBackground,
            true
        )
        val onBackgroundStyle = AAStyle().color(
            AAColor.rgbaColor(
                Color.red(colorOnBackground.data),
                Color.green(colorOnBackground.data),
                Color.blue(colorOnBackground.data),
                0.9f
            )
        )
        val primaryColor = getBaseColor(activity)


        aaOptions.chart?.backgroundColor(backgroundStyle.color)
        aaOptions.tooltip?.backgroundColor(
            AAColor.rgbaColor(
                Color.red(primaryColor),
                Color.green(primaryColor),
                Color.blue(primaryColor),
                0.9f
            )
        )
        aaOptions.title?.style(onBackgroundStyle)
        aaOptions.subtitle?.style(onBackgroundStyle)
        aaOptions.tooltip?.style(backgroundStyle)
        aaOptions.credits?.style(onBackgroundStyle)
        aaOptions.xAxis?.labels?.style(onBackgroundStyle)
        aaOptions.yAxis?.labels?.style(onBackgroundStyle)
        aaOptions.plotOptions?.series?.dataLabels?.style(onBackgroundStyle)
        aaOptions.plotOptions?.series?.dataLabels?.backgroundColor(backgroundStyle.color)
        aaOptions.legend?.itemStyle(AAItemStyle().color(onBackgroundStyle.color))

        aaOptions.touchEventEnabled(true)
    }

    private fun generateColorPalette(
        baseColor: Int,
        size: Int,
        hueDelta: Float = 8f,
        saturationDelta: Float = 2.02f,
        valueDelta: Float = 2.02f
    ): List<Int> {
        val palette = mutableListOf<Int>()
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)

        for (i in 0 until size) {
            val newHue = (hsv[0] + hueDelta * i) % 360 // Ensure hue stays within the 0-360 range
            val newSaturation = (hsv[1] + saturationDelta * i).coerceIn(0f, 1f)
            val newValue = (hsv[2] + valueDelta * i).coerceIn(0f, 1f)

            val newHsv = floatArrayOf(newHue, newSaturation, newValue)
            palette.add(Color.HSVToColor(newHsv))
        }

        return palette
    }
}