package ani.dantotsu.settings

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.databinding.FragmentNovelExtensionsBinding
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.NovelSources
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.data.notification.Notifications
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

class InstalledNovelExtensionsFragment : Fragment(), SearchQueryHandler {
    private var _binding: FragmentNovelExtensionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var extensionsRecyclerView: RecyclerView
    private val skipIcons: Boolean = PrefManager.getVal(PrefName.SkipExtensionIcons)
    private val novelExtensionManager: NovelExtensionManager = Injekt.get()
    private val extensionsAdapter = NovelExtensionsAdapter(
        { _ ->
            Toast.makeText(requireContext(), "Source is not configurable", Toast.LENGTH_SHORT)
                .show()
        },
        { pkg ->
            if (isAdded) {
                novelExtensionManager.uninstallExtension(pkg.pkgName)
                snackString("Extension uninstalled")

            }
        },
        { pkg ->
            if (isAdded) {
                val context = requireContext()
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (pkg.hasUpdate) {
                    novelExtensionManager.updateExtension(pkg)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { installStep ->
                                val builder = NotificationCompat.Builder(
                                    context,
                                    Notifications.CHANNEL_DOWNLOADER_PROGRESS
                                )
                                    .setSmallIcon(R.drawable.ic_round_sync_24)
                                    .setContentTitle("Updating extension")
                                    .setContentText("Step: $installStep")
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                notificationManager.notify(1, builder.build())
                            },
                            { error ->
                                Injekt.get<CrashlyticsInterface>().logException(error)
                                Logger.log(error)
                                val builder = NotificationCompat.Builder(
                                    context,
                                    Notifications.CHANNEL_DOWNLOADER_ERROR
                                )
                                    .setSmallIcon(R.drawable.ic_round_info_24)
                                    .setContentTitle("Update failed: ${error.message}")
                                    .setContentText("Error: ${error.message}")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                notificationManager.notify(1, builder.build())
                                snackString("Update failed: ${error.message}")
                            },
                            {
                                val builder = NotificationCompat.Builder(
                                    context,
                                    Notifications.CHANNEL_DOWNLOADER_PROGRESS
                                )
                                    .setSmallIcon(R.drawable.ic_check)
                                    .setContentTitle("Update complete")
                                    .setContentText("The extension has been successfully updated.")
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                notificationManager.notify(1, builder.build())
                                snackString("Extension updated")
                            }
                        )
                } else {
                    snackString("No update available")
                }
            }
        }, skipIcons
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNovelExtensionsBinding.inflate(inflater, container, false)

        extensionsRecyclerView = binding.allNovelExtensionsRecyclerView
        extensionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        extensionsRecyclerView.adapter = extensionsAdapter

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.absoluteAdapterPosition
                val toPosition = target.absoluteAdapterPosition
                val newList = extensionsAdapter.currentList.toMutableList().apply {
                    add(toPosition, removeAt(fromPosition))
                }
                extensionsAdapter.submitList(newList)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.elevation = 8f
                    viewHolder?.itemView?.translationZ = 8f
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                extensionsAdapter.updatePref()
                viewHolder.itemView.elevation = 0f
                viewHolder.itemView.translationZ = 0f
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(extensionsRecyclerView)


        lifecycleScope.launch {
            novelExtensionManager.installedExtensionsFlow.collect { extensions ->
                extensionsAdapter.updateData(sortToNovelSourcesList(extensions))
            }
        }
        return binding.root
    }

    private fun sortToNovelSourcesList(inpt: List<NovelExtension.Installed>): List<NovelExtension.Installed> {
        val sourcesMap = inpt.associateBy { it.name }
        val orderedSources = NovelSources.pinnedNovelSources.mapNotNull { name ->
            sourcesMap[name]
        }
        return orderedSources + inpt.filter { !NovelSources.pinnedNovelSources.contains(it.name) }
    }


    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    override fun updateContentBasedOnQuery(query: String?) {
        extensionsAdapter.filter(
            query ?: "",
            sortToNovelSourcesList(novelExtensionManager.installedExtensionsFlow.value)
        )
    }

    override fun notifyDataChanged() { // do nothing
    }

    private class NovelExtensionsAdapter(
        private val onSettingsClicked: (NovelExtension.Installed) -> Unit,
        private val onUninstallClicked: (NovelExtension.Installed) -> Unit,
        private val onUpdateClicked: (NovelExtension.Installed) -> Unit,
        val skipIcons: Boolean
    ) : ListAdapter<NovelExtension.Installed, NovelExtensionsAdapter.ViewHolder>(
        DIFF_CALLBACK_INSTALLED
    ) {

        fun updateData(newExtensions: List<NovelExtension.Installed>) {
            submitList(newExtensions)
        }

        fun updatePref() {
            val map = currentList.map { it.name }
            PrefManager.setVal(PrefName.NovelSourcesOrder, map)
            NovelSources.pinnedNovelSources = map
            NovelSources.performReorderNovelSources()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension, parent, false)
            Logger.log("onCreateViewHolder: $view")
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = getItem(position)  // Use getItem() from ListAdapter
            val nsfw = ""
            val lang = LanguageMapper.getLanguageName("all")
            holder.extensionNameTextView.text = extension.name
            val versionText = "$lang ${extension.versionName} $nsfw"
            holder.extensionVersionTextView.text = versionText
            if (!skipIcons) {
                holder.extensionIconImageView.setImageDrawable(extension.icon)
            }
            if (extension.hasUpdate) {
                holder.updateView.isVisible = true
            } else {
                holder.updateView.isVisible = false
            }
            holder.deleteView.setOnClickListener {
                onUninstallClicked(extension)
            }
            holder.updateView.setOnClickListener {
                onUpdateClicked(extension)
            }
            holder.settingsImageView.setOnClickListener {
                onSettingsClicked(extension)
            }
        }

        fun filter(query: String, currentList: List<NovelExtension.Installed>) {
            val filteredList = ArrayList<NovelExtension.Installed>()
            for (extension in currentList) {
                if (extension.name.lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT))) {
                    filteredList.add(extension)
                }
            }
            if (filteredList != currentList)
                submitList(filteredList)
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionVersionTextView: TextView =
                view.findViewById(R.id.extensionVersionTextView)
            val settingsImageView: ImageView = view.findViewById(R.id.settingsImageView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val deleteView: ImageView = view.findViewById(R.id.deleteTextView)
            val updateView: ImageView = view.findViewById(R.id.updateTextView)
        }

        companion object {
            val DIFF_CALLBACK_INSTALLED =
                object : DiffUtil.ItemCallback<NovelExtension.Installed>() {
                    override fun areItemsTheSame(
                        oldItem: NovelExtension.Installed,
                        newItem: NovelExtension.Installed
                    ): Boolean {
                        return oldItem.pkgName == newItem.pkgName
                    }

                    override fun areContentsTheSame(
                        oldItem: NovelExtension.Installed,
                        newItem: NovelExtension.Installed
                    ): Boolean {
                        return oldItem == newItem
                    }
                }
        }
    }


}