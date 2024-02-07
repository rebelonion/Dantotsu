package ani.dantotsu.connections.crashlytics

import android.content.Context

interface CrashlyticsInterface {
    fun initialize(context: Context)
    fun logException(e: Throwable)
    fun log(message: String)
    fun setUserId(id: String)
    fun setCustomKey(key: String, value: String)
    fun setCrashlyticsCollectionEnabled(enabled: Boolean)
}