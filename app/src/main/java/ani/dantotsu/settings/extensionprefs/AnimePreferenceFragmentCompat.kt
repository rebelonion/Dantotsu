package ani.dantotsu.settings.extensionprefs

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.preference.DialogPreference
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.forEach
import androidx.preference.getOnBindEditTextListener
import ani.dantotsu.snackString
import eu.kanade.tachiyomi.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.data.preference.SharedPreferencesDataStore
import eu.kanade.tachiyomi.source.anime.getPreferenceKey
import eu.kanade.tachiyomi.widget.TachiyomiTextInputEditText.Companion.setIncognito
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeSourcePreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = try {
            populateAnimePreferenceScreen()
        } catch (e: Exception) {
            snackString(e.message ?: "Unknown error")
            preferenceManager.createPreferenceScreen(requireContext())
        }
        //set background color
        val color = TypedValue()
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.backgroundColor,
            color,
            true
        )
        view?.setBackgroundColor(color.data)
    }

    private var onCloseAction: (() -> Unit)? = null


    override fun onDestroyView() {
        super.onDestroyView()
        onCloseAction?.invoke()
    }

    fun populateAnimePreferenceScreen(): PreferenceScreen {
        val sourceId = requireArguments().getLong(SOURCE_ID)
        val source = Injekt.get<AnimeSourceManager>().get(sourceId) as? ConfigurableAnimeSource
            ?: error("Source with id: $sourceId not found!")
        val sharedPreferences =
            requireContext().getSharedPreferences(source.getPreferenceKey(), Context.MODE_PRIVATE)
        val dataStore = SharedPreferencesDataStore(sharedPreferences)
        preferenceManager.preferenceDataStore = dataStore
        val sourceScreen = preferenceManager.createPreferenceScreen(requireContext())
        source.setupPreferenceScreen(sourceScreen)
        sourceScreen.forEach { pref ->
            pref.isIconSpaceReserved = false
            if (pref is DialogPreference) {
                pref.dialogTitle = pref.title
            }

            // Apply incognito IME for EditTextPreference
            if (pref is EditTextPreference) {
                val setListener = pref.getOnBindEditTextListener()
                pref.setOnBindEditTextListener {
                    setListener?.onBindEditText(it)
                    it.setIncognito(lifecycleScope)
                }
            }
        }

        return sourceScreen
    }

    fun getInstance(
        sourceId: Long,
        onCloseAction: (() -> Unit)? = null
    ): AnimeSourcePreferencesFragment {
        val fragment = AnimeSourcePreferencesFragment()
        fragment.arguments = bundleOf(SOURCE_ID to sourceId)
        fragment.onCloseAction = onCloseAction
        return fragment
    }

    companion object { //idk why it needs both
        private const val SOURCE_ID = "source_id"
    }
}

class InitialAnimeSourcePreferencesFragment(
    val sharedPreferences: SharedPreferences,
    val source: ConfigurableAnimeSource,
    val currContext: Context
) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = try {
            populateAnimePreferenceScreen()
        } catch (e: Exception) {
            snackString(e.message ?: "Unknown error")
            preferenceManager.createPreferenceScreen(requireContext())
        }
        //set background color
        val color = TypedValue()
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.backgroundColor,
            color,
            true
        )
        view?.setBackgroundColor(color.data)
    }


    fun populateAnimePreferenceScreen(): PreferenceScreen {
        val dataStore = SharedPreferencesDataStore(sharedPreferences)
        preferenceManager.preferenceDataStore = dataStore
        val sourceScreen = preferenceManager.createPreferenceScreen(requireContext())
        source.setupPreferenceScreen(sourceScreen)
        return sourceScreen
    }
}