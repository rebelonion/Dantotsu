package ani.dantotsu.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.FragmentStatisticsBinding
import ani.dantotsu.profile.ChartBuilder.Companion.ChartType
import ani.dantotsu.profile.ChartBuilder.Companion.StatType
import ani.dantotsu.profile.ChartBuilder.Companion.MediaType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAYAxis
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class StatsFragment() :
    Fragment() {
    private lateinit var binding: FragmentStatisticsBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var stats: Query.StatisticsResponse? = null
    private var type: MediaType = MediaType.ANIME
    private var statType: StatType = StatType.COUNT
    private lateinit var user: Query.UserProfile
    private lateinit var activity: ProfileActivity

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
        activity = requireActivity() as ProfileActivity
        user = arguments?.getSerializable("user") as Query.UserProfile

        binding.statisticList.adapter = adapter
        binding.statisticList.setHasFixedSize(true)
        binding.statisticList.isNestedScrollingEnabled = false
        binding.statisticList.layoutManager = LinearLayoutManager(requireContext())
        binding.statisticProgressBar.visibility = View.VISIBLE

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
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::binding.isInitialized) {
            binding.root.requestLayout()
        }
        loadStats(type == MediaType.ANIME)
    }

    private fun loadStats(anime: Boolean) {
        binding.statisticProgressBar.visibility = View.VISIBLE
        binding.statisticList.visibility = View.GONE
        adapter.clear()
        loadFormatChart(anime)
        loadScoreChart(anime)
        loadStatusChart(anime)
        loadReleaseYearChart(anime)
        loadStartYearChart(anime)
        loadLengthChart(anime)
        loadGenreChart(anime)
        loadTagChart(anime)
        loadCountryChart(anime)
        loadVoiceActorsChart(anime)
        loadStudioChart(anime)
        loadStaffChart(anime)
        binding.statisticProgressBar.visibility = View.GONE
        binding.statisticList.visibility = View.VISIBLE
    }

    private fun loadFormatChart(anime: Boolean) {
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
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val formatChart = ChartBuilder.buildChart(
                activity,
                ChartType.OneDimensional,
                AAChartType.Pie,
                statType,
                type,
                names,
                values
            )
            adapter.add(ChartItem("Format", formatChart, activity))
        }
    }

    private fun loadStatusChart(anime: Boolean) {
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
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val statusChart = ChartBuilder.buildChart(
                activity,
                ChartType.OneDimensional,
                AAChartType.Funnel,
                statType,
                type,
                names,
                values
            )
            adapter.add(ChartItem("Status", statusChart, activity))
        }
    }

    private fun loadScoreChart(anime: Boolean) {
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
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val scoreChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Column,
                statType,
                type,
                names,
                values,
                xAxisName = "Score",
            )
            adapter.add(ChartItem("Score", scoreChart, activity))
        }
    }

    private fun loadLengthChart(anime: Boolean) {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.lengths?.map { it.length ?: "unknown" }
                ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.lengths?.map { it.length ?: "unknown" }
                ?: emptyList()
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
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val lengthChart = ChartBuilder.buildChart(
                activity,
                ChartType.OneDimensional,
                AAChartType.Pyramid,
                statType,
                type,
                names,
                values,
                xAxisName = "Length",
            )
            adapter.add(ChartItem("Length", lengthChart, activity))
        }
    }

    private fun loadReleaseYearChart(anime: Boolean) {
        val names: List<Number> = if (anime) {
            stats?.data?.user?.statistics?.anime?.releaseYears?.map { it.releaseYear }
                ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.releaseYears?.map { it.releaseYear }
                ?: emptyList()
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
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val releaseYearChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Bubble,
                statType,
                type,
                names,
                values,
                xAxisName = "Year",
            )
            adapter.add(ChartItem("Release Year", releaseYearChart, activity))
        }
    }

    private fun loadStartYearChart(anime: Boolean) {
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
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val startYearChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Bar,
                statType,
                type,
                names,
                values,
                xAxisName = "Year",
            )
            adapter.add(ChartItem("Start Year", startYearChart, activity))
        }
    }

    private fun loadGenreChart(anime: Boolean) {
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
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val genreChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Areaspline,
                statType,
                type,
                names,
                values,
                xAxisName = "Genre",
                polar = true,
                categories = names
            )
            adapter.add(ChartItem("Genre", genreChart, activity))
        }
    }

    private fun loadTagChart(anime: Boolean) {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.tags?.map { it.tag.name } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.tags?.map { it.tag.name } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.tags?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.tags?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.tags?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.tags?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.tags?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.tags?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val min = values.minOf { it.toInt() }
            val max = values.maxOf { it.toInt() }
            val tagChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Areaspline,
                statType,
                type,
                names,
                values,
                xAxisName = "Tag",
                polar = false,
                categories = names,
                scrollPos = 0.0f
            )
            tagChart.yAxis = AAYAxis().min(min).max(max).tickInterval(if (max > 100) 20 else 10)
            adapter.add(ChartItem("Tag", tagChart, activity))
        }
    }

    private fun loadCountryChart(anime: Boolean) {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.countries?.map { it.country } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.countries?.map { it.country } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.countries?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.countries?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.countries?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.countries?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.countries?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.countries?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val countryChart = ChartBuilder.buildChart(
                activity,
                ChartType.OneDimensional,
                AAChartType.Pie,
                statType,
                type,
                names,
                values,
                xAxisName = "Country",
                polar = false,
                categories = names,
                scrollPos = null
            )
            adapter.add(ChartItem("Country", countryChart, activity))
        }
    }

    private fun loadVoiceActorsChart(anime: Boolean) {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.voiceActors?.map { it.voiceActor.name.full?:"unknown" } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.voiceActors?.map { it.voiceActor.name.full?:"unknown" } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.voiceActors?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.voiceActors?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.voiceActors?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.voiceActors?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.voiceActors?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.voiceActors?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val voiceActorsChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Column,
                statType,
                type,
                names,
                values,
                xAxisName = "Voice Actor",
                polar = false,
                categories = names,
                scrollPos = 0.0f
            )
            adapter.add(ChartItem("Voice Actor", voiceActorsChart, activity))
        }
    }

    private fun loadStaffChart(anime: Boolean) {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.staff?.map { it.staff.name.full?:"unknown" } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.staff?.map { it.staff.name.full?:"unknown" } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.staff?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.staff?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.staff?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.staff?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.staff?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.staff?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val staffChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Line,
                statType,
                type,
                names,
                values,
                xAxisName = "Staff",
                polar = false,
                categories = names,
                scrollPos = 0.0f
            )
            adapter.add(ChartItem("Staff", staffChart, activity))
        }
    }

    private fun loadStudioChart(anime: Boolean) {
        val names: List<String> = if (anime) {
            stats?.data?.user?.statistics?.anime?.studios?.map { it.studio.name } ?: emptyList()
        } else {
            stats?.data?.user?.statistics?.manga?.studios?.map { it.studio.name } ?: emptyList()
        }
        val values: List<Number> = if (anime) {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.anime?.studios?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.anime?.studios?.map { it.minutesWatched / 60 }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.anime?.studios?.map { it.meanScore }
            } ?: emptyList()
        } else {
            when (statType) {
                StatType.COUNT -> stats?.data?.user?.statistics?.manga?.studios?.map { it.count }
                StatType.TIME -> stats?.data?.user?.statistics?.manga?.studios?.map { it.chaptersRead }
                StatType.MEAN_SCORE -> stats?.data?.user?.statistics?.manga?.studios?.map { it.meanScore }
            } ?: emptyList()
        }
        if (names.isNotEmpty() || values.isNotEmpty()) {
            val studioChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Spline,
                statType,
                type,
                names.take(15),
                values.take(15),
                xAxisName = "Studio",
                polar = true,
                categories = names,
                scrollPos = null
            )
            adapter.add(ChartItem("Studio", studioChart, activity))
        }
    }

    companion object {
        fun newInstance(user: Query.UserProfile): StatsFragment {
            val args = Bundle().apply {
                putSerializable("user", user)
            }
            return StatsFragment().apply {
                arguments = args
            }
        }
    }
}