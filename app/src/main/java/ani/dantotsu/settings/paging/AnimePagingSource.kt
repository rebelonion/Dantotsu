package ani.dantotsu.settings.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
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
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemExtensionAllBinding
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.bumptech.glide.Glide
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class AnimeExtensionsViewModelFactory(
    private val animeExtensionManager: AnimeExtensionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnimeExtensionsViewModel(animeExtensionManager) as T
    }
}


class AnimeExtensionsViewModel(
    animeExtensionManager: AnimeExtensionManager
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
    val pagerFlow: Flow<PagingData<AnimeExtension.Available>> = combine(
        animeExtensionManager.availableExtensionsFlow,
        animeExtensionManager.installedExtensionsFlow,
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
            val aEPS = AnimeExtensionPagingSource(available, installed, query)
            currentPagingSource = aEPS
            aEPS
        }.flow
    }.cachedIn(viewModelScope)
}

class AnimeExtensionPagingSource(
    private val availableExtensionsFlow: List<AnimeExtension.Available>,
    private val installedExtensionsFlow: List<AnimeExtension.Installed>,
    private val searchQuery: String
) : PagingSource<Int, AnimeExtension.Available>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AnimeExtension.Available> {
        val position = params.key ?: 0
        val installedExtensions = installedExtensionsFlow.map { it.pkgName }.toSet()
        val availableExtensions =
            availableExtensionsFlow.filterNot { it.pkgName in installedExtensions }
        val query = searchQuery
        val isNsfwEnabled: Boolean = PrefManager.getVal(PrefName.NSFWExtension)

        val filteredExtensions = if (query.isEmpty()) {
            availableExtensions
        } else {
            availableExtensions.filter { it.name.contains(query, ignoreCase = true) }
        }
        val lang: String = PrefManager.getVal(PrefName.LangSort)
        val langFilter =
            if (lang != "all") filteredExtensions.filter { it.lang == lang } else filteredExtensions
        val filternfsw = if (isNsfwEnabled) langFilter else langFilter.filterNot { it.isNsfw }
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

    override fun getRefreshKey(state: PagingState<Int, AnimeExtension.Available>): Int? {
        return null
    }
}

class AnimeExtensionAdapter(private val clickListener: OnAnimeInstallClickListener) :
    PagingDataAdapter<AnimeExtension.Available, AnimeExtensionAdapter.AnimeExtensionViewHolder>(
        DIFF_CALLBACK
    ) {

    private val skipIcons: Boolean = PrefManager.getVal(PrefName.SkipExtensionIcons)

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AnimeExtension.Available>() {
            override fun areItemsTheSame(
                oldItem: AnimeExtension.Available,
                newItem: AnimeExtension.Available
            ): Boolean {
                // Your logic here
                return oldItem.pkgName == newItem.pkgName
            }

            override fun areContentsTheSame(
                oldItem: AnimeExtension.Available,
                newItem: AnimeExtension.Available
            ): Boolean {
                // Your logic here
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeExtensionViewHolder {
        val binding =
            ItemExtensionAllBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class AnimeExtensionViewHolder(private val binding: ItemExtensionAllBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val job = Job()
        private val scope = CoroutineScope(Dispatchers.Main + job)

        init {
            binding.closeTextView.setOnClickListener {
                if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val extension = getItem(bindingAdapterPosition)
                if (extension != null) {
                    clickListener.onInstallClick(extension)
                    binding.closeTextView.setImageResource(R.drawable.ic_sync)
                    scope.launch {
                        while (isActive) {
                            withContext(Dispatchers.Main) {
                                binding.closeTextView.animate()
                                    .rotationBy(360f)
                                    .setDuration(1000)
                                    .setInterpolator(LinearInterpolator())
                                    .start()
                            }
                            delay(1000)
                        }
                    }
                }
            }
        }

        val extensionIconImageView: ImageView = binding.extensionIconImageView

        fun bind(extension: AnimeExtension.Available) {
            val nsfw = if (extension.isNsfw) "(18+)" else ""
            val lang = LanguageMapper.getLanguageName(extension.lang)
            binding.extensionNameTextView.text = extension.name
            val versionText = "$lang ${extension.versionName} $nsfw"
            binding.extensionVersionTextView.text = versionText
        }

        fun clear() {
            job.cancel() // Cancel the coroutine when the view is recycled
        }
    }

    override fun onViewRecycled(holder: AnimeExtensionViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }
}

interface OnAnimeInstallClickListener {
    fun onInstallClick(pkg: AnimeExtension.Available)
}
