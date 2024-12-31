package ani.dantotsu.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.FragmentStatisticsBinding
import ani.dantotsu.profile.ChartBuilder.Companion.ChartPacket
import ani.dantotsu.profile.ChartBuilder.Companion.ChartType
import ani.dantotsu.profile.ChartBuilder.Companion.MediaType
import ani.dantotsu.profile.ChartBuilder.Companion.StatType
import ani.dantotsu.setBaseline
import ani.dantotsu.statusBarHeight
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.xwray.groupie.GroupieAdapter
import eu.kanade.tachiyomi.util.system.getSerializableCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class StatsFragment :
    Fragment() {
    private lateinit var binding: FragmentStatisticsBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var stats: MutableList<Query.StatisticsUser?> = mutableListOf()
    private var type: MediaType = MediaType.ANIME
    private var statType: StatType = StatType.COUNT
    private lateinit var user: Query.UserProfile
    private lateinit var activity: ProfileActivity
    private var loadedFirstTime = false

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

        user = arguments?.getSerializableCompat<Query.UserProfile>("user") as Query.UserProfile

        binding.statisticList.setBaseline(activity.navBar)

        binding.statisticList.adapter = adapter
        binding.statisticList.recycledViewPool.setMaxRecycledViews(0, 0)
        binding.statisticList.isNestedScrollingEnabled = true
        binding.statisticList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.statisticProgressBar.visibility = View.VISIBLE
        binding.compare.visibility = if (user.id == Anilist.userid) View.GONE else View.VISIBLE
        binding.filterContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }

        binding.sourceType.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown,
                MediaType.entries.map { it.name.uppercase(Locale.ROOT).replace("_", " ") }
            )
        )
        binding.sourceFilter.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown,
                StatType.entries.map { it.name.uppercase(Locale.ROOT).replace("_", " ") }
            )
        )

        binding.compare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                activity.lifecycleScope.launch {
                    if (Anilist.userid != null) {
                        withContext(Dispatchers.Main) {
                            binding.statisticProgressBar.visibility = View.VISIBLE
                            binding.statisticList.visibility = View.GONE
                        }
                        val userStats =
                            Anilist.query.getUserStatistics(Anilist.userid!!)?.data?.user
                        if (userStats != null) {
                            stats.add(userStats)
                            withContext(Dispatchers.Main) {
                                loadStats(type == MediaType.ANIME)
                                binding.statisticProgressBar.visibility = View.GONE
                                binding.statisticList.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            } else {
                stats.removeAll(
                    stats.filter { it?.id == Anilist.userid }.toSet()
                )
                loadStats(type == MediaType.ANIME)
            }
        }

        binding.filterContainer.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        binding.statisticList.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (this::binding.isInitialized) {
            binding.statisticList.visibility = View.VISIBLE
            binding.statisticList.setBaseline(activity.navBar)
            binding.root.requestLayout()
            if (!loadedFirstTime) {
                activity.lifecycleScope.launch {
                    stats.clear()
                    stats.add(Anilist.query.getUserStatistics(user.id)?.data?.user)
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
                loadedFirstTime = true
            }
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
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.formats?.map { it.format } ?: emptyList()
            } else {
                stat?.statistics?.manga?.formats?.map { it.format } ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.formats?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.formats?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.formats?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.formats?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.formats?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.formats?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() && values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val formatChart = ChartBuilder.buildChart(
                activity,
                ChartType.OneDimensional,
                AAChartType.Pie,
                statType,
                type,
                chartPackets,
                xAxisName = "Format",
            )
            adapter.add(ChartItem("Format", formatChart, activity))
        }
    }

    private fun loadStatusChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.statuses?.map { it.status } ?: emptyList()
            } else {
                stat?.statistics?.manga?.statuses?.map { it.status } ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.statuses?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.statuses?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.statuses?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.statuses?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.statuses?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.statuses?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() && values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val statusChart = ChartBuilder.buildChart(
                activity,
                ChartType.OneDimensional,
                AAChartType.Funnel,
                statType,
                type,
                chartPackets,
                xAxisName = "Status",
            )
            adapter.add(ChartItem("Status", statusChart, activity))
        }
    }

    private fun loadScoreChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<Int> = if (anime) {
                stat?.statistics?.anime?.scores?.map {
                    convertScore(
                        it.score,
                        stat.mediaListOptions.scoreFormat.toString()
                    )
                } ?: emptyList()
            } else {
                stat?.statistics?.manga?.scores?.map {
                    convertScore(
                        it.score,
                        stat.mediaListOptions.scoreFormat.toString()
                    )
                } ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.scores?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.scores?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.scores?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.scores?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.scores?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.scores?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val scoreChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Column,
                statType,
                type,
                chartPackets,
                xAxisName = "Score",
            )
            adapter.add(ChartItem("Score", scoreChart, activity))
        }
    }

    private fun loadLengthChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.lengths?.map { it.length ?: "unknown" }
                    ?: emptyList()
            } else {
                stat?.statistics?.manga?.lengths?.map { it.length ?: "unknown" }
                    ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.lengths?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.lengths?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.lengths?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.lengths?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.lengths?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.lengths?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val lengthChart = ChartBuilder.buildChart(
                activity,
                ChartType.OneDimensional,
                AAChartType.Pyramid,
                statType,
                type,
                chartPackets,
                xAxisName = "Length",
            )
            adapter.add(ChartItem("Length", lengthChart, activity))
        }
    }

    private fun loadReleaseYearChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<Number> = if (anime) {
                stat?.statistics?.anime?.releaseYears?.map { it.releaseYear }
                    ?: emptyList()
            } else {
                stat?.statistics?.manga?.releaseYears?.map { it.releaseYear }
                    ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.releaseYears?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.releaseYears?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.releaseYears?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.releaseYears?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.releaseYears?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.releaseYears?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val releaseYearChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Bubble,
                statType,
                type,
                chartPackets,
                xAxisName = "Year",
                scrollPos = 0.0f
            )
            adapter.add(ChartItem("Release Year", releaseYearChart, activity))
        }
    }

    private fun loadStartYearChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<Number> = if (anime) {
                stat?.statistics?.anime?.startYears?.map { it.startYear } ?: emptyList()
            } else {
                stat?.statistics?.manga?.startYears?.map { it.startYear } ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.startYears?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.startYears?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.startYears?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.startYears?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.startYears?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.startYears?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val startYearChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Bar,
                statType,
                type,
                chartPackets,
                xAxisName = "Year",
            )
            adapter.add(ChartItem("Start Year", startYearChart, activity))
        }
    }

    private fun loadGenreChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.genres?.map { it.genre } ?: emptyList()
            } else {
                stat?.statistics?.manga?.genres?.map { it.genre } ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.genres?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.genres?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.genres?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.genres?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.genres?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.genres?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val referenceNames = chartPackets.first().names.map { it.toString() }
            val standardizedPackets = chartPackets.map { packet ->
                val valuesMap = packet.names.map { it.toString() }.zip(packet.statData).toMap()
                val standardizedValues = referenceNames.map { name ->
                    valuesMap[name] ?: 0
                }

                // Create a new ChartPacket with standardized names and values.
                ChartPacket(packet.username, referenceNames, standardizedValues)
            }.toMutableList()
            chartPackets.clear()
            chartPackets.addAll(standardizedPackets)
            @Suppress("UNCHECKED_CAST")
            val genreChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Areaspline,
                statType,
                type,
                chartPackets,
                xAxisName = "Genre",
                polar = true,
                passedCategories = chartPackets[0].names as List<String>,
                normalize = true
            )
            adapter.add(ChartItem("Genre", genreChart, activity))
        }
    }

    private fun loadTagChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.tags?.map { it.tag.name } ?: emptyList()
            } else {
                stat?.statistics?.manga?.tags?.map { it.tag.name } ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.tags?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.tags?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.tags?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.tags?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.tags?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.tags?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val referenceNames = chartPackets.first().names.map { it.toString() }
            val standardizedPackets = chartPackets.map { packet ->
                val valuesMap = packet.names.map { it.toString() }.zip(packet.statData).toMap()
                val standardizedValues = referenceNames.map { name ->
                    valuesMap[name] ?: 0
                }

                // Create a new ChartPacket with standardized names and values.
                ChartPacket(packet.username, referenceNames, standardizedValues)
            }.toMutableList()
            chartPackets.clear()
            chartPackets.addAll(standardizedPackets)
            @Suppress("UNCHECKED_CAST")
            val tagChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Areaspline,
                statType,
                type,
                chartPackets,
                xAxisName = "Tag",
                polar = false,
                passedCategories = chartPackets[0].names as List<String>,
                scrollPos = 0.0f
            )
            adapter.add(ChartItem("Tag", tagChart, activity))
        }
    }

    private fun loadCountryChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.countries?.map { it.country } ?: emptyList()
            } else {
                stat?.statistics?.manga?.countries?.map { it.country } ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.countries?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.countries?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.countries?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.countries?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.countries?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.countries?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val referenceNames = chartPackets.first().names.map { it.toString() }
            val standardizedPackets = chartPackets.map { packet ->
                val valuesMap = packet.names.map { it.toString() }.zip(packet.statData).toMap()
                val standardizedValues = referenceNames.map { name ->
                    valuesMap[name] ?: 0
                }

                // Create a new ChartPacket with standardized names and values.
                ChartPacket(packet.username, referenceNames, standardizedValues)
            }.toMutableList()
            chartPackets.clear()
            chartPackets.addAll(standardizedPackets)
            @Suppress("UNCHECKED_CAST")
            val countryChart = ChartBuilder.buildChart(
                activity,
                ChartType.OneDimensional,
                AAChartType.Pie,
                statType,
                type,
                chartPackets,
                xAxisName = "Country",
                polar = false,
                passedCategories = chartPackets[0].names as List<String>,
                scrollPos = null
            )
            adapter.add(ChartItem("Country", countryChart, activity))
        }
    }

    private fun loadVoiceActorsChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.voiceActors?.map { it.voiceActor.name.full ?: "unknown" }
                    ?: emptyList()
            } else {
                stat?.statistics?.manga?.voiceActors?.map { it.voiceActor.name.full ?: "unknown" }
                    ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.voiceActors?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.voiceActors?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.voiceActors?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.voiceActors?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.voiceActors?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.voiceActors?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val referenceNames = chartPackets.first().names.map { it.toString() }
            val standardizedPackets = chartPackets.map { packet ->
                val valuesMap = packet.names.map { it.toString() }.zip(packet.statData).toMap()
                val standardizedValues = referenceNames.map { name ->
                    valuesMap[name] ?: 0
                }

                // Create a new ChartPacket with standardized names and values.
                ChartPacket(packet.username, referenceNames, standardizedValues)
            }.toMutableList()
            chartPackets.clear()
            chartPackets.addAll(standardizedPackets)
            @Suppress("UNCHECKED_CAST")
            val voiceActorsChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Column,
                statType,
                type,
                chartPackets,
                xAxisName = "Voice Actor",
                polar = false,
                passedCategories = chartPackets[0].names as List<String>,
                scrollPos = 0.0f
            )
            adapter.add(ChartItem("Voice Actor", voiceActorsChart, activity))
        }
    }

    private fun loadStudioChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.studios?.map { it.studio.name } ?: emptyList()
            } else {
                stat?.statistics?.manga?.studios?.map { it.studio.name } ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.studios?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.studios?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.studios?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.studios?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.studios?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.studios?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val referenceNames = chartPackets.first().names.map { it.toString() }
            val standardizedPackets = chartPackets.map { packet ->
                val valuesMap = packet.names.map { it.toString() }.zip(packet.statData).toMap()
                val standardizedValues = referenceNames.map { name ->
                    valuesMap[name] ?: 0
                }

                // Create a new ChartPacket with standardized names and values.
                ChartPacket(packet.username, referenceNames, standardizedValues)
            }.toMutableList()
            chartPackets.clear()
            chartPackets.addAll(standardizedPackets)
            @Suppress("UNCHECKED_CAST")
            val studioChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Spline,
                statType,
                type,
                chartPackets,
                xAxisName = "Studio",
                polar = true,
                passedCategories = chartPackets[0].names as List<String>,
                scrollPos = null,
                normalize = true
            )
            adapter.add(ChartItem("Studio", studioChart, activity))
        }
    }

    private fun loadStaffChart(anime: Boolean) {
        val chartPackets = mutableListOf<ChartPacket>()
        stats.forEach { stat ->
            val names: List<String> = if (anime) {
                stat?.statistics?.anime?.staff?.map { it.staff.name.full ?: "unknown" }
                    ?: emptyList()
            } else {
                stat?.statistics?.manga?.staff?.map { it.staff.name.full ?: "unknown" }
                    ?: emptyList()
            }
            val values: List<Number> = if (anime) {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.anime?.staff?.map { it.count }
                    StatType.TIME -> stat?.statistics?.anime?.staff?.map { it.minutesWatched / 60 }
                    StatType.AVG_SCORE -> stat?.statistics?.anime?.staff?.map { it.meanScore }
                } ?: emptyList()
            } else {
                when (statType) {
                    StatType.COUNT -> stat?.statistics?.manga?.staff?.map { it.count }
                    StatType.TIME -> stat?.statistics?.manga?.staff?.map { it.chaptersRead }
                    StatType.AVG_SCORE -> stat?.statistics?.manga?.staff?.map { it.meanScore }
                } ?: emptyList()
            }
            if (names.isNotEmpty() || values.isNotEmpty()) {
                chartPackets.add(ChartPacket(stat?.name ?: "Unknown", names, values))
            }
        }
        if (chartPackets.isNotEmpty()) {
            val referenceNames = chartPackets.first().names.map { it.toString() }
            val standardizedPackets = chartPackets.map { packet ->
                val valuesMap = packet.names.map { it.toString() }.zip(packet.statData).toMap()
                val standardizedValues = referenceNames.map { name ->
                    valuesMap[name] ?: 0
                }

                // Create a new ChartPacket with standardized names and values.
                ChartPacket(packet.username, referenceNames, standardizedValues)
            }.toMutableList()
            chartPackets.clear()
            chartPackets.addAll(standardizedPackets)
            @Suppress("UNCHECKED_CAST")
            val staffChart = ChartBuilder.buildChart(
                activity,
                ChartType.TwoDimensional,
                AAChartType.Line,
                statType,
                type,
                chartPackets,
                xAxisName = "Staff",
                polar = false,
                passedCategories = chartPackets[0].names as List<String>,
                scrollPos = 0.0f
            )
            adapter.add(ChartItem("Staff", staffChart, activity))
        }
    }

    private fun convertScore(score: Int, type: String?): Int {
        return when (type) {
            "POINT_100" -> score
            "POINT_10_DECIMAL" -> score
            "POINT_10" -> score * 10
            "POINT_5" -> score * 20
            "POINT_3" -> score * 33
            else -> score
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