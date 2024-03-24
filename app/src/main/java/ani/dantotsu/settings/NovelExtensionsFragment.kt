package ani.dantotsu.settings

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.databinding.FragmentNovelExtensionsBinding
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.paging.NovelExtensionAdapter
import ani.dantotsu.settings.paging.NovelExtensionsViewModel
import ani.dantotsu.settings.paging.NovelExtensionsViewModelFactory
import ani.dantotsu.settings.paging.OnNovelInstallClickListener
import ani.dantotsu.snackString
import eu.kanade.tachiyomi.data.notification.Notifications
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NovelExtensionsFragment : Fragment(),
    SearchQueryHandler, OnNovelInstallClickListener {
    private var _binding: FragmentNovelExtensionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NovelExtensionsViewModel by viewModels {
        NovelExtensionsViewModelFactory(novelExtensionManager)
    }

    private val adapter by lazy {
        NovelExtensionAdapter(this)
    }

    private val novelExtensionManager: NovelExtensionManager = Injekt.get()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNovelExtensionsBinding.inflate(inflater, container, false)

        binding.allNovelExtensionsRecyclerView.isNestedScrollingEnabled = false
        binding.allNovelExtensionsRecyclerView.adapter = adapter
        binding.allNovelExtensionsRecyclerView.layoutManager = LinearLayoutManager(context)
        (binding.allNovelExtensionsRecyclerView.layoutManager as LinearLayoutManager).isItemPrefetchEnabled =
            true

        lifecycleScope.launch {
            viewModel.pagerFlow.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }


        viewModel.invalidatePager() // Force a refresh of the pager
        return binding.root
    }

    override fun updateContentBasedOnQuery(query: String?) {
        viewModel.setSearchQuery(query ?: "")
    }

    override fun notifyDataChanged() {
        viewModel.invalidatePager()
    }

    override fun onInstallClick(pkg: NovelExtension.Available) {
        if (isAdded) {  // Check if the fragment is currently added to its activity
            val context = requireContext()
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Start the installation process
            novelExtensionManager.installExtension(pkg)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { installStep ->
                        val builder = NotificationCompat.Builder(
                            context,
                            Notifications.CHANNEL_DOWNLOADER_PROGRESS
                        )
                            .setSmallIcon(R.drawable.ic_round_sync_24)
                            .setContentTitle("Installing extension")
                            .setContentText("Step: $installStep")
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                        notificationManager.notify(1, builder.build())
                    },
                    { error ->
                        Injekt.get<CrashlyticsInterface>().logException(error)
                        val builder = NotificationCompat.Builder(
                            context,
                            Notifications.CHANNEL_DOWNLOADER_ERROR
                        )
                            .setSmallIcon(R.drawable.ic_round_info_24)
                            .setContentTitle("Installation failed: ${error.message}")
                            .setContentText("Error: ${error.message}")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                        notificationManager.notify(1, builder.build())
                        snackString("Installation failed: ${error.message}")
                    },
                    {
                        val builder = NotificationCompat.Builder(
                            context,
                            Notifications.CHANNEL_DOWNLOADER_PROGRESS
                        )
                            .setSmallIcon(R.drawable.ic_download_24)
                            .setContentTitle("Installation complete")
                            .setContentText("The extension has been successfully installed.")
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                        notificationManager.notify(1, builder.build())
                        viewModel.invalidatePager()
                        snackString("Extension installed")
                    }
                )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }


}