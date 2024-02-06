package ani.dantotsu.connections.anilist

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import ani.dantotsu.loadMedia
import ani.dantotsu.startMainActivity
import ani.dantotsu.themes.ThemeManager

class UrlMedia : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        var id: Int? = intent?.extras?.getInt("media", 0) ?: 0
        var isMAL = false
        var continueMedia = true
        if (id == 0) {
            continueMedia = false
            val data: Uri? = intent?.data
            isMAL = data?.host != "anilist.co"
            id = data?.pathSegments?.getOrNull(1)?.toIntOrNull()
        } else loadMedia = id
        startMainActivity(
            this,
            bundleOf("mediaId" to id, "mal" to isMAL, "continue" to continueMedia)
        )
    }
}