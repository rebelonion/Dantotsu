package ani.dantotsu.settings.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemExtensionAllBinding
import ani.dantotsu.loadData
import com.bumptech.glide.Glide
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest


class AnimeExtensionsViewModelFactory(
    private val animeExtensionManager: AnimeExtensionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnimeExtensionsViewModel(animeExtensionManager) as T
    }
}


class AnimeExtensionsViewModel(
    private val animeExtensionManager: AnimeExtensionManager
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private var currentPagingSource: AnimeExtensionPagingSource? = null
    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
    fun invalidatePager() {
        currentPagingSource?.invalidate()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagerFlow: Flow<PagingData<AnimeExtension.Available>> = searchQuery.flatMapLatest { query ->
        Pager(
            PagingConfig(
                pageSize = 15,
                initialLoadSize = 15,
                prefetchDistance = 15
            )
        ) {
            AnimeExtensionPagingSource(
                animeExtensionManager.availableExtensionsFlow,
                animeExtensionManager.installedExtensionsFlow,
                searchQuery
            ).also { currentPagingSource = it }
        }.flow
    }.cachedIn(viewModelScope)
}

class AnimeExtensionPagingSource(
    private val availableExtensionsFlow: StateFlow<List<AnimeExtension.Available>>,
    private val installedExtensionsFlow: StateFlow<List<AnimeExtension.Installed>>,
    private val searchQuery: StateFlow<String>
) : PagingSource<Int, AnimeExtension.Available>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AnimeExtension.Available> {
        val position = params.key ?: 0
        val installedExtensions = installedExtensionsFlow.first().map { it.pkgName }.toSet()
        val availableExtensions = availableExtensionsFlow.first().filterNot { it.pkgName in installedExtensions }
        val query = searchQuery.first()
        val filteredExtensions = if (query.isEmpty()) {
            availableExtensions
        } else {
            availableExtensions.filter { it.name.contains(query, ignoreCase = true) }
        }

        return try {
            val sublist = filteredExtensions.subList(
                fromIndex = position,
                toIndex = (position + params.loadSize).coerceAtMost(filteredExtensions.size)
            )
            LoadResult.Page(
                data = sublist,
                prevKey = if (position == 0) null else position - params.loadSize,
                nextKey = if (position + params.loadSize >= filteredExtensions.size) null else position + params.loadSize
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, AnimeExtension.Available>): Int? {
        return null
    }
}

class AnimeExtensionAdapter(private val clickListener: OnAnimeInstallClickListener) :
    PagingDataAdapter<AnimeExtension.Available, AnimeExtensionAdapter.AnimeExtensionViewHolder>(
        DIFF_CALLBACK
    ) {

    private val skipIcons = loadData("skip_extension_icons") ?: false

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AnimeExtension.Available>() {
            override fun areItemsTheSame(oldItem: AnimeExtension.Available, newItem: AnimeExtension.Available): Boolean {
                // Your logic here
                return oldItem.pkgName == newItem.pkgName
            }

            override fun areContentsTheSame(oldItem: AnimeExtension.Available, newItem: AnimeExtension.Available): Boolean {
                // Your logic here
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeExtensionViewHolder {
        val binding = ItemExtensionAllBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimeExtensionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimeExtensionViewHolder, position: Int) {
        val extension = getItem(position)
        if (extension != null) {
            if (!skipIcons) {
                Glide.with(holder.itemView.context)
                    .load(extension.iconUrl)
                    .into(holder.extensionIconImageView)
            }
            holder.bind(extension)
        }
    }

    inner class AnimeExtensionViewHolder(private val binding: ItemExtensionAllBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.closeTextView.setOnClickListener {
                val extension = getItem(bindingAdapterPosition)
                if (extension != null) {
                    clickListener.onInstallClick(extension)
                }
            }
        }
        val extensionIconImageView: ImageView = binding.extensionIconImageView
            fun bind(extension: AnimeExtension.Available) {
            val nsfw = if (extension.isNsfw) {
                "(18+)"
            } else {
                ""
            }
            binding.extensionNameTextView.text = extension.name
            binding.extensionVersionTextView.text = "${extension.versionName} $nsfw"
        }
    }
}

interface OnAnimeInstallClickListener {
    fun onInstallClick(pkg: AnimeExtension.Available)
}
