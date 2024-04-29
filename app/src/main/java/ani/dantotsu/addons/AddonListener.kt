package ani.dantotsu.addons

interface AddonListener {
    fun onAddonInstalled(result: LoadResult?)
    fun onAddonUpdated(result: LoadResult?)
    fun onAddonUninstalled(pkgName: String)

    enum class ListenerAction {
        INSTALL, UPDATE, UNINSTALL
    }
}