package ani.dantotsu.widgets.upcoming

import android.content.Intent
import android.widget.RemoteViewsService
import ani.dantotsu.util.Logger

class UpcomingRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Logger.log("UpcomingRemoteViewsFactory onGetViewFactory")
        return UpcomingRemoteViewsFactory(applicationContext)
    }
}
