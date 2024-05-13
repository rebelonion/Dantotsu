package ani.dantotsu.home.status

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.ActivityReply
import ani.dantotsu.databinding.BottomSheetRecyclerBinding
import ani.dantotsu.profile.activity.ActivityReplyItem
import ani.dantotsu.snackString
import ani.dantotsu.util.MarkdownCreatorActivity
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepliesBottomDialog : BottomSheetDialogFragment() {
    private var _binding: BottomSheetRecyclerBinding? = null
    private val binding get() = _binding!!
    private val adapter: GroupieAdapter = GroupieAdapter()
    private val replies: MutableList<ActivityReply> = mutableListOf()
    private var activityId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetRecyclerBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.repliesRecyclerView.adapter = adapter
        binding.repliesRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.replyButton.setOnClickListener {
            val context = requireContext()
            ContextCompat.startActivity(
                context,
                Intent(context, MarkdownCreatorActivity::class.java)
                    .putExtra("type", "replyActivity")
                    .putExtra("parentId", activityId),
                null
            )
        }
        activityId = requireArguments().getInt("activityId")
        loading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val response = Anilist.query.getReplies(activityId)
            withContext(Dispatchers.Main) {
                loading(false)
                if (response != null) {
                    replies.clear()
                    replies.addAll(response.data.page.activityReplies)
                    adapter.update(replies.map { ActivityReplyItem(it, requireActivity()) { _, _ -> } })
                } else {
                    snackString("Failed to load replies")
                }
            }
        }

    }

    private fun loading(load: Boolean) {
        binding.repliesRefresh.isVisible = load
        binding.repliesRecyclerView.isVisible = !load
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(activityId: Int): RepliesBottomDialog {
            return RepliesBottomDialog().apply {
                arguments = Bundle().apply {
                    putInt("activityId", activityId)
                }
            }
        }
    }
}