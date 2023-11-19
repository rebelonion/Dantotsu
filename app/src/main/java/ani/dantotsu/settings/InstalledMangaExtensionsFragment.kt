package ani.dantotsu.settings


import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.R
import ani.dantotsu.databinding.FragmentMangaExtensionsBinding
import ani.dantotsu.loadData
import ani.dantotsu.settings.extensionprefs.MangaSourcePreferencesFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.source.ConfigurableSource
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class InstalledMangaExtensionsFragment : Fragment() {
    private var _binding: FragmentMangaExtensionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var extensionsRecyclerView: RecyclerView
    val skipIcons = loadData("skip_extension_icons") ?: false
    private val mangaExtensionManager: MangaExtensionManager = Injekt.get()
    private val extensionsAdapter = MangaExtensionsAdapter({ pkg ->
        val changeUIVisibility: (Boolean) -> Unit = { show ->
            val activity = requireActivity() as ExtensionsActivity
            val visibility = if (show) View.VISIBLE else View.GONE
            activity.findViewById<ViewPager2>(R.id.viewPager).visibility = visibility
            activity.findViewById<TabLayout>(R.id.tabLayout).visibility = visibility
            activity.findViewById<TextInputLayout>(R.id.searchView).visibility = visibility
            activity.findViewById<FrameLayout>(R.id.fragmentExtensionsContainer).visibility =
                if (show) View.GONE else View.VISIBLE
        }
        val allSettings = pkg.sources.filterIsInstance<ConfigurableSource>()
        if (allSettings.isNotEmpty()) {
            var selectedSetting = allSettings[0]
            if (allSettings.size > 1) {
                val names = allSettings.map { it.lang }.toTypedArray()
                var selectedIndex = 0
                AlertDialog.Builder(requireContext())
                    .setTitle("Select a Source")
                    .setSingleChoiceItems(names, selectedIndex) { _, which ->
                        selectedIndex = which
                    }
                    .setPositiveButton("OK") { dialog, _ ->
                        selectedSetting = allSettings[selectedIndex]
                        dialog.dismiss()

                        // Move the fragment transaction here
                        val fragment = MangaSourcePreferencesFragment().getInstance(selectedSetting.id){
                            changeUIVisibility(true)
                        }
                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                            .replace(R.id.fragmentExtensionsContainer, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.cancel()
                        changeUIVisibility(true)
                        return@setNegativeButton
                    }
                    .show()
            } else {
                // If there's only one setting, proceed with the fragment transaction
                val fragment = MangaSourcePreferencesFragment().getInstance(selectedSetting.id){
                    changeUIVisibility(true)
                }
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                    .replace(R.id.fragmentExtensionsContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            // Hide ViewPager2 and TabLayout
            changeUIVisibility(false)
        } else {
            Toast.makeText(requireContext(), "Source is not configurable", Toast.LENGTH_SHORT)
                .show()
        }
    },
        { pkg ->
        if (isAdded) {  // Check if the fragment is currently added to its activity
            val context = requireContext()  // Store context in a variable
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager  // Initialize NotificationManager once

            if (pkg.hasUpdate) {
                mangaExtensionManager.updateExtension(pkg)
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
                            FirebaseCrashlytics.getInstance().recordException(error)
                            Log.e("MangaExtensionsAdapter", "Error: ", error)  // Log the error
                            val builder = NotificationCompat.Builder(
                                context,
                                Notifications.CHANNEL_DOWNLOADER_ERROR
                            )
                                .setSmallIcon(R.drawable.ic_round_info_24)
                                .setContentTitle("Update failed: ${error.message}")
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
                mangaExtensionManager.uninstallExtension(pkg.pkgName)
            }
        }
    }, skipIcons)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMangaExtensionsBinding.inflate(inflater, container, false)

        extensionsRecyclerView = binding.allMangaExtensionsRecyclerView
        extensionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        extensionsRecyclerView.adapter = extensionsAdapter


        lifecycleScope.launch {
            mangaExtensionManager.installedExtensionsFlow.collect { extensions ->
                extensionsAdapter.updateData(extensions)
            }
        }
        val extensionsRecyclerView: RecyclerView = binding.allMangaExtensionsRecyclerView
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }


    private class MangaExtensionsAdapter(
        private val onSettingsClicked: (MangaExtension.Installed) -> Unit,
        private val onUninstallClicked: (MangaExtension.Installed) -> Unit,
        skipIcons: Boolean
    ) : ListAdapter<MangaExtension.Installed, MangaExtensionsAdapter.ViewHolder>(
        DIFF_CALLBACK_INSTALLED
    ) {

        val skipIcons = skipIcons

        fun updateData(newExtensions: List<MangaExtension.Installed>) {
            submitList(newExtensions)  // Use submitList instead of manual list handling
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = getItem(position)  // Use getItem() from ListAdapter
            val nsfw = if (extension.isNsfw) {
                "(18+)"
            } else {
                ""
            }
            val lang = when (extension.lang) {
                "all" -> "Multi"
                "ar" -> "Arabic"
                "de" -> "German"
                "en" -> "English"
                "es" -> "Spanish"
                "fr" -> "French"
                "id" -> "Indonesian"
                "it" -> "Italian"
                "ja" -> "Japanese"
                "ko" -> "Korean"
                "pl" -> "Polish"
                "pt-BR" -> "Portuguese (Brazil)"
                "ru" -> "Russian"
                "th" -> "Thai"
                "tr" -> "Turkish"
                "uk" -> "Ukrainian"
                "vi" -> "Vietnamese"
                "zh" -> "Chinese"
                "zh-Hans" -> "Chinese (Simplified)"

                else -> ""
            }
            holder.extensionNameTextView.text = extension.name
            holder.extensionVersionTextView.text = "$lang ${extension.versionName} $nsfw"
            if (!skipIcons) {
                holder.extensionIconImageView.setImageDrawable(extension.icon)
            }
            if (extension.hasUpdate) {
                holder.closeTextView.setImageResource(R.drawable.ic_round_sync_24)
            } else {
                holder.closeTextView.setImageResource(R.drawable.ic_round_delete_24)
            }
            holder.closeTextView.setOnClickListener {
                onUninstallClicked(extension)
            }
            holder.settingsImageView.setOnClickListener {
                onSettingsClicked(extension)
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionVersionTextView: TextView = view.findViewById(R.id.extensionVersionTextView)
            val settingsImageView: ImageView = view.findViewById(R.id.settingsImageView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val closeTextView: ImageView = view.findViewById(R.id.closeTextView)
        }

        companion object {
            val DIFF_CALLBACK_INSTALLED =
                object : DiffUtil.ItemCallback<MangaExtension.Installed>() {
                    override fun areItemsTheSame(
                        oldItem: MangaExtension.Installed,
                        newItem: MangaExtension.Installed
                    ): Boolean {
                        return oldItem.pkgName == newItem.pkgName
                    }

                    override fun areContentsTheSame(
                        oldItem: MangaExtension.Installed,
                        newItem: MangaExtension.Installed
                    ): Boolean {
                        return oldItem == newItem
                    }
                }
        }
    }


}