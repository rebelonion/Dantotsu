package ani.dantotsu.profile.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistQueries
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.FragmentFeedBinding
import ani.dantotsu.logger
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.snackString
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {
    private lateinit var binding: FragmentFeedBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var activityList: List<Activity> = emptyList()
    private lateinit var activity: androidx.activity.ComponentActivity
    private var page: Int = 1
    private var loadedFirstTime = false
    private var userId: Int? = null
    private var global: Boolean = false

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
        activity = requireActivity()
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.listProgressBar.visibility = ViewGroup.VISIBLE
        userId = arguments?.getInt("userId", -1)
        if (userId == -1) userId = null
        global = arguments?.getBoolean("global", false) ?: false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        if (this::binding.isInitialized) {
            binding.root.requestLayout()
            if (!loadedFirstTime) {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    val res = Anilist.query.getFeed(userId, global)
                    withContext(Dispatchers.Main) {
                        res?.data?.page?.activities?.let { activities ->
                            activityList = activities
                            adapter.update(activityList.map { ActivityItem(it) { _, _ -> } })
                        }
                        binding.listProgressBar.visibility = ViewGroup.GONE
                        val scrollView = binding.listRecyclerView

                        binding.listRecyclerView.setOnTouchListener { _, event ->
                            if (event?.action == MotionEvent.ACTION_UP) {
                                if (adapter.itemCount % AnilistQueries.ITEMS_PER_PAGE != 0 && !global) {
                                    snackString("No more activities")
                                } else if (!scrollView.canScrollVertically(1) && !binding.feedRefresh.isVisible
                                    && binding.listRecyclerView.adapter!!.itemCount != 0 &&
                                    (binding.listRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() == (binding.listRecyclerView.adapter!!.itemCount - 1)
                                ) {
                                    page++
                                    binding.feedRefresh.visibility = ViewGroup.VISIBLE
                                    activity.lifecycleScope.launch(Dispatchers.IO) {
                                        val res = Anilist.query.getFeed(userId, global, page)
                                        withContext(Dispatchers.Main) {
                                            res?.data?.page?.activities?.let { activities ->
                                                activityList += activities
                                                adapter.addAll(activities.map { ActivityItem(it) { _, _ -> } })
                                            }
                                            binding.feedRefresh.visibility = ViewGroup.GONE
                                        }
                                    }
                                }
                            }
                            false
                        }
                    }
                }
                loadedFirstTime = true
            }
        }
    }

    companion object {
        fun newInstance(userId: Int?, global: Boolean): FeedFragment {
            val fragment = FeedFragment()
            val args = Bundle()
            args.putInt("userId", userId ?: -1)
            args.putBoolean("global", global)
            fragment.arguments = args
            return fragment
        }
    }
}