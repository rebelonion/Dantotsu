package ani.dantotsu.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity = requireActivity() as SearchActivity

        selectedGenres = activity.result.genres ?: mutableListOf()
        exGenres = activity.result.excludedGenres ?: mutableListOf()
        selectedTags = activity.result.tags ?: mutableListOf()
        exTags = activity.result.excludedTags ?: mutableListOf()

        binding.searchFilterApply.setOnClickListener {
            activity.result.apply {
                format = binding.searchFormat.text.toString().ifBlank { null }
                sort = binding.searchSortBy.text.toString().ifBlank { null }
                    ?.let { Anilist.sortBy[resources.getStringArray(R.array.sort_by).indexOf(it)] }
                season = binding.searchSeason.text.toString().ifBlank { null }
                seasonYear = binding.searchYear.text.toString().toIntOrNull()
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

        binding.searchSortBy.setText(activity.result.sort?.let {
            resources.getStringArray(R.array.sort_by)[Anilist.sortBy.indexOf(it)]
        })
        binding.searchSortBy.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                resources.getStringArray(R.array.sort_by)
            )
        )

        binding.searchFormat.setText(activity.result.format)
        binding.searchFormat.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                (if (activity.result.type == "ANIME") Anilist.anime_formats else Anilist.manga_formats).toTypedArray()
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