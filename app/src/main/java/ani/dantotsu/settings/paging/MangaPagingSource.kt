package ani.dantotsu.settings.paging

import android.util.Log
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
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.databinding.ItemExtensionAllBinding
import ani.dantotsu.loadData
import com.bumptech.glide.Glide
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.lang.Math.min

class MangaExtensionsViewModelFactory(
    private val mangaExtensionManager: MangaExtensionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MangaExtensionsViewModel(mangaExtensionManager) as T
    }
}

class MangaExtensionsViewModel(
    private val mangaExtensionManager: MangaExtensionManager
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private var currentPagingSource: MangaExtensionPagingSource? = null

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun invalidatePager() {
        currentPagingSource?.invalidate()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagerFlow: Flow<PagingData<MangaExtension.Available>> = searchQuery.flatMapLatest { query ->
        Pager(
            PagingConfig(
                pageSize = 15,
                initialLoadSize = 15,
                prefetchDistance = 15
            )
        ) {
            MangaExtensionPagingSource(
                mangaExtensionManager.availableExtensionsFlow,
                mangaExtensionManager.installedExtensionsFlow,
                searchQuery
            ).also { currentPagingSource = it }
        }.flow
    }.cachedIn(viewModelScope)
}


class MangaExtensionPagingSource(
    private val availableExtensionsFlow: StateFlow<List<MangaExtension.Available>>,
    private val installedExtensionsFlow: StateFlow<List<MangaExtension.Installed>>,
    private val searchQuery: StateFlow<String>
) : PagingSource<Int, MangaExtension.Available>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MangaExtension.Available> {
        val position = params.key ?: 0
        val installedExtensions = installedExtensionsFlow.first().map { it.pkgName }.toSet()
        val availableExtensions = availableExtensionsFlow.first().filterNot { it.pkgName in installedExtensions }
        val query = searchQuery.first()
        val filteredExtensions = if (query.isEmpty()) {
            availableExtensions
        } else {
            availableExtensions.filter { it.name.contains(query, ignoreCase = true) }
        }
        val filternfsw = if(SettingsActivity.isNsfwEnabled) {//TODO
            filteredExtensions
        } else {
            filteredExtensions.filterNot { it.isNsfw }
        }
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

    override fun getRefreshKey(state: PagingState<Int, MangaExtension.Available>): Int? {
        return null
    }
}

class MangaExtensionAdapter(private val clickListener: OnMangaInstallClickListener) :
    PagingDataAdapter<MangaExtension.Available, MangaExtensionAdapter.MangaExtensionViewHolder>(
        DIFF_CALLBACK
    ) {

    private val skipIcons = loadData("skip_extension_icons") ?: false

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MangaExtension.Available>() {
            override fun areItemsTheSame(oldItem: MangaExtension.Available, newItem: MangaExtension.Available): Boolean {
                return oldItem.pkgName == newItem.pkgName
            }

            override fun areContentsTheSame(oldItem: MangaExtension.Available, newItem: MangaExtension.Available): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaExtensionViewHolder {
        val binding = ItemExtensionAllBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MangaExtensionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MangaExtensionViewHolder, position: Int) {
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

    inner class MangaExtensionViewHolder(private val binding: ItemExtensionAllBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.closeTextView.setOnClickListener {
                val extension = getItem(bindingAdapterPosition)
                if (extension != null) {
                    clickListener.onInstallClick(extension)
                }
            }
        }
        val extensionIconImageView: ImageView = binding.extensionIconImageView
        fun bind(extension: MangaExtension.Available) {
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

interface OnMangaInstallClickListener {
    fun onInstallClick(pkg: MangaExtension.Available)
}
