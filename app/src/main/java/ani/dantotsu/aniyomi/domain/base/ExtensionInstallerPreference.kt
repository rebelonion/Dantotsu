package ani.dantotsu.aniyomi.domain.base

import android.content.Context
import ani.dantotsu.aniyomi.util.system.hasMiuiPackageInstaller
import ani.dantotsu.aniyomi.domain.base.BasePreferences.ExtensionInstaller
import ani.dantotsu.aniyomi.util.system.isShizukuInstalled
import kotlinx.coroutines.CoroutineScope
import ani.dantotsu.aniyomi.core.preference.Preference
import ani.dantotsu.aniyomi.core.preference.PreferenceStore
import ani.dantotsu.aniyomi.core.preference.getEnum

class ExtensionInstallerPreference(
    private val context: Context,
    preferenceStore: PreferenceStore
) : Preference<ExtensionInstaller> {

    private val basePref = preferenceStore.getEnum(key(), defaultValue())

    override fun key() = "extension_installer"



    val entries get() = BasePreferences.ExtensionInstaller.values().run {
        if (context.hasMiuiPackageInstaller) {
            filter { it != BasePreferences.ExtensionInstaller.PACKAGEINSTALLER }
        } else {
            toList()
        }
    }

    override fun defaultValue() = if (context.hasMiuiPackageInstaller) {
        ExtensionInstaller.LEGACY
    } else {
        ExtensionInstaller.PACKAGEINSTALLER
    }

    private fun check(value: ExtensionInstaller): ExtensionInstaller {
        when (value) {
            ExtensionInstaller.PACKAGEINSTALLER -> {
                if (context.hasMiuiPackageInstaller) return ExtensionInstaller.LEGACY
            }
            ExtensionInstaller.SHIZUKU -> {
                if (!context.isShizukuInstalled) return defaultValue()
            }
            else -> {}
        }
        return value
    }

    override fun get(): ExtensionInstaller {
        val value = basePref.get()
        val checkedValue = check(value)
        if (value != checkedValue) {
            basePref.set(checkedValue)
        }
        return checkedValue
    }

    override fun set(value: ExtensionInstaller) {
        basePref.set(check(value))
    }

    override fun isSet() = basePref.isSet()

    override fun delete() = basePref.delete()

    override fun changes() = basePref.changes()

    override fun stateIn(scope: CoroutineScope) = basePref.stateIn(scope)
}
