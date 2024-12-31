package ani.dantotsu.settings

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetAddRepositoryBinding
import ani.dantotsu.databinding.ItemRepoBinding
import ani.dantotsu.media.MediaType
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem

class RepoItem(
    val url: String,
    val onRemove: (String) -> Unit
) :BindableItem<ItemRepoBinding>() {
    override fun getLayout() = R.layout.item_repo

    override fun bind(viewBinding: ItemRepoBinding, position: Int) {
        viewBinding.repoNameTextView.text = url
        viewBinding.repoDeleteImageView.setOnClickListener {
            onRemove(url)
        }
    }

    override fun initializeViewBinding(view: View): ItemRepoBinding {
        return ItemRepoBinding.bind(view)
    }
}

class AddRepositoryBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddRepositoryBinding? = null
    private val binding get() = _binding!!
    private var mediaType: MediaType = MediaType.ANIME
    private var onRepositoryAdded: ((String, MediaType) -> Unit)? = null
    private var repositories: MutableList<String> = mutableListOf()
    private var onRepositoryRemoved: ((String) -> Unit)? = null
    private var adapter: GroupieAdapter = GroupieAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddRepositoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.repositoriesRecyclerView.adapter = adapter
        binding.repositoriesRecyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
        adapter.addAll(repositories.map { RepoItem(it, ::onRepositoryRemoved) })

        binding.repositoryInput.hint = when(mediaType) {
            MediaType.ANIME -> getString(R.string.anime_add_repository)
            MediaType.MANGA -> getString(R.string.manga_add_repository)
            else -> ""
        }

        binding.addButton.setOnClickListener {
            val input = binding.repositoryInput.text.toString()
            val error = isValidUrl(input)
            if (error == null) {
                onRepositoryAdded?.invoke(input, mediaType)
                dismiss()
            } else {
                binding.repositoryInput.error = error
            }
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.repositoryInput.setOnEditorActionListener { textView, action, keyEvent ->
            if (action == EditorInfo.IME_ACTION_DONE ||
                (keyEvent?.action == KeyEvent.ACTION_UP && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (!textView.text.isNullOrBlank()) {
                    val error = isValidUrl(textView.text.toString())
                    if (error == null) {
                        onRepositoryAdded?.invoke(textView.text.toString(), mediaType)
                        dismiss()
                        return@setOnEditorActionListener true
                    } else {
                        binding.repositoryInput.error = error
                    }
                }
            }
            false
        }
    }

    private fun onRepositoryRemoved(url: String) {
        onRepositoryRemoved?.invoke(url)
        repositories.remove(url)
        adapter.update(repositories.map { RepoItem(it, ::onRepositoryRemoved) })
    }

    private fun isValidUrl(url: String): String? {
        if (!url.startsWith("https://") && !url.startsWith("http://"))
            return "URL must start with http:// or https://"
        if (!url.removeSuffix("/").endsWith("index.min.json"))
            return "URL must end with index.min.json"
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            mediaType: MediaType,
            repositories: List<String>,
            onRepositoryAdded: (String, MediaType) -> Unit,
            onRepositoryRemoved: (String) -> Unit
        ): AddRepositoryBottomSheet {
            return AddRepositoryBottomSheet().apply {
                this.mediaType = mediaType
                this.repositories.addAll(repositories)
                this.onRepositoryAdded = onRepositoryAdded
                this.onRepositoryRemoved = onRepositoryRemoved
            }
        }
    }
}