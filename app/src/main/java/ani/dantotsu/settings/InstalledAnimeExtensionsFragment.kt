package ani.dantotsu.settings

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.R
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.databinding.FragmentAnimeExtensionsBinding
import ani.dantotsu.util.Logger
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.settings.extensionprefs.AnimeSourcePreferencesFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Collections
import java.util.Locale


class InstalledAnimeExtensionsFragment : Fragment(), SearchQueryHandler {


    private var _binding: FragmentAnimeExtensionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var extensionsRecyclerView: RecyclerView
    private val skipIcons: Boolean = PrefManager.getVal(PrefName.SkipExtensionIcons)
    private val animeExtensionManager: AnimeExtensionManager = Injekt.get()
    private val extensionsAdapter = AnimeExtensionsAdapter(
        { pkg ->
            val name = pkg.name
            val changeUIVisibility: (Boolean) -> Unit = { show ->
                val activity = requireActivity() as ExtensionsActivity
                val visibility = if (show) View.VISIBLE else View.GONE
                activity.findViewById<ViewPager2>(R.id.viewPager).visibility = visibility
                activity.findViewById<TabLayout>(R.id.tabLayout).visibility = visibility
                activity.findViewById<TextInputLayout>(R.id.searchView).visibility = visibility
                activity.findViewById<ImageView>(R.id.languageselect).visibility = visibility
                activity.findViewById<TextView>(R.id.extensions).text =
                    if (show) getString(R.string.extensions) else name
                activity.findViewById<FrameLayout>(R.id.fragmentExtensionsContainer).visibility =
                    if (show) View.GONE else View.VISIBLE
            }
            var itemSelected = false
            val allSettings = pkg.sources.filterIsInstance<ConfigurableAnimeSource>()
            if (allSettings.isNotEmpty()) {
                var selectedSetting = allSettings[0]
                if (allSettings.size > 1) {
                    val names = allSettings.map { LanguageMapper.mapLanguageCodeToName(it.lang) }
                        .toTypedArray()
                    var selectedIndex = 0
                    val dialog = AlertDialog.Builder(requireContext(), R.style.MyPopup)
                        .setTitle("Select a Source")
                        .setSingleChoiceItems(names, selectedIndex) { dialog, which ->
                            itemSelected = true
                            selectedIndex = which
                            selectedSetting = allSettings[selectedIndex]
                            dialog.dismiss()

                            val fragment =
                                AnimeSourcePreferencesFragment().getInstance(selectedSetting.id) {
                                    changeUIVisibility(true)
                                }
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_up, R.anim.slide_down)
                                .replace(R.id.fragmentExtensionsContainer, fragment)
                                .addToBackStack(null)
                                .commit()
                        }
                        .setOnDismissListener {
                            if (!itemSelected) {
                                changeUIVisibility(true)
                            }
                        }
                        .show()
                    dialog.window?.setDimAmount(0.8f)
                } else {
                    // If there's only one setting, proceed with the fragment transaction
                    val fragment =
                        AnimeSourcePreferencesFragment().getInstance(selectedSetting.id) {
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
        { pkg, forceDelete ->
            if (isAdded) {  // Check if the fragment is currently added to its activity
                val context = requireContext()  // Store context in a variable
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager  // Initialize NotificationManager once

                if (pkg.hasUpdate && !forceDelete) {
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
                                Injekt.get<CrashlyticsInterface>().logException(error)
                                Logger.log(error)  // Log the error
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
                                    .setSmallIcon(R.drawable.ic_circle_check)
                                    .setContentTitle("Update complete")
                                    .setContentText("The extension has been successfully updated.")
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                notificationManager.notify(1, builder.build())
                                snackString("Extension updated")
                            }
                        )
                } else {
                    animeExtensionManager.uninstallExtension(pkg.pkgName)
                    snackString("Extension uninstalled")
                }
            }
        }, skipIcons
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeExtensionsBinding.inflate(inflater, container, false)

        extensionsRecyclerView = binding.allAnimeExtensionsRecyclerView
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
                val newList = extensionsAdapter.currentList.toMutableList()
                val fromPosition = viewHolder.absoluteAdapterPosition
                val toPosition = target.absoluteAdapterPosition
                if (fromPosition < toPosition) { //probably need to switch to a recyclerview adapter
                    for (i in fromPosition until toPosition) {
                        Collections.swap(newList, i, i + 1)
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(newList, i, i - 1)
                    }
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
            animeExtensionManager.installedExtensionsFlow.collect { extensions ->
                extensionsAdapter.updateData(sortToAnimeSourcesList(extensions))
            }
        }
        return binding.root
    }


    private fun sortToAnimeSourcesList(inpt: List<AnimeExtension.Installed>): List<AnimeExtension.Installed> {
        val sourcesMap = inpt.associateBy { it.name }
        val orderedSources = AnimeSources.pinnedAnimeSources.mapNotNull { name ->
            sourcesMap[name]
        }
        return orderedSources + inpt.filter { !AnimeSources.pinnedAnimeSources.contains(it.name) }
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    override fun updateContentBasedOnQuery(query: String?) {
        extensionsAdapter.filter(
            query ?: "",
            sortToAnimeSourcesList(animeExtensionManager.installedExtensionsFlow.value)
        )
    }

    override fun notifyDataChanged() { // Do nothing
    }

    private class AnimeExtensionsAdapter(
        private val onSettingsClicked: (AnimeExtension.Installed) -> Unit,
        private val onUninstallClicked: (AnimeExtension.Installed, Boolean) -> Unit,
        val skipIcons: Boolean
    ) : ListAdapter<AnimeExtension.Installed, AnimeExtensionsAdapter.ViewHolder>(
        DIFF_CALLBACK_INSTALLED
    ) {

        fun updateData(newExtensions: List<AnimeExtension.Installed>) {
            submitList(newExtensions)
        }

        fun updatePref() {
            val map = currentList.map { it.name }
            PrefManager.setVal(PrefName.AnimeSourcesOrder, map)
            AnimeSources.pinnedAnimeSources = map
            AnimeSources.performReorderAnimeSources()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = getItem(position)
            val nsfw = if (extension.isNsfw) "(18+)" else ""
            val lang = LanguageMapper.mapLanguageCodeToName(extension.lang)
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
                onUninstallClicked(extension, false)
            }
            holder.settingsImageView.setOnClickListener {
                onSettingsClicked(extension)
            }
            holder.closeTextView.setOnLongClickListener {
                onUninstallClicked(extension, true)
                true
            }
        }

        fun filter(query: String, currentList: List<AnimeExtension.Installed>) {
            val filteredList = ArrayList<AnimeExtension.Installed>()
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
            val closeTextView: ImageView = view.findViewById(R.id.closeTextView)
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

}