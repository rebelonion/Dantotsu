package ani.dantotsu.profile

import android.os.Bundle
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
                    updateStats()
                }
                binding.sourceFilter.setOnItemClickListener { _, _, i, _ ->
                    statType = StatType.entries.toTypedArray()[i]
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
            val aaOptions = buildOptions(formatChartModel)
            binding.formatChartView.aa_drawChartWithChartOptions(aaOptions)
        }
    }

    private fun loadMangaStats() {
        val formatChartModel = getFormatChartModel(false)
        if (formatChartModel != null) {
            val aaOptions = buildOptions(formatChartModel)
            binding.formatChartView.aa_drawChartWithChartOptions(aaOptions)
        }
    }

    private fun buildOptions(aaChartModel: AAChartModel): AAOptions {
        val aaOptions = aaChartModel.aa_toAAOptions()
        aaOptions.tooltip?.apply {
            backgroundColor(AAGradientColor.PurpleLake)
                .style(AAStyle.style(AAColor.White))
        }
        aaOptions.chart?.zoomType = "xy"
        aaOptions.chart?.pinchType = "xy"
        aaOptions.legend?.apply {
            enabled(true)
                .verticalAlign(AAChartVerticalAlignType.Top)
                .layout(AAChartLayoutType.Vertical)
                .align(AAChartAlignType.Right)
                .itemMarginTop(10f)
                .labelFormat = "{name}: {y}"
        }
        aaOptions.plotOptions?.series?.connectNulls(true)
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
        return AAChartModel()
            .chartType(AAChartType.Pie)
            .title("Format")
            .subtitle(statType.name.lowercase(Locale.ROOT))
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(true)
            .series(getElements(names, values, StatType.COUNT))
    }

    enum class StatType {
        COUNT, TIME, MEAN_SCORE
    }

    enum class MediaType {
        ANIME, MANGA
    }

    private fun getElements(names: List<String>, statData: List<Number>, type: StatType): Array<Any> {
        val statDataElements = mutableListOf<AADataElement>()
        for (i in statData.indices) {
            statDataElements.add(AADataElement().name(names[i]).y(statData[i]))
        }
        return arrayOf(
            AASeriesElement().name("Count").data(statDataElements.toTypedArray()),
        )
    }
}