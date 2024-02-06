package ani.dantotsu.connections.crashlytics

interface CrashlyticsInterface {
    fun logException(e: Throwable)
    fun log(message: String)
    fun setUserId(id: String)
    fun setCustomKey(key: String, value: String)
    fun setCrashlyticsCollectionEnabled(enabled: Boolean)
}