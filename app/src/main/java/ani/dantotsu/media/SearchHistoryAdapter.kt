package ani.dantotsu.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemSearchHistoryBinding
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefManager.asLiveStringSet
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.SharedPreferenceStringSetLiveData
import java.util.Locale

class SearchHistoryAdapter(private val type: String, private val searchClicked: (String) -> Unit) :
    ListAdapter<String, SearchHistoryAdapter.SearchHistoryViewHolder>(
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
        searchHistoryLiveData =
            PrefManager.getLiveVal(historyType, mutableSetOf<String>()).asLiveStringSet()
        searchHistoryLiveData?.observeForever {
            searchHistory = it.toMutableSet()
            submitList(searchHistory?.toList())
        }
    }

    fun remove(item: String) {
        searchHistory?.remove(item)
        PrefManager.setVal(historyType, searchHistory)
        submitList(searchHistory?.toList())
    }

    fun add(item: String) {
        if (searchHistory?.contains(item) == true || item.isBlank()) return
        if (PrefManager.getVal(PrefName.Incognito)) return
        searchHistory?.add(item)
        submitList(searchHistory?.toList())
        PrefManager.setVal(historyType, searchHistory)
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
        val item = getItem(position)
        holder.binding.searchHistoryTextView.text = item
        holder.binding.closeTextView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition >= itemCount || currentPosition < 0) return@setOnClickListener
            remove(getItem(currentPosition))
        }
        holder.binding.searchHistoryTextView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition >= itemCount || currentPosition < 0) return@setOnClickListener
            searchClicked(getItem(currentPosition))
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
