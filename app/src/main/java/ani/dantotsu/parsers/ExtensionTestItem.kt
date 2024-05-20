package ani.dantotsu.parsers

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemExtensionTestBinding
import ani.dantotsu.getThemeColor
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtensionTestItem(
    private var extensionType: String,
    private var testType: String,
    private var extension: BaseParser,
    private var searchString: String = "Chainsaw Man"
) : BindableItem<ItemExtensionTestBinding>() {
    private lateinit var binding: ItemExtensionTestBinding
    private lateinit var context: Context
    private var job: Job? = null
    private var isRunning = false
    private var pingResult: Triple<Int, Int?, String>? = null
    private var searchResultSize: Int? = null
    private var episodeResultSize: Int? = null
    private var serverResultSize: Int? = null

    override fun bind(viewBinding: ItemExtensionTestBinding, position: Int) {
        binding = viewBinding
        context = binding.root.context
        binding.extensionIconImageView.setImageDrawable(extension.icon)
        binding.extensionNameTextView.text = extension.name
        binding.extensionLoading.isVisible = isRunning
        hideAllResults()

        pingResult()
        searchResult()
        episodeResult()
        serverResult()
    }

    override fun getLayout(): Int {
        return R.layout.item_extension_test
    }

    override fun initializeViewBinding(view: View): ItemExtensionTestBinding {
        return ItemExtensionTestBinding.bind(view)
    }

    private fun hideAllResults() {
        if (::binding.isInitialized.not()) return
        binding.searchResultText.isVisible = false
        binding.episodeResultText.isVisible = false
        binding.serverResultText.isVisible = false
    }

    fun cancelJob() {
        job?.cancel()
        job = null
        binding.extensionLoading.isVisible = false
    }

    fun startTest() {
        pingResult = null
        searchResultSize = null
        episodeResultSize = null
        serverResultSize = null
        isRunning = true
        hideAllResults()
        job?.cancel()
        job = Job()
        CoroutineScope(Dispatchers.IO + job!!).launch {
            when (extensionType) {
                "anime" -> {
                    val extension = extension as AnimeParser
                    runAnimeTest(extension)
                }

                "manga" -> {
                    val extension = extension as MangaParser
                    runMangaTest(extension)
                }

                "novel" -> {
                    val extension = extension as NovelParser
                    runNovelTest(extension)
                }
            }
        }
    }

    private suspend fun runAnimeTest(extension: AnimeParser) {
        pingResult = extension.ping()
        withContext(Dispatchers.Main) {
            pingResult()
        }
        if (testType == "ping") {
            done()
            return
        }
        val searchResult = extension.search(searchString)
        searchResultSize = searchResult.size
        withContext(Dispatchers.Main) {
            searchResult()
        }
        if (searchResultSize == 0 || testType == "basic") {
            done()
            return
        }
        val episodeResult = extension.loadEpisodes("", null, searchResult.first().sAnime!!)
        episodeResultSize = episodeResult.size
        withContext(Dispatchers.Main) {
            episodeResult()
        }
        if (episodeResultSize == 0) {
            done()
            return
        }
        val serverResult = extension.loadVideoServers("", null, episodeResult.first().sEpisode!!)
        serverResultSize = serverResult.size
        withContext(Dispatchers.Main) {
            serverResult()
        }

        done()
    }

    private suspend fun runMangaTest(extension: MangaParser) {
        pingResult = extension.ping()
        withContext(Dispatchers.Main) {
            pingResult()
        }
        if (testType == "ping") {
            done()
            return
        }
        val searchResult = extension.search(searchString)
        searchResultSize = searchResult.size
        withContext(Dispatchers.Main) {
            searchResult()
        }
        if (searchResultSize == 0 || testType == "basic") {
            done()
            return
        }
        val chapterResult = extension.loadChapters("", null, searchResult.first().sManga!!)
        episodeResultSize = chapterResult.size
        withContext(Dispatchers.Main) {
            episodeResult()
        }
        if (episodeResultSize == 0) {
            done()
            return
        }
        val serverResult = extension.loadImages("",  chapterResult.first().sChapter)
        serverResultSize = serverResult.size
        withContext(Dispatchers.Main) {
            serverResult()
        }

        withContext(Dispatchers.Main) {
            if (::binding.isInitialized )
                binding.extensionLoading.isVisible = false
            isRunning = false
        }
    }

    private suspend fun runNovelTest(extension: NovelParser) {
        withContext(Dispatchers.Main) {
            pingResult()
        }
        if (testType == "ping") {
            done()
            return
        }
        val searchResult = extension.search(searchString)
        searchResultSize = searchResult.size
        withContext(Dispatchers.Main) {
            searchResult()
        }
        if (searchResultSize == 0 || testType == "basic") {
            done()
            return
        }
        val chapterResult = extension.loadBook(searchResult.first().link, null)
        episodeResultSize = chapterResult.links.size
        withContext(Dispatchers.Main) {
            episodeResult()
            serverResult()
        }

        withContext(Dispatchers.Main) {
            if (::binding.isInitialized )
                binding.extensionLoading.isVisible = false
            isRunning = false
        }
    }

    private fun done() {
        if (::binding.isInitialized.not()) return
        binding.extensionLoading.isVisible = false
        isRunning = false
    }

    private fun pingResult() {
        if (::binding.isInitialized.not()) return
        if (extensionType == "novel") {
            binding.pingResultText.isVisible = true
            binding.pingResultText.text = context.getString(R.string.test_not_supported)
            binding.pingResultText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_round_info_24, 0, 0, 0
            )
            return
        }
        if (pingResult == null) {
            binding.pingResultText.isVisible = false
            return
        } else {
            binding.pingResultText.isVisible = true
        }
        binding.pingResultText.setTextColor(
            context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
        )
        val (code, time, message) = pingResult!!
        if (code == 200) {
            binding.pingResultText.text = context.getString(R.string.ping_success, time.toString())
            binding.pingResultText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_circle_check, 0, 0, 0
            )
            return
        }
        binding.pingResultText.text =
            context.getString(R.string.ping_error, code.toString(), message)
        binding.pingResultText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_circle_cancel, 0, 0, 0
        )
        binding.pingResultText.setTextColor(
            context.getThemeColor(com.google.android.material.R.attr.colorError)
        )
    }

    private fun searchResult() {
        if (::binding.isInitialized.not()) return
        if (searchResultSize == null) {
            binding.searchResultText.isVisible = false
            return
        }
        binding.searchResultText.setTextColor(
            context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
        )
        binding.searchResultText.isVisible = true
        if (searchResultSize == 0) {
            val text = context.getString(R.string.title_search_test,
                context.getString(R.string.no_results_found))
            binding.searchResultText.text = text
            binding.searchResultText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_circle_cancel, 0, 0, 0
            )
            binding.searchResultText.setTextColor(
                context.getThemeColor(com.google.android.material.R.attr.colorError)
            )
            return
        }
        val text = context.getString(R.string.title_search_test,
            context.getString(R.string.results_found, searchResultSize.toString()))
        binding.searchResultText.text = text
        binding.searchResultText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_circle_check, 0, 0, 0
        )
    }

    private fun episodeResult() {
        if (::binding.isInitialized.not()) return
        if (episodeResultSize == null) {
            binding.episodeResultText.isVisible = false
            return
        }
        binding.episodeResultText.setTextColor(
            context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
        )
        binding.episodeResultText.isVisible = true
        if (episodeResultSize == 0) {
            val text = when(extensionType) {
                "anime" -> context.getString(R.string.episode_search_test,
                    context.getString(R.string.no_results_found))
                "manga" -> context.getString(R.string.chapter_search_test,
                    context.getString(R.string.no_results_found))
                else -> context.getString(R.string.book_search_test,
                    context.getString(R.string.no_results_found))
            }
            binding.episodeResultText.text = text
            binding.episodeResultText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_circle_cancel, 0, 0, 0
            )
            binding.episodeResultText.setTextColor(
                context.getThemeColor(com.google.android.material.R.attr.colorError)
            )
            return
        }
        val text = when(extensionType) {
            "anime" -> context.getString(R.string.episode_search_test,
                context.getString(R.string.results_found, episodeResultSize.toString()))
            "manga" -> context.getString(R.string.chapter_search_test,
                context.getString(R.string.results_found, episodeResultSize.toString()))
            else -> context.getString(R.string.book_search_test,
                context.getString(R.string.results_found, episodeResultSize.toString()))
        }
        binding.episodeResultText.text = text
        binding.episodeResultText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_circle_check, 0, 0, 0
        )
    }

    private fun serverResult() {
        if (::binding.isInitialized.not()) return
        if (extensionType == "novel") {
            binding.pingResultText.isVisible = true
            binding.pingResultText.text = context.getString(R.string.test_not_supported)
            binding.pingResultText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_round_info_24, 0, 0, 0
            )
            return
        }
        if (serverResultSize == null) {
            binding.serverResultText.isVisible = false
            return
        }
        binding.serverResultText.setTextColor(
            context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
        )
        binding.serverResultText.isVisible = true
        if (serverResultSize == 0) {
            val text = when(extensionType) {
                "anime" -> context.getString(R.string.video_search_test,
                    context.getString(R.string.no_results_found))
                "manga" -> context.getString(R.string.image_search_test,
                    context.getString(R.string.no_results_found))
                else -> context.getString(R.string.book_search_test,
                    context.getString(R.string.no_results_found))
            }
            binding.serverResultText.text = text
            binding.serverResultText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_circle_cancel, 0, 0, 0
            )
            binding.serverResultText.setTextColor(
                context.getThemeColor(com.google.android.material.R.attr.colorError)
            )
            return
        }
        val text = when(extensionType) {
            "anime" -> context.getString(R.string.video_search_test,
                context.getString(R.string.results_found, serverResultSize.toString()))
            "manga" -> context.getString(R.string.image_search_test,
                context.getString(R.string.results_found, serverResultSize.toString()))
            else -> context.getString(R.string.book_search_test,
                context.getString(R.string.results_found, serverResultSize.toString()))
        }
        binding.serverResultText.text = text
        binding.serverResultText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_circle_check, 0, 0, 0
        )
    }

}