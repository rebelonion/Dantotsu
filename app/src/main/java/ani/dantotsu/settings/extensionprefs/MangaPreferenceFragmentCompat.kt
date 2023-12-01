package ani.dantotsu.settings.extensionprefs

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.preference.DialogPreference
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.forEach
import androidx.preference.getOnBindEditTextListener
import eu.kanade.tachiyomi.PreferenceScreen
import eu.kanade.tachiyomi.data.preference.SharedPreferencesDataStore
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.manga.getPreferenceKey
import eu.kanade.tachiyomi.widget.TachiyomiTextInputEditText.Companion.setIncognito
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaSourcePreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = populateMangaPreferenceScreen()
    }

    private var onCloseAction: (() -> Unit)? = null

    override fun onDestroyView() {
        super.onDestroyView()
        onCloseAction?.invoke()

    }

    fun populateMangaPreferenceScreen(): PreferenceScreen {
        val sourceId = requireArguments().getLong(SOURCE_ID)
        val source = Injekt.get<MangaSourceManager>().get(sourceId)!!
        check(source is ConfigurableSource)
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
                println("pref.dialogTitle: ${pref.dialogTitle}")
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
    ): MangaSourcePreferencesFragment {
        val fragment = MangaSourcePreferencesFragment()
        fragment.arguments = bundleOf(SOURCE_ID to sourceId)
        fragment.onCloseAction = onCloseAction
        return fragment
    }

    companion object {
        private const val SOURCE_ID = "source_id"
    }
}