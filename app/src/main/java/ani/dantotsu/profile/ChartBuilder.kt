package ani.dantotsu.profile

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartStackingType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartZoomType
import com.github.aachartmodel.aainfographics.aachartcreator.AADataElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.aa_toAAOptions
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AADataLabels
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAItemStyle
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAScrollablePlotArea
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAYAxis
import com.github.aachartmodel.aainfographics.aatools.AAColor

class ChartBuilder {
    companion object {
        enum class ChartType {
            OneDimensional, TwoDimensional
        }

        enum class StatType {
            COUNT, TIME, MEAN_SCORE
        }

        enum class MediaType {
            ANIME, MANGA
        }

        fun buildChart(
            context: Context,
            chartType: ChartType,
            aaChartType: AAChartType,
            statType: StatType,
            mediaType: MediaType,
            names: List<Any>,
            statData: List<Number>,
            xAxisName: String = "X Axis",
            xAxisTickInterval: Int? = null,
            polar: Boolean = false,
            categories: List<String>? = null,
            scrollPos: Float? = null,
        ): AAOptions {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimary,
                typedValue,
                true
            )
            val primaryColor = typedValue.data
            val palette = generateColorPalette(primaryColor, names.size)
            val aaChartModel = when (chartType) {
                ChartType.OneDimensional -> {
                    val chart = AAChartModel()
                        .chartType(aaChartType)
                        .subtitle(getTypeName(statType, mediaType))
                        .zoomType(AAChartZoomType.None)
                        .dataLabelsEnabled(true)
                        .series(
                            get1DElements(
                                names,
                                statData,
                                palette,
                                primaryColor
                            )
                        )
                    xAxisTickInterval?.let { chart.xAxisTickInterval(it) }
                    categories?.let { chart.categories(it.toTypedArray()) }
                    chart
                }

                ChartType.TwoDimensional -> {
                    val hexColorsArray: Array<Any> =
                        palette.map { String.format("#%06X", 0xFFFFFF and it) }.toTypedArray()
                    val chart = AAChartModel()
                        .chartType(aaChartType)
                        .subtitle(getTypeName(statType, mediaType))
                        .zoomType(AAChartZoomType.None)
                        .dataLabelsEnabled(false)
                        .yAxisTitle(getTypeName(statType, mediaType))
                        .stacking(AAChartStackingType.Normal)
                        .series(get2DElements(names, statData, primaryColor))
                        .colorsTheme(hexColorsArray)

                    xAxisTickInterval?.let { chart.xAxisTickInterval(it) }
                    categories?.let { chart.categories(it.toTypedArray()) }

                    chart
                }
            }
            val aaOptions = aaChartModel.aa_toAAOptions()
            aaOptions.chart?.polar = polar
            aaOptions.tooltip?.apply {
                headerFormat
                formatter(
                    getToolTipFunction(
                        chartType,
                        xAxisName,
                        getTypeName(statType, mediaType)
                    )
                )
            }
            aaOptions.legend?.apply {
                enabled(true)
                    .labelFormat = "{name}: {y}"
            }
            aaOptions.plotOptions?.series?.connectNulls(true)
            aaOptions.chart?.panning = true

            scrollPos?.let {
                aaOptions.chart?.scrollablePlotArea(AAScrollablePlotArea().scrollPositionX(scrollPos))
                aaOptions.chart?.scrollablePlotArea?.minWidth((context.resources.displayMetrics.widthPixels.toFloat() / context.resources.displayMetrics.density) * (names.size.toFloat() / 18.0f))
            }
            val min = statData.minOfOrNull { it.toDouble() } ?: 0.0
            val max = statData.maxOfOrNull { it.toDouble() } ?: 0.0
            val aaYaxis = AAYAxis().min(min).max(max)
            val tickInterval = when (max) {
                in 0.0..10.0 -> 1.0
                in 10.0..30.0 -> 5.0
                in 30.0..100.0 -> 10.0
                in 100.0..1000.0 -> 100.0
                in 1000.0..10000.0 -> 1000.0
                else -> 10000.0
            }
            aaYaxis.tickInterval(tickInterval)
            aaOptions.yAxis(aaYaxis)

            setColors(aaOptions, context, primaryColor)

            return aaOptions
        }

        private fun get2DElements(
            names: List<Any>,
            statData: List<Any>,
            primaryColor: Int
        ): Array<Any> {
            val statValues = mutableListOf<Array<Any>>()
            for (i in statData.indices) {
                statValues.add(arrayOf(names[i], statData[i], statData[i]))
            }
            return arrayOf(
                AASeriesElement().name("Score")
                    .data(statValues.toTypedArray())
                    .dataLabels(
                        AADataLabels()
                            .enabled(false)
                    )
                    .colorByPoint(true)
                    .fillColor(AAColor.rgbaColor(
                        Color.red(primaryColor),
                        Color.green(primaryColor),
                        Color.blue(primaryColor),
                        0.9f
                    ))
            )
        }

        private fun get1DElements(
            names: List<Any>,
            statData: List<Number>,
            colors: List<Int>,
            primaryColor: Int
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
                    element.dataLabels(
                        AADataLabels()
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

        private fun getTypeName(statType: StatType, mediaType: MediaType): String {
            return when (statType) {
                StatType.COUNT -> "Count"
                StatType.TIME -> if (mediaType == MediaType.ANIME) "Hours Watched" else "Chapters Read"
                StatType.MEAN_SCORE -> "Mean Score"
            }
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
                val newHue =
                    (hsv[0] + hueDelta * i) % 360 // Ensure hue stays within the 0-360 range
                val newSaturation = (hsv[1] + saturationDelta * i).coerceIn(0f, 1f)
                val newValue = (hsv[2] + valueDelta * i).coerceIn(0f, 1f)

                val newHsv = floatArrayOf(newHue, newSaturation, newValue)
                palette.add(Color.HSVToColor(newHsv))
            }

            return palette
        }

        private fun setColors(aaOptions: AAOptions, context: Context, primaryColor: Int) {
            val backgroundColor = TypedValue()
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurfaceVariant,
                backgroundColor,
                true
            )
            val backgroundStyle = AAStyle().color(
                AAColor.rgbaColor(
                    Color.red(backgroundColor.data),
                    Color.green(backgroundColor.data),
                    Color.blue(backgroundColor.data),
                    1f
                )
            )
            val colorOnBackground = TypedValue()
            context.theme.resolveAttribute(
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

        private fun getToolTipFunction(
            chartType: ChartType,
            type: String,
            typeName: String
        ): String {
            return when (chartType) {
                ChartType.OneDimensional -> {
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
                }

                ChartType.TwoDimensional -> {
                    """
        function () {
        return '$type: ' +
        this.x +
        '<br/> ' +
        ' $typeName ' +
        this.y
        }
            """.trimIndent()
                }
            }
        }
    }
}