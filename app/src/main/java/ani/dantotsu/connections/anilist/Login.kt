package ani.dantotsu.connections.anilist

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.logError
import ani.dantotsu.logger
import ani.dantotsu.startMainActivity
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.others.LangSet

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LangSet.setLocale(this)
ThemeManager(this).applyTheme()
        val data: Uri? = intent?.data
        logger(data.toString())
        try {
            Anilist.token = Regex("""(?<=access_token=).+(?=&token_type)""").find(data.toString())!!.value
            val filename = "anilistToken"
            this.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(Anilist.token!!.toByteArray())
            }
        } catch (e: Exception) {
            logError(e)
        }
        startMainActivity(this)
    }
}
