package ani.dantotsu.connections.crashlytics

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

class FirebaseCrashlytics : CrashlyticsInterface {
    override fun initialize(context: Context) {
        FirebaseApp.initializeApp(context)
    }

    override fun logException(e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }

    override fun log(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }

    override fun setUserId(id: String) {
        Firebase.crashlytics.setUserId(id)
    }

    override fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }

}