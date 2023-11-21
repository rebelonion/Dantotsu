package ani.dantotsu.others

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale




class LangSet {
    companion object{
        fun setLocale(activity: Activity) {
            val useCursedLang = activity.getSharedPreferences("Dantotsu", Activity.MODE_PRIVATE).getBoolean("use_cursed_lang", false)
            if(!useCursedLang) return
            val locale = Locale("en", "rDW")
            Locale.setDefault(locale)
            val resources: Resources = activity.resources
            val config: Configuration = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}