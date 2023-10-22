package ani.dantotsu.settings

import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.FragmentAnimeExtensionsBinding
import ani.dantotsu.loadData
import com.bumptech.glide.Glide
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionsFragment : Fragment(),
    SearchQueryHandler {
    private var _binding: FragmentAnimeExtensionsBinding? = null
    private val binding get() = _binding!!

    val skipIcons = loadData("skip_extension_icons") ?: false

    private lateinit var extensionsRecyclerView: RecyclerView
    private lateinit var allextenstionsRecyclerView: RecyclerView
    private val animeExtensionManager: AnimeExtensionManager = Injekt.get<AnimeExtensionManager>()
    private val extensionsAdapter = AnimeExtensionsAdapter({ pkg ->
        if (isAdded) {  // Check if the fragment is currently added to its activity
            val context = requireContext()  // Store context in a variable
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager  // Initialize NotificationManager once

            if (pkg.hasUpdate) {
                animeExtensionManager.updateExtension(pkg)
                    .observeOn(AndroidSchedulers.mainThread())  // Observe on main thread
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
                            Log.e("AnimeExtensionsAdapter", "Error: ", error)  // Log the error
                            val builder = NotificationCompat.Builder(
                                context,
                                Notifications.CHANNEL_DOWNLOADER_ERROR
                            )
                                .setSmallIcon(R.drawable.ic_round_info_24)
                                .setContentTitle("Update failed")
                                .setContentText("Error: ${error.message}")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                            notificationManager.notify(1, builder.build())
                        },
                        {
                            val builder = NotificationCompat.Builder(
                                context,
                                Notifications.CHANNEL_DOWNLOADER_PROGRESS
                            )
                                .setSmallIcon(androidx.media3.ui.R.drawable.exo_ic_check)
                                .setContentTitle("Update complete")
                                .setContentText("The extension has been successfully updated.")
                                .setPriority(NotificationCompat.PRIORITY_LOW)
                            notificationManager.notify(1, builder.build())
                        }
                    )
            } else {
                animeExtensionManager.uninstallExtension(pkg.pkgName)
            }
        }
    }, skipIcons)

    private val allExtensionsAdapter = AllAnimeExtensionsAdapter(lifecycleScope, { pkgName ->

        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Start the installation process
        animeExtensionManager.installExtension(pkgName)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { installStep ->
                    val builder = NotificationCompat.Builder(
                        requireContext(),
                        Notifications.CHANNEL_DOWNLOADER_PROGRESS
                    )
                        .setSmallIcon(R.drawable.ic_round_sync_24)
                        .setContentTitle("Installing extension")
                        .setContentText("Step: $installStep")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                    notificationManager.notify(1, builder.build())
                },
                { error ->
                    val builder = NotificationCompat.Builder(
                        requireContext(),
                        Notifications.CHANNEL_DOWNLOADER_ERROR
                    )
                        .setSmallIcon(R.drawable.ic_round_info_24)
                        .setContentTitle("Installation failed")
                        .setContentText("Error: ${error.message}")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                    notificationManager.notify(1, builder.build())
                },
                {
                    val builder = NotificationCompat.Builder(
                        requireContext(),
                        Notifications.CHANNEL_DOWNLOADER_PROGRESS
                    )
                        .setSmallIcon(androidx.media3.ui.R.drawable.exo_ic_check)
                        .setContentTitle("Installation complete")
                        .setContentText("The extension has been successfully installed.")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                    notificationManager.notify(1, builder.build())
                }
            )
    }, skipIcons)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeExtensionsBinding.inflate(inflater, container, false)

        extensionsRecyclerView = binding.animeExtensionsRecyclerView
        extensionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        extensionsRecyclerView.adapter = extensionsAdapter

        allextenstionsRecyclerView = binding.allAnimeExtensionsRecyclerView
        allextenstionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        allextenstionsRecyclerView.adapter = allExtensionsAdapter

        lifecycleScope.launch {
            animeExtensionManager.installedExtensionsFlow.collect { extensions ->
                extensionsAdapter.updateData(extensions)
            }
        }
        lifecycleScope.launch {
            combine(
                animeExtensionManager.availableExtensionsFlow,
                animeExtensionManager.installedExtensionsFlow
            ) { availableExtensions, installedExtensions ->
                // Pair of available and installed extensions
                Pair(availableExtensions, installedExtensions)
            }.collect { pair ->
                val (availableExtensions, installedExtensions) = pair

                allExtensionsAdapter.updateData(availableExtensions, installedExtensions)
            }
        }
        val extensionsRecyclerView: RecyclerView = binding.animeExtensionsRecyclerView
        return binding.root
    }

    override fun updateContentBasedOnQuery(query: String?) {
        if (query.isNullOrEmpty()) {
            allExtensionsAdapter.filter("")  // Reset the filter
            allextenstionsRecyclerView.visibility = View.VISIBLE
            extensionsRecyclerView.visibility = View.VISIBLE
        } else {
            allExtensionsAdapter.filter(query)
            allextenstionsRecyclerView.visibility = View.VISIBLE
            extensionsRecyclerView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }


    private class AnimeExtensionsAdapter(
        private val onUninstallClicked: (AnimeExtension.Installed) -> Unit,
        skipIcons: Boolean
    ) : ListAdapter<AnimeExtension.Installed, AnimeExtensionsAdapter.ViewHolder>(
        DIFF_CALLBACK_INSTALLED
    ) {

        val skipIcons = skipIcons

        fun updateData(newExtensions: List<AnimeExtension.Installed>) {
            submitList(newExtensions)  // Use submitList instead of manual list handling
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = getItem(position)  // Use getItem() from ListAdapter
            holder.extensionNameTextView.text = extension.name
            if (!skipIcons) {
                holder.extensionIconImageView.setImageDrawable(extension.icon)
            }
            if (extension.hasUpdate) {
                holder.closeTextView.text = "Update"
                holder.closeTextView.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.warning
                    )
                )
            } else {
                holder.closeTextView.text = "Uninstall"
            }
            holder.closeTextView.setOnClickListener {
                onUninstallClicked(extension)
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val closeTextView: TextView = view.findViewById(R.id.closeTextView)
        }

        companion object {
            val DIFF_CALLBACK_INSTALLED =
                object : DiffUtil.ItemCallback<AnimeExtension.Installed>() {
                    override fun areItemsTheSame(
                        oldItem: AnimeExtension.Installed,
                        newItem: AnimeExtension.Installed
                    ): Boolean {
                        return oldItem.pkgName == newItem.pkgName
                    }

                    override fun areContentsTheSame(
                        oldItem: AnimeExtension.Installed,
                        newItem: AnimeExtension.Installed
                    ): Boolean {
                        return oldItem == newItem
                    }
                }
        }
    }


    private class AllAnimeExtensionsAdapter(
        private val coroutineScope: CoroutineScope,
        private val onButtonClicked: (AnimeExtension.Available) -> Unit,
        skipIcons: Boolean
    ) : ListAdapter<AnimeExtension.Available, AllAnimeExtensionsAdapter.ViewHolder>(
        DIFF_CALLBACK_AVAILABLE
    ) {
        val skipIcons = skipIcons

        fun updateData(
            newExtensions: List<AnimeExtension.Available>,
            installedExtensions: List<AnimeExtension.Installed> = emptyList()
        ) {
            coroutineScope.launch(Dispatchers.Default) {
                val installedPkgNames = installedExtensions.map { it.pkgName }.toSet()
                val filteredExtensions = newExtensions.filter { it.pkgName !in installedPkgNames }

                // Switch back to main thread to update UI
                withContext(Dispatchers.Main) {
                    submitList(filteredExtensions)
                }
            }
        }


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AllAnimeExtensionsAdapter.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension_all, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = getItem(position)

            holder.extensionNameTextView.text = extension.name

            if (!skipIcons) {
                Glide.with(holder.itemView.context)
                    .load(extension.iconUrl)
                    .into(holder.extensionIconImageView)
            }

            holder.closeTextView.text = "Install"
            holder.closeTextView.setOnClickListener {
                onButtonClicked(extension)
            }
        }

        fun filter(query: String) {
            val filteredExtensions = if (query.isEmpty()) {
                currentList
            } else {
                currentList.filter { it.name.contains(query, ignoreCase = true) }
            }
            submitList(filteredExtensions)
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val closeTextView: TextView = view.findViewById(R.id.closeTextView)
        }

        companion object {
            val DIFF_CALLBACK_AVAILABLE =
                object : DiffUtil.ItemCallback<AnimeExtension.Available>() {
                    override fun areItemsTheSame(
                        oldItem: AnimeExtension.Available,
                        newItem: AnimeExtension.Available
                    ): Boolean {
                        return oldItem.pkgName == newItem.pkgName
                    }

                    override fun areContentsTheSame(
                        oldItem: AnimeExtension.Available,
                        newItem: AnimeExtension.Available
                    ): Boolean {
                        return oldItem == newItem
                    }
                }
        }
    }

}