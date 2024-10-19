package ani.dantotsu.profile

import android.content.Context
import android.graphics.Color
import ani.dantotsu.getThemeColor
import ani.dantotsu.util.ColorEditor
import com.github.aachartmodel.aainfographics.aachartcreator.*
import com.github.aachartmodel.aainfographics.aaoptionsmodel.*
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
            val primaryColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
            var chartType = passedChartType
            var aaChartType = passedAaChartType
            var categories = passedCategories
            
            // Convert to 2D if needed
            if (chartType == ChartType.OneDimensional && chartPackets.size != 1) {
                chartType = ChartType.TwoDimensional
                aaChartType = AAChartType.Column
                categories = chartPackets[0].names.map { it.toString() }
            }

            // Normalize data if required
            if (normalize && chartPackets.size > 1) {
                chartPackets.forEach {
                    it.statData = normalizeData(it.statData)
                }
            }

            val namesMax = chartPackets.maxOf { it.names.size }
            val palette = ColorEditor.generateColorPalette(primaryColor, namesMax)

            val aaChartModel = when (chartType) {
                ChartType.OneDimensional -> buildOneDimensionalChart(
                    aaChartType, statType, mediaType, normalize, chartPackets, xAxisTickInterval,
                    categories, primaryColor, palette
                )

                ChartType.TwoDimensional -> buildTwoDimensionalChart(
                    aaChartType, statType, mediaType, normalize, chartPackets, xAxisTickInterval,
                    categories, primaryColor, palette
                )
            }

            val aaOptions = aaChartModel.aa_toAAOptions()
            aaOptions.chart?.polar = polar

            // Apply tooltip formatting
            aaOptions.tooltip?.apply {
                formatter(
                    getToolTipFunction(
                        chartType,
                        getTypeName(statType, mediaType),
                        chartPackets.size
                    )
                )
                if (chartPackets.size > 1) useHTML(true)
            }

            // Other options
            aaOptions.legend?.enabled(true)?.labelFormat = "{name}"
            aaOptions.plotOptions?.series?.connectNulls(false)?.stacking(AAChartStackingType.False)
            aaOptions.chart?.panning = true

            // Handle scrolling if applicable
            scrollPos?.let {
                aaOptions.chart?.scrollablePlotArea(
                    AAScrollablePlotArea().scrollPositionX(scrollPos.toInt())
                )?.minWidth(
                    (context.resources.displayMetrics.widthPixels.toFloat() / context.resources.displayMetrics.density) * (namesMax.toFloat() / 18.0f)
                )
            }

            // Setting min and max for Y-axis
            setYAxis(aaOptions, chartPackets)

            setColors(aaOptions, context)

            return aaOptions
        }

        private fun buildOneDimensionalChart(
            aaChartType: AAChartType,
            statType: StatType,
            mediaType: MediaType,
            normalize: Boolean,
            chartPackets: List<ChartPacket>,
            xAxisTickInterval: Int?,
            categories: List<String>?,
            primaryColor: Int,
            palette: List<Int>
        ): AAChartModel {
            val chart = AAChartModel()
                .chartType(aaChartType)
                .subtitle(
                    getTypeName(statType, mediaType) + if (normalize && chartPackets.size > 1) " (Normalized)" else ""
                )
                .zoomType(AAChartZoomType.None)
                .dataLabelsEnabled(true)

            val elements = chartPackets.mapIndexed { index, chartPacket ->
                AASeriesElement()
                    .name(chartPacket.username)
                    .data(
                        get1DElements(
                            chartPacket.names,
                            chartPacket.statData,
                            palette
                        )
                    )
                    .color(if (index == 0) primaryColor else ColorEditor.oppositeColor(primaryColor))
            }.toTypedArray()

            chart.series(elements)
            xAxisTickInterval?.let { chart.xAxisTickInterval(it) }
            categories?.let { chart.categories(it.toTypedArray()) }

            return chart
        }

        private fun buildTwoDimensionalChart(
            aaChartType: AAChartType,
            statType: StatType,
            mediaType: MediaType,
            normalize: Boolean,
            chartPackets: List<ChartPacket>,
            xAxisTickInterval: Int?,
            categories: List<String>?,
            primaryColor: Int,
            palette: List<Int>
        ): AAChartModel {
            val hexColorsArray = palette.map { String.format("#%06X", 0xFFFFFF and it) }.toTypedArray()

            val chart = AAChartModel()
                .chartType(aaChartType)
                .subtitle(
                    getTypeName(statType, mediaType) + if (normalize && chartPackets.size > 1) " (Normalized)" else ""
                )
                .zoomType(AAChartZoomType.None)
                .dataLabelsEnabled(false)
                .yAxisTitle(getTypeName(statType, mediaType))

            if (chartPackets.size == 1) {
                chart.colorsTheme(hexColorsArray)
            }

            val elements = chartPackets.mapIndexed { index, chartPacket ->
                get2DElements(
                    chartPacket.names,
                    chartPacket.statData,
                    chartPackets.size == 1
                ).name(chartPacket.username)
                    .color(
                        AAColor.rgbaColor(
                            Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor), 0.9f
                        )
                    )
            }.toTypedArray()

            chart.series(elements)
            xAxisTickInterval?.let { chart.xAxisTickInterval(it) }
            categories?.let { chart.categories(it.toTypedArray()) }

            return chart
        }

        private fun setYAxis(aaOptions: AAOptions, chartPackets: List<ChartPacket>) {
            val allStatData = chartPackets.flatMap { it.statData }
            val min = (allStatData.minOfOrNull { it.toDouble() } ?: 0.0) - 1.0
            val coercedMin = min.coerceAtLeast(0.0)
            val max = allStatData.maxOfOrNull { it.toDouble() } ?: 0.0
            val tickInterval = when (max) {
                in 0.0..10.0 -> 1.0
                in 10.0..30.0 -> 5.0
                in 30.0..100.0 -> 10.0
                in 100.0..1000.0 -> 100.0
                in 1000.0..10000.0 -> 1000.0
                else -> 10000.0
            }

            val aaYaxis = AAYAxis().min(coercedMin).max(max).tickInterval(tickInterval)
            aaOptions.yAxis(aaYaxis)
        }

        private fun get1DElements(
            names: List<Any>,
            statData: List<Number>,
            colors: List<Int>
        ): Array<Any> {
            val statDataElements = names.mapIndexed { i, name ->
                AADataElement()
                    .y(statData[i])
                    .color(
                        AAColor.rgbaColor(
                            Color.red(colors[i]),
                            Color.green(colors[i]),
                            Color.blue(colors[i]),
                            0.9f
                        )
                    ).apply {
                        if (name is Number) {
                            x(name)
                        } else {
                            x(i)
                            this.name(name.toString())
                        }
                    }
            }

            return statDataElements.toTypedArray()
        }

        private fun get2DElements(
            names: List<Any>,
            statData: List<Any>,
            colorByPoint: Boolean
        ): AASeriesElement {
            val statValues = names.mapIndexed { i, name ->
                arrayOf(name, statData[i], statData[i])
            }

            return AASeriesElement()
                .data(statValues.toTypedArray())
                .dataLabels(AADataLabels().enabled(false))
                .colorByPoint(colorByPoint)
        }

        private fun normalizeData(list: List<Number>): List<Number> {
            val max = list.maxOf { it.toDouble() }
            return list.map {
                it.toDouble() / max
            }
        }

        private fun getToolTipFunction(
            chartType: ChartType,
            statType: String,
            packetSize: Int
        ): String {
            return when (chartType) {
                ChartType.OneDimensional -> if (packetSize == 1) {
                    """function() { return this.category + "<br>" + " $statType : " + this.y.toFixed(1); }"""
                } else {
                    """function() { return this.series.name + "<br>" + this.category + "<br>" + "$statType : " + this.y.toFixed(1); }"""
                }

                ChartType.TwoDimensional -> """function() { return this.series.name + "<br>" + this.point.name + "<br>" + "$statType : " + this.point.y.toFixed(1); }"""
            }
        }

        private fun setColors(aaOptions: AAOptions, context: Context) {
            val primaryColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
            val primaryDarkColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimaryVariant)

            aaOptions.chart?.backgroundColor = primaryColor
            aaOptions.title?.style?.color = primaryDarkColor
        }

        private fun getTypeName(statType: StatType, mediaType: MediaType): String {
            return when (statType) {
                StatType.COUNT -> if (mediaType == MediaType.ANIME) "Anime Count" else "Manga Count"
                StatType.TIME -> if (mediaType == MediaType.ANIME) "Time (Hours)" else "Time (Chapters)"
                StatType.AVG_SCORE -> if (mediaType == MediaType.ANIME) "Average Anime Score" else "Average Manga Score"
            }
        }
    }
}
