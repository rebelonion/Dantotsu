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
    private var selectedCountry: String? = null

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
        val filterDrawable = when (activity.result.sort) {
            Anilist.sortBy[0] -> R.drawable.ic_round_area_chart_24
            Anilist.sortBy[1] -> R.drawable.ic_round_filter_peak_24
            Anilist.sortBy[2] -> R.drawable.ic_round_star_graph_24
            Anilist.sortBy[3] -> R.drawable.ic_round_filter_list_24
            Anilist.sortBy[4] -> R.drawable.ic_round_filter_list_24_reverse
            Anilist.sortBy[5] -> R.drawable.ic_round_assist_walker_24
            else -> R.drawable.ic_round_filter_alt_24
        }
        binding.sortByFilter.setImageResource(filterDrawable)
    }

    private fun resetSearchFilter() {
        activity.result.sort = null
        binding.sortByFilter.setImageResource(R.drawable.ic_round_filter_alt_24)
        startBounceZoomAnimation(binding.sortByFilter)
        selectedCountry = null
        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_search_googlefonts)
        startBounceZoomAnimation(binding.countryFilter)

        selectedGenres.clear()
        exGenres.clear()
        selectedTags.clear()
        exTags.clear()
        binding.searchStatus.setText("")
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

        selectedGenres = activity.result.genres ?: mutableListOf()
        exGenres = activity.result.excludedGenres ?: mutableListOf()
        selectedTags = activity.result.tags ?: mutableListOf()
        exTags = activity.result.excludedTags ?: mutableListOf()
        setSortByFilterImage()

        binding.resetSearchFilter.setOnClickListener {
            val rotateAnimation = ObjectAnimator.ofFloat(binding.resetSearchFilter, "rotation", 180f, 540f)
            rotateAnimation.duration = 500
            rotateAnimation.interpolator = AccelerateDecelerateInterpolator()
            rotateAnimation.start()
            resetSearchFilter()
        }

        binding.resetSearchFilter.setOnLongClickListener {
            val rotateAnimation = ObjectAnimator.ofFloat(binding.resetSearchFilter, "rotation", 180f, 540f)
            rotateAnimation.duration = 500
            rotateAnimation.interpolator = AccelerateDecelerateInterpolator()
            rotateAnimation.start()
            val bounceAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_zoom)

            binding.resetSearchFilter.startAnimation(bounceAnimation)
            binding.resetSearchFilter.postDelayed({
                resetSearchFilter()

                CoroutineScope(Dispatchers.Main).launch {
                    activity.result.apply {
                        status = binding.searchStatus.text.toString().ifBlank { null }
                        format = binding.searchFormat.text.toString().ifBlank { null }
                        season = binding.searchSeason.text.toString().ifBlank { null }
                        seasonYear = binding.searchYear.text.toString().toIntOrNull()
                        sort = activity.result.sort
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

        binding.sortByFilter.setOnClickListener { view ->
            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.sortby_filter_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.sort_by_score -> {
                        activity.result.sort = Anilist.sortBy[0]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_area_chart_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_popular -> {
                        activity.result.sort = Anilist.sortBy[1]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_filter_peak_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_trending -> {
                        activity.result.sort = Anilist.sortBy[2]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_star_graph_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_a_z -> {
                        activity.result.sort = Anilist.sortBy[3]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_filter_list_24)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_z_a -> {
                        activity.result.sort = Anilist.sortBy[4]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_filter_list_24_reverse)
                        startBounceZoomAnimation()
                    }

                    R.id.sort_by_pure_pain -> {
                        activity.result.sort = Anilist.sortBy[5]
                        binding.sortByFilter.setImageResource(R.drawable.ic_round_assist_walker_24)
                        startBounceZoomAnimation()
                    }
                }
                true
            }
            popupMenu.show()
        }

        binding.countryFilter.setOnClickListener { view ->
            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.country_filter_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.country_china -> {
                        selectedCountry = "China"
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_china_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }
                    R.id.country_south_korea -> {
                        selectedCountry = "South Korea"
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_south_korea_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }
                    R.id.country_japan -> {
                        selectedCountry = "Japan"
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_japan_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }
                    R.id.country_taiwan -> {
                        selectedCountry = "Taiwan"
                        binding.countryFilter.setImageResource(R.drawable.ic_round_globe_taiwan_googlefonts)
                        startBounceZoomAnimation(binding.countryFilter)
                    }
                }
                true
            }
            popupMenu.show()
        }

        binding.searchFilterApply.setOnClickListener {
            activity.result.apply {
                status = binding.searchStatus.text.toString().ifBlank { null }
                format = binding.searchFormat.text.toString().ifBlank { null }
                season = binding.searchSeason.text.toString().ifBlank { null }
                seasonYear = binding.searchYear.text.toString().toIntOrNull()
                sort = activity.result.sort
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

        binding.searchStatus.setText(activity.result.status)
        binding.searchStatus.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                (if (activity.result.type == "ANIME") Anilist.animeStatus else Anilist.mangaStatus).toTypedArray()
            )
        )

        binding.searchFormat.setText(activity.result.format)
        binding.searchFormat.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                (if (activity.result.type == "ANIME") Anilist.animeFormats else Anilist.mangaFormats).toTypedArray()
            )
        )

        if (activity.result.type == "MANGA") binding.searchSeasonYearCont.visibility = GONE
        else {
            binding.searchSeason.setText(activity.result.season)
            binding.searchSeason.setAdapter(
                ArrayAdapter(
                    binding.root.context,
                    R.layout.item_dropdown,
                    Anilist.seasons.toTypedArray()
                )
            )

            binding.searchYear.setText(activity.result.seasonYear?.toString())
            binding.searchYear.setAdapter(
                ArrayAdapter(
                    binding.root.context,
                    R.layout.item_dropdown,
                    (1970 until Calendar.getInstance().get(Calendar.YEAR) + 2).map { it.toString() }
                        .reversed().toTypedArray()
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
            FilterChipAdapter(Anilist.tags?.get(activity.result.isAdult) ?: listOf()) { chip ->
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