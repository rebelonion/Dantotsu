package ani.dantotsu.settings.saving.internal

import android.content.Context
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.PrefWrapper

class Compat {
    companion object {
        fun importOldPrefs(context: Context) {
            if (PrefWrapper.getVal(PrefName.HasUpdatedPrefs, false)) return
            val oldPrefs = context.getSharedPreferences("downloads_pref", Context.MODE_PRIVATE)
            val jsonString = oldPrefs.getString("downloads_key", null)
            PrefWrapper.setVal(PrefName.DownloadsKeys, jsonString)
            oldPrefs.edit().clear().apply()
            PrefWrapper.setVal(PrefName.HasUpdatedPrefs, true)
        }
    }
}