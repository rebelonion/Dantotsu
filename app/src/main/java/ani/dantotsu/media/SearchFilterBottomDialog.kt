package ani.dantotsu.media

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.BottomSheetSearchFilterBinding
import ani.dantotsu.databinding.ItemChipBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class SearchFilterBottomDialog : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSearchFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: SearchActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSearchFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var selectedGenres = mutableListOf<String>()
    private var exGenres = mutableListOf<String>()
    private var selectedTags = mutableListOf<String>()
    private var exTags = mutableListOf<String>()
    private fun updateChips() {
        binding.searchFilterGenres.adapter?.notifyDataSetChanged()
        binding.searchFilterTags.adapter?.notifyDataSetChanged()
    }

    private fun startBounceZoomAnimation(view: View? = null) {
        val targetView = view ?: binding.sortByFilter
        val bounceZoomAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_zoom)
        targetView.startAnimation(bounceZoomAnimation)
    }

    private fun setSortByFilterImage() {
        val filterDrawable = when (activity.aniMangaResult.sort) {
            Anilist.sortBy[0] -> R.drawable.ic_round_area_chart_24
            Anilist.sortBy[1] -> R.drawable.ic_round_filter_peak_24
            Anilist.sortBy[2] -> R.drawable.ic_round_star_graph_24
            Anilist.sortBy[3] -> R.drawable.ic_round_new_releases_24
            Anilist.sortBy[4] -> R.drawable.ic_round_filter_list_24
            Anilist.sortBy[5] -> R.drawable.ic_round_filter_list_24_reverse
            Anilist.sortBy[6] -> R.drawable.ic_round_assist_walker_24
            else -> R.drawable.ic_round_filter_alt_24
        }
        binding.sortByFilter.setImageResource(filterDrawable)
    }

    private fun resetSearchFilter() {
        activity.aniMangaResult.sort = null
        binding.sortByFilter.setImageResource(R.drawable.ic_round_filter_alt_24)
        startBounceZoomAnimation(binding.sortByFilter)
        activity.aniMangaResult.countryOfOrigin = null
        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_search_googlefonts)
        startBounceZoomAnimation(binding.countryFilter)

        selectedGenres.clear()
        exGenres.clear()
        selectedTags.clear()
        exTags.clear()
        binding.searchStatus.setText("")
        binding.searchSource.setText("")
        binding.searchFormat.setText("")
        binding.searchSeason.setText("")
        binding.searchYear.setText("")
        binding.searchStatus.clearFocus()
        binding.searchFormat.clearFocus()
        binding.searchSeason.clearFocus()
        binding.searchYear.clearFocus()
        updateChips()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity = requireActivity() as SearchActivity

        selectedGenres = activity.aniMangaResult.genres ?: mutableListOf()
        exGenres = activity.aniMangaResult.excludedGenres ?: mutableListOf()
        selectedTags = activity.aniMangaResult.tags ?: mutableListOf()
        exTags = activity.aniMangaResult.excludedTags ?: mutableListOf()
        setSortByFilterImage()

        binding.resetSearchFilter.setOnClickListener {
            val rotateAnimation =
                ObjectAnimator.ofFloat(binding.resetSearchFilter, "rotation", 180f, 540f)
            rotateAnimation.duration = 500
            rotateAnimation.interpolator = AccelerateDecelerateInterpolator()
            rotateAnimation.start()
            resetSearchFilter()
        }

        binding.resetSearchFilter.setOnLongClickListener {
            val rotateAnimation =
                ObjectAnimator.ofFloat(binding.resetSearchFilter, "rotation", 180f, 540f)
            rotateAnimation.duration = 500
            rotateAnimation.interpolator = AccelerateDecelerateInterpolator()
            rotateAnimation.start()
            val bounceAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_zoom)

            binding.resetSearchFilter.startAnimation(bounceAnimation)
            binding.resetSearchFilter.postDelayed({
                resetSearchFilter()

                CoroutineScope(Dispatchers.Main).launch {
                    activity.aniMangaResult.apply {
                        status =
                            binding.searchStatus.text.toString().replace(" ", "_").ifBlank { null }
                        source =
                            binding.searchSource.text.toString().replace(" ", "_").ifBlank { null }
                        format = binding.searchFormat.text.toString().ifBlank { null }
                        season = binding.searchSeason.text.toString().ifBlank { null }
                        startYear = binding.searchYear.text.toString().toIntOrNull()
                        seasonYear = binding.searchYear.text.toString().toIntOrNull()
                        sort = activity.aniMangaResult.sort
                        genres = selectedGenres
                        tags = selectedTags
                        excludedGenres = exGenres
                        excludedTags = exTags
                    }
                    activity.updateChips.invoke()
                    activity.search()
                    dismiss()
                }
            }, 500)
            true
        }

        binding.sortByFilter.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.sortby_filter_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.sort_by_score -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[0]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_area_chart_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_popular -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[1]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_filter_peak_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_trending -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[2]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_star_graph_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_recent -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[3]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_new_releases_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_a_z -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[4]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_filter_list_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_z_a -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[5]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_filter_list_24_reverse)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_pure_pain -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[6]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_assist_walker_24)
                        startBounceZoomAnimation()
                    }
                }
                true
            }
            popupMenu.show()
        }

        binding.countryFilter.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.country_filter_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.country_global -> {
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_search_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }

                    R.id.country_china -> {
                        activity.aniMangaResult.countryOfOrigin = "CN"
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_china_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }

                    R.id.country_south_korea -> {
                        activity.aniMangaResult.countryOfOrigin = "KR"
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_south_korea_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }

                    R.id.country_japan -> {
                        activity.aniMangaResult.countryOfOrigin = "JP"
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_japan_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }

                    R.id.country_taiwan -> {
                        activity.aniMangaResult.countryOfOrigin = "TW"
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_taiwan_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }
                }
                true
            }
            popupMenu.show()
        }

        binding.searchFilterApply.setOnClickListener {
            activity.aniMangaResult.apply {
                status = binding.searchStatus.text.toString().replace(" ", "_").ifBlank { null }
                source = binding.searchSource.text.toString().replace(" ", "_").ifBlank { null }
                format = binding.searchFormat.text.toString().ifBlank { null }
                season = binding.searchSeason.text.toString().ifBlank { null }
                if (activity.aniMangaResult.type == "ANIME") {
                    seasonYear = binding.searchYear.text.toString().toIntOrNull()
                } else {
                    startYear = binding.searchYear.text.toString().toIntOrNull()
                }
                sort = activity.aniMangaResult.sort
                countryOfOrigin = activity.aniMangaResult.countryOfOrigin
                genres = selectedGenres
                tags = selectedTags
                excludedGenres = exGenres
                excludedTags = exTags
            }
            activity.updateChips.invoke()
            activity.search()
            dismiss()
        }
        binding.searchFilterCancel.setOnClickListener {
            dismiss()
        }
        val format =
            if (activity.aniMangaResult.type == "ANIME") Anilist.animeStatus else Anilist.mangaStatus
        binding.searchStatus.setText(activity.aniMangaResult.status?.replace("_", " "))
        binding.searchStatus.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                format
            )
        )

        binding.searchSource.setText(activity.aniMangaResult.source?.replace("_", " "))
        binding.searchSource.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                Anilist.source.toTypedArray()
            )
        )

        binding.searchFormat.setText(activity.aniMangaResult.format)
        binding.searchFormat.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                (if (activity.aniMangaResult.type == "ANIME") Anilist.animeFormats else Anilist.mangaFormats).toTypedArray()
            )
        )

        if (activity.aniMangaResult.type == "ANIME") {
            binding.searchYear.setText(activity.aniMangaResult.seasonYear?.toString())
        } else {
            binding.searchYear.setText(activity.aniMangaResult.startYear?.toString())
        }
        binding.searchYear.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                (1970 until Calendar.getInstance().get(Calendar.YEAR) + 2).map { it.toString() }
                    .reversed().toTypedArray()
            )
        )

        if (activity.aniMangaResult.type == "MANGA") binding.searchSeasonCont.visibility = GONE
        else {
            binding.searchSeason.setText(activity.aniMangaResult.season)
            binding.searchSeason.setAdapter(
                ArrayAdapter(
                    binding.root.context,
                    R.layout.item_dropdown,
                    Anilist.seasons.toTypedArray()
                )
            )
        }

        binding.searchFilterGenres.adapter = FilterChipAdapter(Anilist.genres ?: listOf()) { chip ->
            val genre = chip.text.toString()
            chip.isChecked = selectedGenres.contains(genre)
            chip.isCloseIconVisible = exGenres.contains(genre)
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chip.isCloseIconVisible = false
                    exGenres.remove(genre)
                    selectedGenres.add(genre)
                } else
                    selectedGenres.remove(genre)
            }
            chip.setOnLongClickListener {
                chip.isChecked = false
                chip.isCloseIconVisible = true
                exGenres.add(genre)
            }
        }
        binding.searchGenresGrid.setOnCheckedChangeListener { _, isChecked ->
            binding.searchFilterGenres.layoutManager =
                if (!isChecked) LinearLayoutManager(binding.root.context, HORIZONTAL, false)
                else GridLayoutManager(binding.root.context, 2, VERTICAL, false)
        }
        binding.searchGenresGrid.isChecked = false

        binding.searchFilterTags.adapter =
            FilterChipAdapter(
                Anilist.tags?.get(activity.aniMangaResult.isAdult) ?: listOf()
            ) { chip ->
                val tag = chip.text.toString()
                chip.isChecked = selectedTags.contains(tag)
                chip.isCloseIconVisible = exTags.contains(tag)
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        chip.isCloseIconVisible = false
                        exTags.remove(tag)
                        selectedTags.add(tag)
                    } else
                        selectedTags.remove(tag)
                }
                chip.setOnLongClickListener {
                    chip.isChecked = false
                    chip.isCloseIconVisible = true
                    exTags.add(tag)
                }
            }
        binding.searchTagsGrid.setOnCheckedChangeListener { _, isChecked ->
            binding.searchFilterTags.layoutManager =
                if (!isChecked) LinearLayoutManager(binding.root.context, HORIZONTAL, false)
                else GridLayoutManager(binding.root.context, 2, VERTICAL, false)
        }
        binding.searchTagsGrid.isChecked = false
    }


    class FilterChipAdapter(val list: List<String>, private val perform: ((Chip) -> Unit)) :
        RecyclerView.Adapter<FilterChipAdapter.SearchChipViewHolder>() {
        inner class SearchChipViewHolder(val binding: ItemChipBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchChipViewHolder {
            val binding =
                ItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SearchChipViewHolder(binding)
        }


        override fun onBindViewHolder(holder: SearchChipViewHolder, position: Int) {
            val title = list[position]
            holder.setIsRecyclable(false)
            holder.binding.root.apply {
                text = title
                isCheckable = true
                perform.invoke(this)
            }
        }

        override fun getItemCount(): Int = list.size
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        fun newInstance() = SearchFilterBottomDialog()
    }

}