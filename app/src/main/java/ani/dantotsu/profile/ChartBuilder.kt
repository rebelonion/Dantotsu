package ani.dantotsu.profile

import android.content.Context
import android.graphics.Color
import ani.dantotsu.getThemeColor
import ani.dantotsu.util.ColorEditor
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
            COUNT, TIME, AVG_SCORE
        }

        enum class MediaType {
            ANIME, MANGA
        }

        data class ChartPacket(
            val username: String,
            val names: List<Any>,
            var statData: List<Number>
        )

        fun buildChart(
            context: Context,
            passedChartType: ChartType,
            passedAaChartType: AAChartType,
            statType: StatType,
            mediaType: MediaType,
            chartPackets: List<ChartPacket>,
            xAxisName: String,
            xAxisTickInterval: Int? = null,
            polar: Boolean = false,
            passedCategories: List<String>? = null,
            scrollPos: Float? = null,
            normalize: Boolean = false
        ): AAOptions {
            val primaryColor =
                context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
            var chartType = passedChartType
            var aaChartType = passedAaChartType
            var categories = passedCategories
            if (chartType == ChartType.OneDimensional && chartPackets.size != 1) {
                //need to convert to 2D
                chartType = ChartType.TwoDimensional
                aaChartType = AAChartType.Column
                categories = chartPackets[0].names.map { it.toString() }
            }
            if (normalize && chartPackets.size > 1) {
                chartPackets.forEach {
                    it.statData = normalizeData(it.statData)
                }
            }

            val namesMax = chartPackets.maxOf { it.names.size }
            val palette = ColorEditor.generateColorPalette(primaryColor, namesMax)
            val aaChartModel = when (chartType) {
                ChartType.OneDimensional -> {
                    val chart = AAChartModel()
                        .chartType(aaChartType)
                        .subtitle(
                            getTypeName(
                                statType,
                                mediaType
                            ) + if (normalize && chartPackets.size > 1) " (Normalized)" else ""
                        )
                        .zoomType(AAChartZoomType.None)
                        .dataLabelsEnabled(true)
                    val elements: MutableList<Any> = mutableListOf()
                    chartPackets.forEachIndexed { index, chartPacket ->
                        val element = AASeriesElement()
                            .name(chartPacket.username)
                            .data(
                                get1DElements(
                                    chartPacket.names,
                                    chartPacket.statData,
                                    palette
                                )
                            )
                        if (index == 0) {
                            element.color(primaryColor)
                        } else {
                            element.color(ColorEditor.oppositeColor(primaryColor))
                        }
                        elements.add(element)

                    }
                    chart.series(elements.toTypedArray())
                    xAxisTickInterval?.let { chart.xAxisTickInterval(it) }
                    categories?.let { chart.categories(it.toTypedArray()) }
                    chart
                }

                ChartType.TwoDimensional -> {
                    val hexColorsArray: Array<Any> =
                        palette.map { String.format("#%06X", 0xFFFFFF and it) }.toTypedArray()
                    val chart = AAChartModel()
                        .chartType(aaChartType)
                        .subtitle(
                            getTypeName(
                                statType,
                                mediaType
                            ) + if (normalize && chartPackets.size > 1) " (Normalized)" else ""
                        )
                        .zoomType(AAChartZoomType.None)
                        .dataLabelsEnabled(false)
                        .yAxisTitle(
                            getTypeName(
                                statType,
                                mediaType
                            ) + if (normalize && chartPackets.size > 1) " (Normalized)" else ""
                        )
                    if (chartPackets.size == 1) {
                        chart.colorsTheme(hexColorsArray)
                    }

                    val elements: MutableList<AASeriesElement> = mutableListOf()
                    chartPackets.forEachIndexed { index, chartPacket ->
                        val element = get2DElements(
                            chartPacket.names,
                            chartPacket.statData,
                            chartPackets.size == 1
                        )
                        element.name(chartPacket.username)

                        if (index == 0) {
                            element.color(
                                AAColor.rgbaColor(
                                    Color.red(primaryColor),
                                    Color.green(primaryColor),
                                    Color.blue(primaryColor),
                                    0.9f
                                )
                            )

                        } else {
                            element.color(
                                AAColor.rgbaColor(
                                    Color.red(
                                        ColorEditor.oppositeColor(
                                            primaryColor
                                        )
                                    ),
                                    Color.green(ColorEditor.oppositeColor(primaryColor)),
                                    Color.blue(ColorEditor.oppositeColor(primaryColor)),
                                    0.9f
                                )
                            )
                        }
                        if (chartPackets.size == 1) {
                            element.fillColor(
                                AAColor.rgbaColor(
                                    Color.red(primaryColor),
                                    Color.green(primaryColor),
                                    Color.blue(primaryColor),
                                    0.9f
                                )
                            )
                        }
                        elements.add(element)
                    }
                    chart.series(elements.toTypedArray())

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
                        getTypeName(statType, mediaType),
                        chartPackets.size
                    )
                )
                if (chartPackets.size > 1) {
                    useHTML(true)
                }
            }
            aaOptions.legend?.apply {
                enabled(true)
                    .labelFormat = "{name}"
            }
            aaOptions.plotOptions?.series?.connectNulls(false)
            aaOptions.plotOptions?.series?.stacking(AAChartStackingType.False)
            aaOptions.chart?.panning = true

            scrollPos?.let {
                aaOptions.chart?.scrollablePlotArea(AAScrollablePlotArea().scrollPositionX(scrollPos))
                aaOptions.chart?.scrollablePlotArea?.minWidth((context.resources.displayMetrics.widthPixels.toFloat() / context.resources.displayMetrics.density) * (namesMax.toFloat() / 18.0f))
            }
            val allStatData = chartPackets.flatMap { it.statData }
            val min = (allStatData.minOfOrNull { it.toDouble() } ?: 0.0) - 1.0
            val coercedMin = min.coerceAtLeast(0.0)
            val max = allStatData.maxOfOrNull { it.toDouble() } ?: 0.0

            val aaYaxis = AAYAxis().min(coercedMin).max(max)
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

            setColors(aaOptions, context)

            return aaOptions
        }

        private fun get2DElements(
            names: List<Any>,
            statData: List<Any>,
            colorByPoint: Boolean
        ): AASeriesElement {
            val statValues = mutableListOf<Array<Any>>()
            for (i in statData.indices) {
                statValues.add(arrayOf(names[i], statData[i], statData[i]))
            }
            return AASeriesElement()
                .data(statValues.toTypedArray())
                .dataLabels(
                    AADataLabels()
                        .enabled(false)
                )
                .colorByPoint(colorByPoint)
        }

        private fun get1DElements(
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
            return statDataElements.toTypedArray()
        }

        private fun getTypeName(statType: StatType, mediaType: MediaType): String {
            return when (statType) {
                StatType.COUNT -> "Count"
                StatType.TIME -> if (mediaType == MediaType.ANIME) "Hours Watched" else "Chapters Read"
                StatType.AVG_SCORE -> "Mean Score"
            }
        }

        private fun normalizeData(data: List<Number>): List<Number> {
            if (data.isEmpty()) {
                return data
            }
            val max = data.maxOf { it.toDouble() }
            return data.map { (it.toDouble() / max) * 100 }
        }

        private fun setColors(aaOptions: AAOptions, context: Context) {
            val backgroundColor =
                context.getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant)
            val backgroundStyle = AAStyle().color(
                AAColor.rgbaColor(
                    Color.red(backgroundColor),
                    Color.green(backgroundColor),
                    Color.blue(backgroundColor),
                    1f
                )
            )
            val colorOnBackground =
                context.getThemeColor(com.google.android.material.R.attr.colorOnSurface)
            val onBackgroundStyle = AAStyle().color(
                AAColor.rgbaColor(
                    Color.red(colorOnBackground),
                    Color.green(colorOnBackground),
                    Color.blue(colorOnBackground),
                    1.0f
                )
            )


            aaOptions.chart?.backgroundColor(backgroundStyle.color)
            aaOptions.tooltip?.backgroundColor(
                AAColor.rgbaColor(
                    Color.red(backgroundColor),
                    Color.green(backgroundColor),
                    Color.blue(backgroundColor),
                    1.0f
                )
            )
            aaOptions.title?.style(onBackgroundStyle)
            aaOptions.subtitle?.style(onBackgroundStyle)
            aaOptions.tooltip?.style(onBackgroundStyle)
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
            typeName: String,
            chartSize: Int
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
                    if (chartSize == 1) {
                        """
        function () {
        return '$type: ' +
        this.x +
        '<br/> ' +
        ' $typeName ' +
        this.y
        }
            """.trimIndent()
                    } else {
                        """
function() {
    let wholeContentStr = '<span style=\"' + 'color:gray; font-size:13px\"' + '>◉${type}: ' + this.x + '</span><br/>';
    if (this.points) {
    let length = this.points.length;
    for (let i = 0; i < length; i++) {
        let thisPoint = this.points[i];
        let yValue = thisPoint.y;
        if (yValue != 0) {
            let spanStyleStartStr = '<span style=\"' + 'color: ' + thisPoint.color + '; font-size:13px\"' + '>◉ ';
            let spanStyleEndStr = '</span> <br/>';
            wholeContentStr += spanStyleStartStr + thisPoint.series.name + ': ' + yValue + spanStyleEndStr;

        }
    }
    } else {
        let spanStyleStartStr = '<span style=\"' + 'color: ' + this.point.color + '; font-size:13px\"' + '>◉ ';
        let spanStyleEndStr = '</span> <br/>';
        wholeContentStr += spanStyleStartStr + this.point.series.name + ': ' + this.point.y + spanStyleEndStr;
    }
    return wholeContentStr;
}
        """.trimIndent()
                    }
                }
            }
        }

    }
}