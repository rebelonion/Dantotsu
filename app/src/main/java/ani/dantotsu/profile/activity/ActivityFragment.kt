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
import ani.dantotsu.setBaseline
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch

class ActivityFragment(
    var type: ActivityType,
    val userId: Int? = null,
    var activityId: Int? = null,
) : Fragment() {
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
        val navBar = if (userId != null) {
            (activity as ProfileActivity).navBar
        } else {
            (activity as FeedActivity).navBar
        }
        binding.listRecyclerView.setBaseline(navBar)
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.listProgressBar.isVisible = true
        binding.feedRefresh.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        binding.emptyTextView.text = getString(R.string.no_notifications)
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


    private suspend fun getList() {
        val list = when (type) {
            ActivityType.GLOBAL -> getActivities(true)
            ActivityType.USER -> getActivities()
            ActivityType.OTHER_USER -> getActivities(userId = userId)
            ActivityType.ONE -> getActivities(activityId = activityId)
        }
        adapter.addAll(list.map { ActivityItem(it, ::onActivityClick, requireActivity()) })
    }

    private suspend fun getActivities(
        global: Boolean = false,
        userId: Int? = null,
        activityId: Int? = null,
    ): List<Activity> {
        val res = Anilist.query.getFeed(userId, global, page, activityId)?.data?.page?.activities
        page += 1
        return res
            ?.filter { if (Anilist.adult) true else it.media?.isAdult != true }
            ?.filterNot { it.recipient?.id != null && it.recipient.id != Anilist.userid }
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
        when (type) {
            "USER" -> {
                ContextCompat.startActivity(
                    requireContext(), Intent(requireContext(), ProfileActivity::class.java)
                        .putExtra("userId", id), null
                )
            }

            "MEDIA" -> {
                ContextCompat.startActivity(
                    requireContext(), Intent(requireContext(), MediaDetailsActivity::class.java)
                        .putExtra("mediaId", id), null
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::binding.isInitialized) {
            binding.root.requestLayout()
            val navBar = if (userId != null) {
                (activity as ProfileActivity).navBar
            } else {
                (activity as FeedActivity).navBar
            }
            binding.listRecyclerView.setBaseline(navBar)
        }
    }

    companion object {
        enum class ActivityType {
            GLOBAL, USER, OTHER_USER, ONE
        }
    }
}