package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.*
import android.os.Build.VERSION.*
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.*
import ani.dantotsu.aniyomi.anime.AnimeExtensionManager
import ani.dantotsu.aniyomi.anime.model.AnimeExtension
import ani.dantotsu.databinding.ActivityExtensionsBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.injectLazy


class ExtensionsActivity : AppCompatActivity() {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@ExtensionsActivity)
    }
    lateinit var binding: ActivityExtensionsBinding
    private lateinit var extensionsRecyclerView: RecyclerView
    private lateinit var allextenstionsRecyclerView: RecyclerView
    private val animeExtensionManager: AnimeExtensionManager by injectLazy()
    private val extensionsAdapter = ExtensionsAdapter { pkgName ->
        animeExtensionManager.uninstallExtension(pkgName)
    }
    private val allExtensionsAdapter = AllExtensionsAdapter(lifecycleScope) { pkgName ->

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Start the installation process
        animeExtensionManager.installExtension(pkgName)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { installStep ->
                    val builder = NotificationCompat.Builder(this,
                        ani.dantotsu.aniyomi.data.Notifications.CHANNEL_DOWNLOADER_PROGRESS
                    )
                        .setSmallIcon(R.drawable.ic_round_sync_24)
                        .setContentTitle("Installing extension")
                        .setContentText("Step: $installStep")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                    notificationManager.notify(1, builder.build())
                },
                { error ->
                    val builder = NotificationCompat.Builder(this,
                        ani.dantotsu.aniyomi.data.Notifications.CHANNEL_DOWNLOADER_ERROR
                    )
                        .setSmallIcon(R.drawable.ic_round_info_24)
                        .setContentTitle("Installation failed")
                        .setContentText("Error: ${error.message}")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                    notificationManager.notify(1, builder.build())
                },
                {
                    val builder = NotificationCompat.Builder(this,
                        ani.dantotsu.aniyomi.data.Notifications.CHANNEL_DOWNLOADER_PROGRESS)
                        .setSmallIcon(androidx.media3.ui.R.drawable.exo_ic_check)
                        .setContentTitle("Installation complete")
                        .setContentText("The extension has been successfully installed.")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                    notificationManager.notify(1, builder.build())
                }
            )
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtensionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extensionsRecyclerView = findViewById(R.id.extensionsRecyclerView)
        extensionsRecyclerView.layoutManager = LinearLayoutManager(this)
        extensionsRecyclerView.adapter = extensionsAdapter

        allextenstionsRecyclerView = findViewById(R.id.allExtensionsRecyclerView)
        allextenstionsRecyclerView.layoutManager = LinearLayoutManager(this)
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


        val searchView: SearchView = findViewById(R.id.searchView)
        val extensionsRecyclerView: RecyclerView = findViewById(R.id.extensionsRecyclerView)
        val extensionsHeader: LinearLayout = findViewById(R.id.extensionsHeader)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    allExtensionsAdapter.filter("")  // Reset the filter
                    allextenstionsRecyclerView.visibility = View.VISIBLE
                    extensionsHeader.visibility = View.VISIBLE
                    extensionsRecyclerView.visibility = View.VISIBLE
                } else {
                    allExtensionsAdapter.filter(newText)
                    allextenstionsRecyclerView.visibility = View.VISIBLE
                    extensionsRecyclerView.visibility = View.GONE
                    extensionsHeader.visibility = View.GONE
                }
                return true
            }
        })


        initActivity(this)



        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        onBackPressedDispatcher.addCallback(this, restartMainActivity)

        binding.settingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }

    private class ExtensionsAdapter(private val onUninstallClicked: (String) -> Unit) : RecyclerView.Adapter<ExtensionsAdapter.ViewHolder>() {

        private var extensions: List<AnimeExtension.Installed> = emptyList()

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
            holder.extensionIconImageView.setImageDrawable(extension.icon)
            holder.closeTextView.text = "Uninstall"
            holder.closeTextView.setOnClickListener {
                onUninstallClicked(extension.pkgName)
            }
        }

        override fun getItemCount(): Int = extensions.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val closeTextView: TextView = view.findViewById(R.id.closeTextView)
        }
    }

    private class AllExtensionsAdapter(private val coroutineScope: CoroutineScope,
                                       private val onButtonClicked: (AnimeExtension.Available) -> Unit) : RecyclerView.Adapter<AllExtensionsAdapter.ViewHolder>() {
        private var extensions: List<AnimeExtension.Available> = emptyList()

        fun updateData(newExtensions: List<AnimeExtension.Available>, installedExtensions: List<AnimeExtension.Installed> = emptyList()) {
            val installedPkgNames = installedExtensions.map { it.pkgName }.toSet()
            extensions = newExtensions.filter { it.pkgName !in installedPkgNames }
            filteredExtensions = extensions
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllExtensionsAdapter.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension_all, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = filteredExtensions[position]
            holder.extensionNameTextView.text = extension.name
            coroutineScope.launch {
                val drawable = urlToDrawable(holder.itemView.context, extension.iconUrl)
                holder.extensionIconImageView.setImageDrawable(drawable)
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