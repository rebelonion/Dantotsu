package ani.dantotsu.profile.notification

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
import androidx.webkit.internal.ApiFeature.N
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.databinding.FragmentNotificationsBinding
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.notifications.comment.CommentStore
import ani.dantotsu.notifications.subscription.SubscriptionStore
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.FeedActivity
import ani.dantotsu.setBaseline
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch


class NotificationFragment(
    val type: NotificationType,
    val getID: Int = -1
) : Fragment() {
    private lateinit var binding: FragmentNotificationsBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var currentPage = 1
    private var hasNextPage = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navbar = (activity as NotificationActivity).navBar
        binding.notificationRecyclerView.setBaseline(navbar)
        binding.notificationRecyclerView.adapter = adapter
        binding.notificationRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.notificationRefresh.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        binding.emptyTextView.text = getString(R.string.no_notifications)
        lifecycleScope.launch {
            getList()
            if (adapter.itemCount == 0) {
                binding.emptyTextView.isVisible = true
            }
            binding.notificationProgressBar.isVisible = false
        }
        binding.notificationSwipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                adapter.clear()
                currentPage = 1
                getList()
                binding.notificationSwipeRefresh.isRefreshing = false
            }
        }
        binding.notificationRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (shouldLoadMore()) {
                    lifecycleScope.launch {
                        getList()
                        binding.notificationRefresh.isVisible = false
                    }
                }
            }
        })

    }

    private suspend fun getList() {
        val list = when (type) {
            NotificationType.ONE -> getNotificationsFiltered(false) { it.id == getID }
            NotificationType.MEDIA -> getNotificationsFiltered { it.media != null }
            NotificationType.USER -> getNotificationsFiltered { it.media == null }
            NotificationType.SUBSCRIPTION -> getSubscriptions()
            NotificationType.COMMENT -> getComments()
            else -> listOf()
        }
        adapter.addAll(list.map { NotificationItem(it, ::onClick) })
    }

    private suspend fun getNotificationsFiltered(
        reset: Boolean = true,
        filter: (Notification) -> Boolean
    ): List<Notification> {
        val userId =
            Anilist.userid ?: PrefManager.getVal<String>(PrefName.AnilistUserId).toIntOrNull() ?: 0
        val res = Anilist.query.getNotifications(userId, currentPage, reset)?.data?.page
        currentPage = res?.pageInfo?.currentPage?.plus(1) ?: 1
        hasNextPage = res?.pageInfo?.hasNextPage ?: false
        return res?.notifications?.filter(filter) ?: listOf()
    }

    private fun getSubscriptions(): List<Notification> {
        val list = PrefManager.getNullableVal<List<SubscriptionStore>>(
            PrefName.SubscriptionNotificationStore,
            null
        ) ?: listOf()
        return list.sortedByDescending { (it.time / 1000L).toInt() }
            .filter { it.image != null }.map {
                Notification(
                    it.type,
                    System.currentTimeMillis().toInt(),
                    commentId = it.mediaId,
                    mediaId = it.mediaId,
                    notificationType = it.type,
                    context = it.title + ": " + it.content,
                    createdAt = (it.time / 1000L).toInt(),
                    image = it.image,
                    banner = it.banner ?: it.image
                )
            }
    }

    private fun getComments(): List<Notification> {
        val list = PrefManager.getNullableVal<List<CommentStore>>(
            PrefName.CommentNotificationStore,
            null
        ) ?: listOf()
        return list
            .sortedByDescending { (it.time / 1000L).toInt() }
            .map {
                Notification(
                    it.type.toString(),
                    System.currentTimeMillis().toInt(),
                    commentId = it.commentId,
                    notificationType = it.type.toString(),
                    mediaId = it.mediaId,
                    context = it.title + "\n" + it.content,
                    createdAt = (it.time / 1000L).toInt(),
                )
            }
    }

    private fun shouldLoadMore(): Boolean {
        val layoutManager =
            (binding.notificationRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        val adapter = binding.notificationRecyclerView.adapter

        return hasNextPage && !binding.notificationRefresh.isVisible && adapter?.itemCount != 0 &&
                layoutManager == (adapter!!.itemCount - 1) &&
                !binding.notificationRecyclerView.canScrollVertically(1)
    }

    fun onClick(
        id: Int,
        optional: Int?,
        type: NotificationClickType
    ) {
        when (type) {
            NotificationClickType.USER -> {
                ContextCompat.startActivity(
                    requireContext(), Intent(requireContext(), ProfileActivity::class.java)
                        .putExtra("userId", id), null
                )
            }

            NotificationClickType.MEDIA -> {
                ContextCompat.startActivity(
                    requireContext(), Intent(requireContext(), MediaDetailsActivity::class.java)
                        .putExtra("mediaId", id), null
                )
            }

            NotificationClickType.ACTIVITY -> {
                ContextCompat.startActivity(
                    requireContext(), Intent(requireContext(), FeedActivity::class.java)
                        .putExtra("activityId", id), null
                )
            }

            NotificationClickType.COMMENT -> {
                ContextCompat.startActivity(
                    requireContext(), Intent(requireContext(), MediaDetailsActivity::class.java)
                        .putExtra("FRAGMENT_TO_LOAD", "COMMENTS")
                        .putExtra("mediaId", id)
                        .putExtra("commentId", optional ?: -1),
                    null
                )

            }

            NotificationClickType.UNDEFINED -> {
                // Do nothing
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::binding.isInitialized) {
            binding.root.requestLayout()
            binding.root.setBaseline((activity as NotificationActivity).navBar)
        }
    }

    companion object {
        enum class NotificationClickType {
            USER, MEDIA, ACTIVITY, COMMENT, UNDEFINED
        }

        enum class NotificationType {
            MEDIA, USER, SUBSCRIPTION, COMMENT, ONE
        }
    }

}