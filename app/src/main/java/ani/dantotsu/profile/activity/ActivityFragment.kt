package ani.dantotsu.profile.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.FragmentFeedBinding
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.util.ActivityMarkdownCreator
import com.xwray.groupie.GroupieAdapter
import eu.kanade.tachiyomi.util.system.getSerializableCompat
import kotlinx.coroutines.launch

class ActivityFragment : Fragment() {
    private lateinit var type: ActivityType
    private var userId: Int? = null
    private var activityId: Int? = null
    private lateinit var binding: FragmentFeedBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var page: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            type = it.getSerializableCompat<ActivityType>("type") as ActivityType
            userId = it.getInt("userId")
            activityId = it.getInt("activityId")
        }
        binding.titleBar.visibility =
            if (type == ActivityType.OTHER_USER) View.VISIBLE else View.GONE
        binding.titleText.text =
            if (userId == Anilist.userid) getString(R.string.create_new_activity) else getString(R.string.write_a_message)
        binding.titleImage.setOnClickListener { handleTitleImageClick() }
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.listProgressBar.isVisible = true

        binding.feedRefresh.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        binding.emptyTextView.text = getString(R.string.nothing_here)
        lifecycleScope.launch {
            getList()
            if (adapter.itemCount == 0) {
                binding.emptyTextView.isVisible = true
            }
            binding.listProgressBar.isVisible = false
        }
        binding.feedSwipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                adapter.clear()
                page = 1
                getList()
                binding.feedSwipeRefresh.isRefreshing = false
            }
        }
        binding.listRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (shouldLoadMore()) {
                    lifecycleScope.launch {
                        binding.feedRefresh.isVisible = true
                        getList()
                        binding.feedRefresh.isVisible = false
                    }
                }
            }
        })
    }

    private fun handleTitleImageClick() {
        val intent = Intent(context, ActivityMarkdownCreator::class.java).apply {
            putExtra("type", if (userId == Anilist.userid) "activity" else "message")
            putExtra("userId", userId)
        }
        ContextCompat.startActivity(requireContext(), intent, null)
    }

    private suspend fun getList() {
        val list = when (type) {
            ActivityType.GLOBAL -> getActivities(global = true)
            ActivityType.USER -> getActivities(filter = true)
            ActivityType.OTHER_USER -> getActivities(userId = userId)
            ActivityType.ONE -> getActivities(activityId = activityId)
        }
        adapter.addAll(list.map { ActivityItem(it, adapter, ::onActivityClick) })
    }

    private suspend fun getActivities(
        global: Boolean = false,
        userId: Int? = null,
        activityId: Int? = null,
        filter: Boolean = false
    ): List<Activity> {
        val res = Anilist.query.getFeed(userId, global, page, activityId)?.data?.page?.activities
        page += 1
        return res
            ?.filter { if (Anilist.adult) true else it.media?.isAdult != true }
            ?.filterNot { it.recipient?.id != null && it.recipient.id != Anilist.userid && filter }
            ?: emptyList()
    }

    private fun shouldLoadMore(): Boolean {
        val layoutManager =
            (binding.listRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        val adapter = binding.listRecyclerView.adapter
        return !binding.listRecyclerView.canScrollVertically(1) &&
                !binding.feedRefresh.isVisible && adapter?.itemCount != 0 &&
                layoutManager == (adapter!!.itemCount - 1)

    }

    private fun onActivityClick(id: Int, type: String) {
        val intent = when (type) {
            "USER" -> Intent(requireContext(), ProfileActivity::class.java).putExtra("userId", id)
            "MEDIA" -> Intent(
                requireContext(),
                MediaDetailsActivity::class.java
            ).putExtra("mediaId", id)

            else -> return
        }
        ContextCompat.startActivity(requireContext(), intent, null)
    }

    override fun onResume() {
        super.onResume()
        if (this::binding.isInitialized) {
            binding.root.requestLayout()
        }
    }

    companion object {
        enum class ActivityType { GLOBAL, USER, OTHER_USER, ONE }

        fun newInstance(
            type: ActivityType,
            userId: Int? = null,
            activityId: Int? = null
        ): ActivityFragment {
            return ActivityFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", type)
                    userId?.let { putInt("userId", it) }
                    activityId?.let { putInt("activityId", it) }
                }
            }
        }
    }
}