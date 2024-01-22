package ani.dantotsu.widgets

import android.content.Intent
import android.widget.RemoteViewsService
import ani.dantotsu.logger

class CurrentlyAiringRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        logger("CurrentlyAiringRemoteViewsFactory onGetViewFactory")
        return CurrentlyAiringRemoteViewsFactory(applicationContext, intent)
    }
}
