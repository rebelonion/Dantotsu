package ani.dantotsu.connections.crashlytics

class CrashlyticsStub : CrashlyticsInterface {
    override fun logException(e: Throwable) {
        //no-op
    }

    override fun log(message: String) {
        //no-op
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