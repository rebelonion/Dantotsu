package ani.dantotsu.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.FragmentStatisticsBinding
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartAlignType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartLayoutType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartStackingType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartVerticalAlignType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartZoomType
import com.github.aachartmodel.aainfographics.aachartcreator.AADataElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.aa_toAAOptions
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAChart
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AALang
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAPosition
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAScrollablePlotArea
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.github.aachartmodel.aainfographics.aatools.AAColor
import com.github.aachartmodel.aainfographics.aatools.AAGradientColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar

class StatsFragment(private val user: Query.UserProfile, private val activity: ProfileActivity) :
    Fragment() {
    private lateinit var binding: FragmentStatisticsBinding
    private var selected: Int = 0
    private lateinit var tabLayout: AnimatedBottomBar
    private var stats: Query.StatisticsResponse? = null

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
        tabLayout = binding.typeTab
        val animeTab = tabLayout.createTab(R.drawable.ic_round_movie_filter_24, "Anime")
        val mangaTab = tabLayout.createTab(R.drawable.ic_round_menu_book_24, "Manga")
        tabLayout.addTab(animeTab)
        tabLayout.addTab(mangaTab)

        tabLayout.visibility = View.GONE
        activity.lifecycleScope.launch {
            stats = Anilist.query.getUserStatistics(user.id)
            withContext(Dispatchers.Main) {
                tabLayout.visibility = View.VISIBLE
                tabLayout.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
                    override fun onTabSelected(
                        lastIndex: Int,
                        lastTab: AnimatedBottomBar.Tab?,
                        newIndex: Int,
                        newTab: AnimatedBottomBar.Tab
                    ) {
                        selected = newIndex
                        when (newIndex) {
                            0 -> loadAnimeStats()
                            1 -> loadMangaStats()
                        }
                    }
                })
                tabLayout.selectTabAt(selected)
                loadAnimeStats()
            }
        }
    }

    override fun onResume() {
        if (this::tabLayout.isInitialized) {
            tabLayout.selectTabAt(selected)
        }
        super.onResume()
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
        val fotmatTypes: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.formats?.map { it.format } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.countries?.map { it.country } ?: emptyList()
        }
        val formatCount: List<Int> = if (anime) {
            stats?.data?.user?.statistics?.anime?.formats?.map { it.count } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.countries?.map { it.count } ?: emptyList()
        }
        if (fotmatTypes.isEmpty() || formatCount.isEmpty())
            return null
        return AAChartModel()
            .chartType(AAChartType.Pie)
            .title("Format")
            .zoomType(AAChartZoomType.XY)
            .dataLabelsEnabled(true)
            .series(getElements(fotmatTypes, formatCount))
    }

    private fun getElements(types: List<String>, counts: List<Int>): Array<Any> {
        val elements = AASeriesElement()
        val dataElements = mutableListOf<AADataElement>()
        for (i in types.indices) {
            dataElements.add(AADataElement().name(types[i]).y(counts[i]))
        }
        return arrayOf(elements.data(dataElements.toTypedArray()))
    }
}