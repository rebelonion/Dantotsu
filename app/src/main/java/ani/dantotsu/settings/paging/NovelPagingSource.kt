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
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.parsers.novel.NovelExtensionManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest


class NovelExtensionsViewModelFactory(
    private val novelExtensionManager: NovelExtensionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NovelExtensionsViewModel(novelExtensionManager) as T
    }
}

class NovelExtensionsViewModel(
    private val novelExtensionManager: NovelExtensionManager
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private var currentPagingSource: NovelExtensionPagingSource? = null

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun invalidatePager() {
        currentPagingSource?.invalidate()

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagerFlow: Flow<PagingData<NovelExtension.Available>> = combine(
        novelExtensionManager.availableExtensionsFlow,
        novelExtensionManager.installedExtensionsFlow,
        searchQuery
    ) { available, installed, query ->
        Triple(available, installed, query)
    }.flatMapLatest { (available, installed, query) ->
        Pager(
            PagingConfig(
                pageSize = 15,
                initialLoadSize = 15,
                prefetchDistance = 15
            )
        ) {
            NovelExtensionPagingSource(available, installed, query)
        }.flow
    }.cachedIn(viewModelScope)
}


class NovelExtensionPagingSource(
    private val availableExtensionsFlow: List<NovelExtension.Available>,
    private val installedExtensionsFlow: List<NovelExtension.Installed>,
    private val searchQuery: String
) : PagingSource<Int, NovelExtension.Available>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NovelExtension.Available> {
        val position = params.key ?: 0
        val installedExtensions = installedExtensionsFlow.map { it.pkgName }.toSet()
        val availableExtensions =
            availableExtensionsFlow.filterNot { it.pkgName in installedExtensions }
        val query = searchQuery
        val isNsfwEnabled: Boolean = loadData("NFSWExtension") ?: true
        val filteredExtensions = if (query.isEmpty()) {
            availableExtensions
        } else {
            availableExtensions.filter { it.name.contains(query, ignoreCase = true) }
        }
        val filternfsw = filteredExtensions
        /*val filternfsw = if(isNsfwEnabled) {  currently not implemented
            filteredExtensions
        } else {
            filteredExtensions.filterNot { it.isNsfw }
        }*/
        return try {
            val sublist = filternfsw.subList(
                fromIndex = position,
                toIndex = (position + params.loadSize).coerceAtMost(filternfsw.size)
            )
            LoadResult.Page(
                data = sublist,
                prevKey = if (position == 0) null else position - params.loadSize,
                nextKey = if (position + params.loadSize >= filternfsw.size) null else position + params.loadSize
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, NovelExtension.Available>): Int? {
        return null
    }
}

class NovelExtensionAdapter(private val clickListener: OnNovelInstallClickListener) :
    PagingDataAdapter<NovelExtension.Available, NovelExtensionAdapter.NovelExtensionViewHolder>(
        DIFF_CALLBACK
    ) {

    private val skipIcons = loadData("skip_extension_icons") ?: false

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NovelExtension.Available>() {
            override fun areItemsTheSame(
                oldItem: NovelExtension.Available,
                newItem: NovelExtension.Available
            ): Boolean {
                return oldItem.pkgName == newItem.pkgName
            }

            override fun areContentsTheSame(
                oldItem: NovelExtension.Available,
                newItem: NovelExtension.Available
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelExtensionViewHolder {
        val binding =
            ItemExtensionAllBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NovelExtensionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NovelExtensionViewHolder, position: Int) {
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

    inner class NovelExtensionViewHolder(private val binding: ItemExtensionAllBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.closeTextView.setOnClickListener {
                val extension = getItem(bindingAdapterPosition)
                if (extension != null) {
                    clickListener.onInstallClick(extension)
                }
            }
        }

        val extensionIconImageView: ImageView = binding.extensionIconImageView
        fun bind(extension: NovelExtension.Available) {
            val nsfw = ""
            val lang = LanguageMapper.mapLanguageCodeToName("all")
            binding.extensionNameTextView.text = extension.name
            binding.extensionVersionTextView.text = "$lang ${extension.versionName} $nsfw"
        }
    }
}

interface OnNovelInstallClickListener {
    fun onInstallClick(pkg: NovelExtension.Available)
}
