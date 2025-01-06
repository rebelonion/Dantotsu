package ani.dantotsu.media

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import ani.dantotsu.App.Companion.context
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistSearch.SearchType
import ani.dantotsu.databinding.ItemChipBinding
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.imagesearch.ImageSearchActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
import com.google.android.material.checkbox.MaterialCheckBox.STATE_INDETERMINATE
import com.google.android.material.checkbox.MaterialCheckBox.STATE_UNCHECKED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchAdapter(private val activity: SearchActivity, private val type: SearchType) :
    HeaderInterface() {

    private fun updateFilterTextViewDrawable() {
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
        binding.filterTextView.setCompoundDrawablesWithIntrinsicBounds(filterDrawable, 0, 0, 0)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: SearchHeaderViewHolder, position: Int) {
        binding = holder.binding

        searchHistoryAdapter = SearchHistoryAdapter(type) {
            binding.searchBarText.setText(it)
            binding.searchBarText.setSelection(it.length)
        }
        binding.searchHistoryList.layoutManager = LinearLayoutManager(binding.root.context)
        binding.searchHistoryList.adapter = searchHistoryAdapter

        val imm: InputMethodManager =
            activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager

        if (activity.searchType != SearchType.MANGA && activity.searchType != SearchType.ANIME) {
            throw IllegalArgumentException("Invalid search type (wrong adapter)")
        }

        when (activity.style) {
            0 -> {
                binding.searchResultGrid.alpha = 1f
                binding.searchResultList.alpha = 0.33f
            }

            1 -> {
                binding.searchResultList.alpha = 1f
                binding.searchResultGrid.alpha = 0.33f
            }
        }

        binding.searchBar.hint = activity.aniMangaResult.type
        if (PrefManager.getVal(PrefName.Incognito)) {
            val startIconDrawableRes = R.drawable.ic_incognito_24
            val startIconDrawable: Drawable? =
                context?.let { AppCompatResources.getDrawable(it, startIconDrawableRes) }
            binding.searchBar.startIconDrawable = startIconDrawable
        }

        var adult = activity.aniMangaResult.isAdult
        var listOnly = activity.aniMangaResult.onList

        binding.searchBarText.removeTextChangedListener(textWatcher)
        binding.searchBarText.setText(activity.aniMangaResult.search)

        binding.searchAdultCheck.isChecked = adult
        binding.searchList.isChecked = listOnly == true

        binding.searchChipRecycler.adapter = SearchChipAdapter(activity, this).also {
            activity.updateChips = { it.update() }
        }

        binding.searchChipRecycler.layoutManager =
            LinearLayoutManager(binding.root.context, HORIZONTAL, false)

        binding.searchFilter.setOnClickListener {
            SearchFilterBottomDialog.newInstance().show(activity.supportFragmentManager, "dialog")
        }
        binding.searchFilter.setOnLongClickListener {
            val popupMenu = PopupMenu(activity, binding.searchFilter)
            popupMenu.menuInflater.inflate(R.menu.sortby_filter_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sort_by_score -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[0]
                        activity.updateChips.invoke()
                        activity.search()
                        updateFilterTextViewDrawable()
                    }

                    R.id.sort_by_popular -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[1]
                        activity.updateChips.invoke()
                        activity.search()
                        updateFilterTextViewDrawable()
                    }

                    R.id.sort_by_trending -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[2]
                        activity.updateChips.invoke()
                        activity.search()
                        updateFilterTextViewDrawable()
                    }

                    R.id.sort_by_recent -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[3]
                        activity.updateChips.invoke()
                        activity.search()
                        updateFilterTextViewDrawable()
                    }

                    R.id.sort_by_a_z -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[4]
                        activity.updateChips.invoke()
                        activity.search()
                        updateFilterTextViewDrawable()
                    }

                    R.id.sort_by_z_a -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[5]
                        activity.updateChips.invoke()
                        activity.search()
                        updateFilterTextViewDrawable()
                    }

                    R.id.sort_by_pure_pain -> {
                        activity.aniMangaResult.sort = Anilist.sortBy[6]
                        activity.updateChips.invoke()
                        activity.search()
                        updateFilterTextViewDrawable()
                    }
                }
                true
            }
            popupMenu.show()
            true
        }
        if (activity.aniMangaResult.type != "ANIME") {
            binding.searchByImage.visibility = View.GONE
        }
        binding.searchByImage.setOnClickListener {
            activity.startActivity(Intent(activity, ImageSearchActivity::class.java))
        }
        binding.clearHistory.setOnClickListener {
            it.startAnimation(fadeOutAnimation())
            it.visibility = View.GONE
            searchHistoryAdapter.clearHistory()
        }
        updateClearHistoryVisibility()
        fun searchTitle() {
            activity.aniMangaResult.apply {
                search =
                    if (binding.searchBarText.text.toString() != "") binding.searchBarText.text.toString() else null
                onList = listOnly
                isAdult = adult
            }
            if (binding.searchBarText.text.toString().equals("hentai", true)) {
                openLinkInBrowser("https://www.youtube.com/watch?v=GgJrEOo0QoA")
            }
            activity.search()
        }

        textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().isBlank()) {
                    activity.emptyMediaAdapter()
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(200)
                        activity.runOnUiThread {
                            setHistoryVisibility(true)
                        }
                    }
                } else {
                    setHistoryVisibility(false)
                    searchTitle()
                }
            }
        }
        binding.searchBarText.addTextChangedListener(textWatcher)

        binding.searchBarText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    searchTitle()
                    binding.searchBarText.clearFocus()
                    imm.hideSoftInputFromWindow(binding.searchBarText.windowToken, 0)
                    true
                }

                else -> false
            }
        }
        binding.searchBar.setEndIconOnClickListener { searchTitle() }

        binding.searchResultGrid.setOnClickListener {
            it.alpha = 1f
            binding.searchResultList.alpha = 0.33f
            activity.style = 0
            PrefManager.setVal(PrefName.SearchStyle, 0)
            activity.recycler()
        }
        binding.searchResultList.setOnClickListener {
            it.alpha = 1f
            binding.searchResultGrid.alpha = 0.33f
            activity.style = 1
            PrefManager.setVal(PrefName.SearchStyle, 1)
            activity.recycler()
        }

        if (Anilist.adult) {
            binding.searchAdultCheck.visibility = View.VISIBLE
            binding.searchAdultCheck.isChecked = adult
            binding.searchAdultCheck.setOnCheckedChangeListener { _, b ->
                adult = b
                searchTitle()
            }
        } else binding.searchAdultCheck.visibility = View.GONE
        binding.searchList.apply {
            if (Anilist.userid != null) {
                visibility = View.VISIBLE
                checkedState = when (listOnly) {
                    null -> STATE_UNCHECKED
                    true -> STATE_CHECKED
                    false -> STATE_INDETERMINATE
                }

                addOnCheckedStateChangedListener { _, state ->
                    listOnly = when (state) {
                        STATE_CHECKED -> true
                        STATE_INDETERMINATE -> false
                        STATE_UNCHECKED -> null
                        else -> null
                    }
                }

                setOnTouchListener { _, event ->
                    (event.actionMasked == MotionEvent.ACTION_DOWN).also {
                        if (it) checkedState = (checkedState + 1) % 3
                        searchTitle()
                    }
                }
            } else visibility = View.GONE
        }

        search = Runnable { searchTitle() }
        requestFocus = Runnable { binding.searchBarText.requestFocus() }
    }

    class SearchChipAdapter(
        val activity: SearchActivity,
        private val searchAdapter: SearchAdapter
    ) :
        RecyclerView.Adapter<SearchChipAdapter.SearchChipViewHolder>() {
        private var chips = activity.aniMangaResult.toChipList()

        inner class SearchChipViewHolder(val binding: ItemChipBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchChipViewHolder {
            val binding =
                ItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SearchChipViewHolder(binding)
        }


        override fun onBindViewHolder(holder: SearchChipViewHolder, position: Int) {
            val chip = chips[position]
            holder.binding.root.apply {
                text = chip.text.replace("_", " ")
                setOnClickListener {
                    activity.aniMangaResult.removeChip(chip)
                    update()
                    activity.search()
                    searchAdapter.updateFilterTextViewDrawable()
                }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun update() {
            chips = activity.aniMangaResult.toChipList()
            notifyDataSetChanged()
            searchAdapter.updateFilterTextViewDrawable()
        }

        override fun getItemCount(): Int = chips.size
    }
}
