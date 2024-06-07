package ani.dantotsu.connections.crashlytics

import android.content.Context
import ani.dantotsu.util.Logger

class CrashlyticsStub : CrashlyticsInterface {
    override fun initialize(context: Context) {
        //no-op
    }

    override fun logException(e: Throwable) {
        Logger.log(e)
    }

    override fun log(message: String) {
        Logger.log(message)
    }

    override fun setUserId(id: String) {
        //no-op
    }

    override fun setCustomKey(key: String, value: String) {
        //no-op
    }

    override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        //no-op
    }

}