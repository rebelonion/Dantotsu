package ani.dantotsu.settings

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.Anilist.activityMergeTimeMap
import ani.dantotsu.connections.anilist.Anilist.rowOrderMap
import ani.dantotsu.connections.anilist.Anilist.scoreFormats
import ani.dantotsu.connections.anilist.Anilist.staffNameLang
import ani.dantotsu.connections.anilist.Anilist.titleLang
import ani.dantotsu.connections.anilist.AnilistMutations
import ani.dantotsu.connections.anilist.api.ScoreFormat
import ani.dantotsu.connections.anilist.api.UserStaffNameLanguage
import ani.dantotsu.connections.anilist.api.UserTitleLanguage
import ani.dantotsu.databinding.ActivitySettingsAnilistBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.customAlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AnilistSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsAnilistBinding
    private lateinit var anilistMutations: AnilistMutations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsAnilistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        anilistMutations = AnilistMutations()

        binding.apply {
            settingsAnilistLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            binding.anilistSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            val currentTitleLang = Anilist.titleLanguage
            val titleFormat = UserTitleLanguage.entries.firstOrNull { it.name == currentTitleLang }
                ?: UserTitleLanguage.ENGLISH

            settingsAnilistTitleLanguage.setText(titleLang[titleFormat.ordinal])
            settingsAnilistTitleLanguage.setAdapter(
                ArrayAdapter(context, R.layout.item_dropdown, titleLang)
            )
            settingsAnilistTitleLanguage.setOnItemClickListener { _, _, i, _ ->
                val selectedLanguage = when (i) {
                    0 -> "ENGLISH"
                    1 -> "ROMAJI"
                    2 -> "NATIVE"
                    else -> "ENGLISH"
                }
                lifecycleScope.launch {
                    anilistMutations.updateSettings(titleLanguage = selectedLanguage)
                    Anilist.titleLanguage = selectedLanguage
                    restartApp()
                }
                settingsAnilistTitleLanguage.clearFocus()
            }

            val currentStaffNameLang = Anilist.staffNameLanguage
            val staffNameFormat =
                UserStaffNameLanguage.entries.firstOrNull { it.name == currentStaffNameLang }
                    ?: UserStaffNameLanguage.ROMAJI_WESTERN

            settingsAnilistStaffLanguage.setText(staffNameLang[staffNameFormat.ordinal])
            settingsAnilistStaffLanguage.setAdapter(
                ArrayAdapter(context, R.layout.item_dropdown, staffNameLang)
            )
            settingsAnilistStaffLanguage.setOnItemClickListener { _, _, i, _ ->
                val selectedLanguage = when (i) {
                    0 -> "ROMAJI_WESTERN"
                    1 -> "ROMAJI"
                    2 -> "NATIVE"
                    else -> "ROMAJI_WESTERN"
                }
                lifecycleScope.launch {
                    anilistMutations.updateSettings(staffNameLanguage = selectedLanguage)
                    Anilist.staffNameLanguage = selectedLanguage
                    restartApp()
                }
                settingsAnilistStaffLanguage.clearFocus()
            }

            val currentMergeTimeDisplay =
                activityMergeTimeMap.entries.firstOrNull { it.value == Anilist.activityMergeTime }?.key
                    ?: "${Anilist.activityMergeTime} mins"
            settingsAnilistActivityMergeTime.setText(currentMergeTimeDisplay)
            settingsAnilistActivityMergeTime.setAdapter(
                ArrayAdapter(context, R.layout.item_dropdown, activityMergeTimeMap.keys.toList())
            )
            settingsAnilistActivityMergeTime.setOnItemClickListener { _, _, i, _ ->
                val selectedDisplayTime = activityMergeTimeMap.keys.toList()[i]
                val selectedApiTime = activityMergeTimeMap[selectedDisplayTime] ?: 0
                lifecycleScope.launch {
                    anilistMutations.updateSettings(activityMergeTime = selectedApiTime)
                    Anilist.activityMergeTime = selectedApiTime
                    restartApp()
                }
                settingsAnilistActivityMergeTime.clearFocus()
            }

            val currentScoreFormat = Anilist.scoreFormat
            val scoreFormat = ScoreFormat.entries.firstOrNull { it.name == currentScoreFormat }
                ?: ScoreFormat.POINT_100
            settingsAnilistScoreFormat.setText(scoreFormats[scoreFormat.ordinal])
            settingsAnilistScoreFormat.setAdapter(
                ArrayAdapter(context, R.layout.item_dropdown, scoreFormats)
            )
            settingsAnilistScoreFormat.setOnItemClickListener { _, _, i, _ ->
                val selectedFormat = when (i) {
                    0 -> "POINT_100"
                    1 -> "POINT_10_DECIMAL"
                    2 -> "POINT_10"
                    3 -> "POINT_5"
                    4 -> "POINT_3"
                    else -> "POINT_100"
                }
                lifecycleScope.launch {
                    anilistMutations.updateSettings(scoreFormat = selectedFormat)
                    Anilist.scoreFormat = selectedFormat
                    restartApp()
                }
                settingsAnilistScoreFormat.clearFocus()
            }

            val currentRowOrder =
                rowOrderMap.entries.firstOrNull { it.value == Anilist.rowOrder }?.key ?: "Score"
            settingsAnilistRowOrder.setText(currentRowOrder)
            settingsAnilistRowOrder.setAdapter(
                ArrayAdapter(context, R.layout.item_dropdown, rowOrderMap.keys.toList())
            )
            settingsAnilistRowOrder.setOnItemClickListener { _, _, i, _ ->
                val selectedDisplayOrder = rowOrderMap.keys.toList()[i]
                val selectedApiOrder = rowOrderMap[selectedDisplayOrder] ?: "score"
                lifecycleScope.launch {
                    anilistMutations.updateSettings(rowOrder = selectedApiOrder)
                    Anilist.rowOrder = selectedApiOrder
                    restartApp()
                }
                settingsAnilistRowOrder.clearFocus()
            }

            val containers =
                listOf(binding.animeCustomListsContainer, binding.mangaCustomListsContainer)
            val customLists = listOf(Anilist.animeCustomLists, Anilist.mangaCustomLists)
            val buttons = listOf(binding.addAnimeListButton, binding.addMangaListButton)

            containers.forEachIndexed { index, container ->
                customLists[index]?.forEach { listName ->
                    addCustomListItem(listName, container, index == 0)
                }
            }

            buttons.forEachIndexed { index, button ->
                button.setOnClickListener {
                    addCustomListItem("", containers[index], index == 0)
                }
            }

            binding.SettingsAnilistCustomListSave.setOnClickListener {
                saveCustomLists()
            }

            val currentTimezone = Anilist.timezone?.let { Anilist.getDisplayTimezone(it, context) }
                ?: context.getString(R.string.selected_no_time_zone)
            settingsAnilistTimezone.setText(currentTimezone)
            settingsAnilistTimezone.setAdapter(
                ArrayAdapter(context, R.layout.item_dropdown, Anilist.timeZone)
            )
            settingsAnilistTimezone.setOnItemClickListener { _, _, i, _ ->
                val selectedTimezone = Anilist.timeZone[i]
                val apiTimezone = Anilist.getApiTimezone(selectedTimezone)
                lifecycleScope.launch {
                    anilistMutations.updateSettings(timezone = apiTimezone)
                    Anilist.timezone = apiTimezone
                    restartApp()
                }
                settingsAnilistTimezone.clearFocus()
            }

            val displayAdultContent = Anilist.adult
            val airingNotifications = Anilist.airingNotifications

            binding.settingsRecyclerView1.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 2,
                        name = getString(R.string.airing_notifications),
                        desc = getString(R.string.airing_notifications_desc),
                        icon = R.drawable.ic_round_notifications_active_24,
                        isChecked = airingNotifications,
                        switch = { isChecked, _ ->
                            lifecycleScope.launch {
                                anilistMutations.updateSettings(airingNotifications = isChecked)
                                Anilist.airingNotifications = isChecked
                                restartApp()
                            }
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.display_adult_content),
                        desc = getString(R.string.display_adult_content_desc),
                        icon = R.drawable.ic_round_nsfw_24,
                        isChecked = displayAdultContent,
                        switch = { isChecked, _ ->
                            lifecycleScope.launch {
                                anilistMutations.updateSettings(displayAdultContent = isChecked)
                                Anilist.adult = isChecked
                                restartApp()
                            }
                        }
                    ),
                )
            )
            binding.settingsRecyclerView1.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        }

        binding.settingsRecyclerView2.adapter = SettingsAdapter(
            arrayListOf(
                Settings(
                    type = 2,
                    name = getString(R.string.restrict_messages),
                    desc = getString(R.string.restrict_messages_desc),
                    icon = R.drawable.ic_round_lock_open_24,
                    isChecked = Anilist.restrictMessagesToFollowing,
                    switch = { isChecked, _ ->
                        lifecycleScope.launch {
                            anilistMutations.updateSettings(restrictMessagesToFollowing = isChecked)
                            Anilist.restrictMessagesToFollowing = isChecked
                            restartApp()
                        }
                    }
                ),
            )
        )
        binding.settingsRecyclerView2.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    }

    private fun addCustomListItem(listName: String, container: LinearLayout, isAnime: Boolean) {
        val customListItemView = layoutInflater.inflate(R.layout.item_custom_list, container, false)
        val textInputLayout = customListItemView.findViewById<TextInputLayout>(R.id.customListItem)
        val editText = textInputLayout.editText as? TextInputEditText
        editText?.setText(listName)
        textInputLayout.setEndIconOnClickListener {
            val name = editText?.text.toString()
            if (name.isNotEmpty()) {
                val listExists = if (isAnime) {
                    Anilist.animeCustomLists?.contains(name) ?: false
                } else {
                    Anilist.mangaCustomLists?.contains(name) ?: false
                }

                if (listExists) {
                    customAlertDialog().apply {
                        setTitle(getString(R.string.delete_custom_list))
                        setMessage(getString(R.string.delete_custom_list_confirm, name))
                        setPosButton(getString(R.string.delete)) {
                            deleteCustomList(name, isAnime)
                            container.removeView(customListItemView)
                        }
                        setNegButton(getString(R.string.cancel))
                    }.show()
                } else {
                    container.removeView(customListItemView)
                }
            } else {
                container.removeView(customListItemView)
            }
        }
        container.addView(customListItemView)
    }

    private fun deleteCustomList(name: String, isAnime: Boolean) {
        lifecycleScope.launch {
            val type = if (isAnime) "ANIME" else "MANGA"
            val success = anilistMutations.deleteCustomList(name, type)
            if (success) {
                if (isAnime) {
                    Anilist.animeCustomLists = Anilist.animeCustomLists?.filter { it != name }
                } else {
                    Anilist.mangaCustomLists = Anilist.mangaCustomLists?.filter { it != name }
                }
                toast("Custom list deleted")
            } else {
                toast("Failed to delete custom list")
            }
        }
    }

    private fun saveCustomLists() {
        val animeCustomLists = binding.animeCustomListsContainer.children
            .mapNotNull { (it.findViewById<TextInputLayout>(R.id.customListItem).editText as? TextInputEditText)?.text?.toString() }
            .filter { it.isNotEmpty() }
            .toList()
        val mangaCustomLists = binding.mangaCustomListsContainer.children
            .mapNotNull { (it.findViewById<TextInputLayout>(R.id.customListItem).editText as? TextInputEditText)?.text?.toString() }
            .filter { it.isNotEmpty() }
            .toList()

        lifecycleScope.launch {
            val success = anilistMutations.updateCustomLists(animeCustomLists, mangaCustomLists)
            if (success) {
                Anilist.animeCustomLists = animeCustomLists
                Anilist.mangaCustomLists = mangaCustomLists
                toast("Custom lists saved")
            } else {
                toast("Failed to save custom lists")
            }
        }
    }
}