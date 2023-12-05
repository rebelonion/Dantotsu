package ani.dantotsu.connections

import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.currContext
import ani.dantotsu.media.Media
import ani.dantotsu.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun updateProgress(media: Media, number: String) {
    val incognito = currContext()?.getSharedPreferences("Dantotsu", 0)
        ?.getBoolean("incognito", false) ?: false
    if (!incognito) {
        if (Anilist.userid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val a = number.toFloatOrNull()?.toInt()
                if ((a ?: 0) > (media.userProgress ?: 0)) {
                    Anilist.mutation.editList(
                        media.id,
                        a,
                        status = if (media.userStatus == "REPEATING") media.userStatus else "CURRENT"
                    )
                    MAL.query.editList(
                        media.idMAL,
                        media.anime != null,
                        a, null,
                        if (media.userStatus == "REPEATING") media.userStatus!! else "CURRENT"
                    )
                    toast(currContext()?.getString(R.string.setting_progress, a))
                }
                media.userProgress = a
                Refresh.all()
            }
        } else {
            toast(currContext()?.getString(R.string.login_anilist_account))
        }
    } else {
        toast("Sneaky sneaky :3")
    }
}