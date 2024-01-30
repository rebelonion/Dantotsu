package ani.dantotsu.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemSearchHistoryBinding
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.PrefWrapper
import ani.dantotsu.settings.saving.PrefWrapper.asLiveStringSet
import ani.dantotsu.settings.saving.SharedPreferenceStringSetLiveData
import java.util.Locale

class SearchHistoryAdapter(private val type: String, private val searchClicked: (String) -> Unit) : ListAdapter<String, SearchHistoryAdapter.SearchHistoryViewHolder>(
    DIFF_CALLBACK_INSTALLED
) {
    private var searchHistoryLiveData: SharedPreferenceStringSetLiveData? = null
    private var searchHistory: MutableSet<String>? = null
    private var historyType: PrefName = when (type.lowercase(Locale.ROOT)) {
        "anime" -> PrefName.AnimeSearchHistory
        "manga" -> PrefName.MangaSearchHistory
        else -> throw IllegalArgumentException("Invalid type")
    }

    init {
        searchHistoryLiveData = PrefWrapper.getLiveVal(historyType, mutableSetOf<String>()).asLiveStringSet()
        searchHistoryLiveData?.observeForever {
            searchHistory = it.toMutableSet()
            submitList(searchHistory?.reversed())
        }
    }

    fun remove(item: String) {
        searchHistory?.remove(item)
        PrefWrapper.setVal(historyType, searchHistory)
    }

    fun add(item: String) {
        if (searchHistory?.contains(item) == true || item.isBlank()) return
        if (PrefWrapper.getVal(PrefName.Incognito, false)) return
        searchHistory?.add(item)
        PrefWrapper.setVal(historyType, searchHistory)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchHistoryAdapter.SearchHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_history, parent, false)
        return SearchHistoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SearchHistoryAdapter.SearchHistoryViewHolder,
        position: Int
    ) {
        holder.binding.searchHistoryTextView.text = getItem(position)
        holder.binding.closeTextView.setOnClickListener {
            if (position >= itemCount || position < 0) return@setOnClickListener
            remove(getItem(position))
        }
        holder.binding.searchHistoryTextView.setOnClickListener {
            if (position >= itemCount || position < 0) return@setOnClickListener
            searchClicked(getItem(position))
        }
    }

    inner class SearchHistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemSearchHistoryBinding.bind(view)
    }

    companion object {
        val DIFF_CALLBACK_INSTALLED = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
