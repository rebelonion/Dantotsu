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
import androidx.recyclerview.widget.LinearLayoutManager
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

class AnimeExtensionsFragment : Fragment(), SearchQueryHandler {
    private var _binding: FragmentAnimeExtensionsBinding? = null
    private val binding get() = _binding!!

    val skipIcons = loadData("skip_extension_icons") ?: false

    private lateinit var extensionsRecyclerView: RecyclerView
    private lateinit var allextenstionsRecyclerView: RecyclerView
    private val animeExtensionManager: AnimeExtensionManager = Injekt.get<AnimeExtensionManager>()
    private val extensionsAdapter = AnimeExtensionsAdapter ({ pkg ->
        if(pkg.hasUpdate){
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            animeExtensionManager.updateExtension(pkg)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { installStep ->
                        val builder = NotificationCompat.Builder(
                            requireContext(),
                            Notifications.CHANNEL_DOWNLOADER_PROGRESS
                        )
                            .setSmallIcon(R.drawable.ic_round_sync_24)
                            .setContentTitle("Updating extension")
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
                            .setContentTitle("Update failed")
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
                            .setContentTitle("Update complete")
                            .setContentText("The extension has been successfully updated.")
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                        notificationManager.notify(1, builder.build())
                    }
                )
            }else {
            animeExtensionManager.uninstallExtension(pkg.pkgName)
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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnimeExtensionsBinding.inflate(inflater, container, false)

        extensionsRecyclerView = binding.animeExtensionsRecyclerView
        extensionsRecyclerView.layoutManager = LinearLayoutManager( requireContext())
        extensionsRecyclerView.adapter = extensionsAdapter

        allextenstionsRecyclerView = binding.allAnimeExtensionsRecyclerView
        allextenstionsRecyclerView.layoutManager = LinearLayoutManager( requireContext())
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
            println("asdf: ${allExtensionsAdapter.getItemCount()}")
        } else {
            allExtensionsAdapter.filter(query)
            allextenstionsRecyclerView.visibility = View.VISIBLE
            extensionsRecyclerView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }


    private class AnimeExtensionsAdapter(private val onUninstallClicked: (AnimeExtension.Installed) -> Unit, skipIcons: Boolean) : RecyclerView.Adapter<AnimeExtensionsAdapter.ViewHolder>() {

        private var extensions: List<AnimeExtension.Installed> = emptyList()
        val skipIcons = skipIcons

        fun updateData(newExtensions: List<AnimeExtension.Installed>) {
            extensions = newExtensions
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = extensions[position]
            holder.extensionNameTextView.text = extension.name
            if (!skipIcons) {
                holder.extensionIconImageView.setImageDrawable(extension.icon)
            }
            if(extension.hasUpdate){
                holder.closeTextView.text = "Update"
                holder.closeTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.warning))
            }else{
                holder.closeTextView.text = "Uninstall"
            }
            holder.closeTextView.setOnClickListener {
                onUninstallClicked(extension)
            }
        }

        override fun getItemCount(): Int = extensions.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val closeTextView: TextView = view.findViewById(R.id.closeTextView)
        }
    }

    private class AllAnimeExtensionsAdapter(private val coroutineScope: CoroutineScope,
                                       private val onButtonClicked: (AnimeExtension.Available) -> Unit, skipIcons: Boolean) : RecyclerView.Adapter<AllAnimeExtensionsAdapter.ViewHolder>() {
        private var extensions: List<AnimeExtension.Available> = emptyList()
        val skipIcons = skipIcons

        fun updateData(newExtensions: List<AnimeExtension.Available>, installedExtensions: List<AnimeExtension.Installed> = emptyList()) {
            val installedPkgNames = installedExtensions.map { it.pkgName }.toSet()
            extensions = newExtensions.filter { it.pkgName !in installedPkgNames }
            filteredExtensions = extensions
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllAnimeExtensionsAdapter.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension_all, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = filteredExtensions[position]
            holder.extensionNameTextView.text = extension.name
            if (!skipIcons) {
                coroutineScope.launch {
                    val drawable = urlToDrawable(holder.itemView.context, extension.iconUrl)
                    holder.extensionIconImageView.setImageDrawable(drawable)
                }
            }
            holder.closeTextView.text = "Install"
            holder.closeTextView.setOnClickListener {
                onButtonClicked(extension)
            }
        }

        override fun getItemCount(): Int = filteredExtensions.size

        private var filteredExtensions: List<AnimeExtension.Available> = emptyList()

        fun filter(query: String) {
            filteredExtensions = if (query.isEmpty()) {
                extensions
            } else {
                extensions.filter { it.name.contains(query, ignoreCase = true) }
            }
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val closeTextView: TextView = view.findViewById(R.id.closeTextView)
        }

        suspend fun urlToDrawable(context: Context, url: String): Drawable? {
            return withContext(Dispatchers.IO) {
                try {
                    return@withContext Glide.with(context)
                        .load(url)
                        .submit()
                        .get()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }
    }
}