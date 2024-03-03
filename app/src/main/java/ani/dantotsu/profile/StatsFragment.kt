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
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartStackingType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartZoomType
import com.github.aachartmodel.aainfographics.aachartcreator.AADataElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.aa_toAAOptions
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAArea
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAChart
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AADataLabels
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAItemStyle
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAMarker
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAPlotOptions
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAYAxis
import com.github.aachartmodel.aainfographics.aatools.AAColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class StatsFragment(private val user: Query.UserProfile, private val activity: ProfileActivity) :
    Fragment() {
    private lateinit var binding: FragmentStatisticsBinding
    private var stats: Query.StatisticsResponse? = null
    private var type: MediaType = MediaType.ANIME
    private var statType: StatType = StatType.COUNT
    private var primaryColor: Int = 0

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

        val typedValue = TypedValue()
        activity.theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimary,
            typedValue,
            true
        )
        primaryColor = typedValue.data

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
                StatType.entries.map { it.name.uppercase(Locale.ROOT) }
            )
        )

        binding.filterContainer.visibility = View.GONE
        activity.lifecycleScope.launch {
            stats = Anilist.query.getUserStatistics(user.id)
            withContext(Dispatchers.Main) {
                binding.filterContainer.visibility = View.VISIBLE
                binding.sourceType.setOnItemClickListener { _, _, i, _ ->
                    type = MediaType.entries.toTypedArray()[i]
                    loadStats(type == MediaType.ANIME)
                }
                binding.sourceFilter.setOnItemClickListener { _, _, i, _ ->
                    statType = StatType.entries.toTypedArray()[i]
                    loadStats(type == MediaType.ANIME)
                }
                loadStats(type == MediaType.ANIME)
                binding.statisticProgressBar.visibility = View.GONE
                binding.chartsContainer.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats(type == MediaType.ANIME)
    }

    private fun loadStats(anime: Boolean) {
        val formatChartModel = getFormatChartModel(anime)
        if (formatChartModel != null) {
            binding.formatChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(formatChartModel)
            binding.formatChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.formatChartView.visibility = View.GONE
        }

        val statusChartModel = getStatusChartModel(anime)
        if (statusChartModel != null) {
            binding.statusChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(statusChartModel)
            binding.statusChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.statusChartView.visibility = View.GONE
        }

        val scoreChartModel = getScoreChartModel(anime)
        if (scoreChartModel != null) {
            binding.scoreChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(scoreChartModel, false, """
        function () {
        return 'score: ' +
        this.x +
        '<br/> ' +
        ' ${getTypeName()} ' +
        this.y
        }
            """.trimIndent()
            )
            binding.scoreChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.scoreChartView.visibility = View.GONE
        }

        val lengthChartModel = getLengthChartModel(anime)
        if (lengthChartModel != null) {
            binding.lengthChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(lengthChartModel)
            binding.lengthChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.lengthChartView.visibility = View.GONE
        }

        val releaseYearChartModel = getReleaseYearChartModel(anime)
        if (releaseYearChartModel != null) {
            binding.releaseYearChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(releaseYearChartModel, false, """
        function () {
        return 'Year: ' +
        this.x +
        '<br/> ' +
        ' ${getTypeName()} ' +
        this.y
        }
            """.trimIndent()
            )
            binding.releaseYearChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.releaseYearChartView.visibility = View.GONE
        }

        val startYearChartModel = getStartYearChartModel(anime)
        if (startYearChartModel != null) {
            binding.startYearChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(startYearChartModel, false, """
        function () {
        return 'Year: ' +
        this.x +
        '<br/> ' +
        ' ${getTypeName()} ' +
        this.y
        }
            """.trimIndent()
            )
            binding.startYearChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.startYearChartView.visibility = View.GONE
        }

        val genreChartModel = getGenreChartModel(anime)
        if (genreChartModel.first != null) {
            binding.genreChartView.visibility = View.VISIBLE
            val aaOptions = buildOptions(genreChartModel.first!!, true, """
        function () {
        return 'Genre: ' +
        this.x +
        '<br/> ' +
        ' ${getTypeName()} ' +
        this.y
        }
            """.trimIndent()
            )
            val min = genreChartModel.second.first
            val max = genreChartModel.second.second
            aaOptions.yAxis = AAYAxis().min(min).max(max).tickInterval(if (max > 100) 20 else 10)
            binding.genreChartView.aa_drawChartWithChartOptions(aaOptions)
        } else {
            binding.genreChartView.visibility = View.GONE
        }


    }

    private fun buildOptions(
        aaChartModel: AAChartModel,
        polar: Boolean = true,
        formatting: String? = null
    ): AAOptions {
        val aaOptions = aaChartModel.aa_toAAOptions()
        aaOptions.chart?.zoomType = "xy"
        aaOptions.chart?.pinchType = "xy"
        aaOptions.chart?.polar = polar
        aaOptions.tooltip?.apply {
            headerFormat
            if (formatting != null) {
                formatter(formatting)
            } else {
                formatter(
                    """
        function () {
        return this.point.name
        + ': <br/> '
        + '<b> '
        +  this.y
        + ', '
        + (this.percentage).toFixed(2)
        + '%'
        }
             """.trimIndent()
                )
            }
        }
        aaOptions.legend?.apply {
            enabled(true)
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
            stats?.data?.user?.statistics?.manga?.formats?.map { it.format } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.formats?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.formats?.map { it.minutesWatched / 60 }
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
        val primaryColor = primaryColor
        val palette = generateColorPalette(primaryColor, names.size)
        return AAChartModel()
            .chartType(AAChartType.Pie)
            .subtitle(getTypeName())
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
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.statuses?.map { it.minutesWatched / 60 }
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
        val palette = generateColorPalette(primaryColor, names.size)
        return AAChartModel()
            .chartType(AAChartType.Funnel)
            .subtitle(getTypeName())
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(true)
            .series(getElements(names, values, palette))
    }

    private fun getScoreChartModel(anime: Boolean): AAChartModel? {
        val names: List<Int> = if (anime) {
            stats?.data?.user?.statistics?.anime?.scores?.map { it.score } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.scores?.map { it.score } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.scores?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.scores?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.scores?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.scores?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.scores?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.scores?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isEmpty() || values.isEmpty())
            return null
        val palette = generateColorPalette(primaryColor, names.size)
        return AAChartModel()
            .chartType(AAChartType.Column)
            .subtitle(getTypeName())
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(false)
            .yAxisTitle(getTypeName())
            .xAxisTickInterval(10)
            .stacking(AAChartStackingType.Normal)
            .series(getElements(names, values, palette))
    }

    private fun getLengthChartModel(anime: Boolean): AAChartModel? {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.lengths?.map { it.length?: "unknown" } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.lengths?.map { it.length?: "unknown" } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.lengths?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.lengths?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.lengths?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.lengths?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.lengths?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.lengths?.map { it.meanScore }
            } ?: emptyList()
        }
        //clear nulls from names
        if (names.isEmpty() || values.isEmpty())
            return null
        val palette = generateColorPalette(primaryColor, names.size)
        return AAChartModel()
            .chartType(AAChartType.Pyramid)
            .subtitle(getTypeName())
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(true)
            .series(getElements(names, values, palette))
    }

    private fun getReleaseYearChartModel(anime: Boolean): AAChartModel? {
        val names: List<Number> = if (anime) {
            stats?.data?.user?.statistics?.anime?.releaseYears?.map { it.releaseYear } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.releaseYears?.map { it.releaseYear } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.releaseYears?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.releaseYears?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.releaseYears?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.releaseYears?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.releaseYears?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.releaseYears?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isEmpty() || values.isEmpty())
            return null
        val palette = generateColorPalette(primaryColor, names.size)
        val hexColorsArray: Array<Any> = palette.map { String.format("#%06X", 0xFFFFFF and it) }.toTypedArray()
        return AAChartModel()
            .chartType(AAChartType.Bubble)
            .subtitle(getTypeName())
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(false)
            .yAxisTitle(getTypeName())
            .stacking(AAChartStackingType.Normal)
            .series(getElementsSimple(names, values))
            .colorsTheme(hexColorsArray)
    }

    private fun getStartYearChartModel(anime: Boolean): AAChartModel? {
        val names: List<Number> = if (anime) {
            stats?.data?.user?.statistics?.anime?.startYears?.map { it.startYear } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.startYears?.map { it.startYear } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.startYears?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.startYears?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.startYears?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.startYears?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.startYears?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.startYears?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isEmpty() || values.isEmpty())
            return null
        val palette = generateColorPalette(primaryColor, names.size)
        val hexColorsArray: Array<Any> = palette.map { String.format("#%06X", 0xFFFFFF and it) }.toTypedArray()
        return AAChartModel()
            .chartType(AAChartType.Bar)
            .subtitle(getTypeName())
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(false)
            .yAxisTitle(getTypeName())
            .stacking(AAChartStackingType.Normal)
            .series(getElementsSimple(names, values))
            .colorsTheme(hexColorsArray)
    }

    private fun getGenreChartModel(anime: Boolean): Pair<AAChartModel?, Pair<Int, Int>> {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.genres?.map { it.genre } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.genres?.map { it.genre } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.genres?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.genres?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.genres?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.genres?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.genres?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.genres?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isEmpty() || values.isEmpty())
            return Pair(null, Pair(0, 0))
        val palette = generateColorPalette(primaryColor, names.size)
        val hexColorsArray: Array<Any> = palette.map { String.format("#%06X", 0xFFFFFF and it) }.toTypedArray()
        return Pair(AAChartModel()
            .chartType(AAChartType.Area)
            .subtitle(getTypeName())
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(false)
            .legendEnabled(false)
            .yAxisTitle(getTypeName())
            .stacking(AAChartStackingType.Normal)
            .series(getElementsSimple(names, values))
            .colorsTheme(hexColorsArray)
            .categories(names.toTypedArray()),
            Pair(values.minOf { it.toInt() }, values.maxOf { it.toInt() }))
    }

    enum class StatType {
        COUNT, TIME, MEAN_SCORE
    }

    enum class MediaType {
        ANIME, MANGA
    }

    private fun getTypeName(): String {
        return when (statType) {
            StatType.COUNT -> "Count"
            StatType.TIME -> if (type == MediaType.ANIME) "Hours Watched" else "Chapters Read"
            StatType.MEAN_SCORE -> "Mean Score"
        }
    }

    private fun getElements(
        names: List<Any>,
        statData: List<Number>,
        colors: List<Int>
    ): Array<Any> {
        val statDataElements = mutableListOf<AADataElement>()
        for (i in statData.indices) {
            val element = AADataElement()
                .y(statData[i])
                .color(
                    AAColor.rgbaColor(
                        Color.red(colors[i]),
                        Color.green(colors[i]),
                        Color.blue(colors[i]),
                        0.9f
                    )
                )
            if (names[i] is Number) {
                element.x(names[i] as Number)
                element.dataLabels(AADataLabels()
                    .enabled(false)
                    .format("{point.y}")
                    .backgroundColor(AAColor.rgbaColor(255, 255, 255, 0.0f))
                )
            } else {
                element.x(i)
                element.name(names[i] as String)
            }
            statDataElements.add(element)
        }
        return arrayOf(
            AASeriesElement().name("Score").color(primaryColor)
                .data(statDataElements.toTypedArray())
        )
    }

    private fun getElementsSimple(
        names: List<Any>,
        statData: List<Any>
    ): Array<Any> {
        val statValues = mutableListOf<Array<Any>>()
        for (i in statData.indices) {
            statValues.add(arrayOf(names[i], statData[i], statData[i]))
        }
        return arrayOf(
            AASeriesElement().name("Score")
                .data(statValues.toTypedArray())
                .dataLabels(AADataLabels()
                    .enabled(false)
                )
                .colorByPoint(true)
        )
    }

    private fun setColors(aaOptions: AAOptions) {
        val backgroundColor = TypedValue()
        activity.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, backgroundColor, true)
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