package ani.dantotsu.themes

import android.content.Context
import android.content.res.Configuration
import ani.dantotsu.R

class ThemeManager(private val context: Context) {
    fun applyTheme() {
        val useOLED = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getBoolean("use_oled", false) && isDarkThemeActive(context)
        if(context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getBoolean("use_material_you", false)){
            return
        }
        val theme = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getString("theme", "PURPLE")!!

        val themeToApply = when (theme) {
            "PURPLE" -> if (useOLED) R.style.Theme_Dantotsu_PurpleOLED else R.style.Theme_Dantotsu_Purple
            "BLUE" -> if (useOLED) R.style.Theme_Dantotsu_BlueOLED else R.style.Theme_Dantotsu_Blue
            "GREEN" -> if (useOLED) R.style.Theme_Dantotsu_GreenOLED else R.style.Theme_Dantotsu_Green
            "PINK" -> if (useOLED) R.style.Theme_Dantotsu_PinkOLED else R.style.Theme_Dantotsu_Pink
            "RED" -> if (useOLED) R.style.Theme_Dantotsu_RedOLED else R.style.Theme_Dantotsu_Red
            "LAVENDER" -> if (useOLED) R.style.Theme_Dantotsu_LavenderOLED else R.style.Theme_Dantotsu_Lavender
            "MONOCHROME (BETA)" -> if (useOLED) R.style.Theme_Dantotsu_MonochromeOLED else R.style.Theme_Dantotsu_Monochrome
            else -> if (useOLED) R.style.Theme_Dantotsu_PurpleOLED else R.style.Theme_Dantotsu_Purple
        }

        context.setTheme(themeToApply)
    }

    private fun isDarkThemeActive(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }


    companion object{
        enum class Theme(val theme: String) {
            PURPLE("PURPLE"),
            BLUE("BLUE"),
            GREEN("GREEN"),
            PINK("PINK"),
            RED("RED"),
            LAVENDER("LAVENDER"),
            MONOCHROME("MONOCHROME (BETA)");

            companion object {
                fun fromString(value: String): Theme {
                    return values().find { it.theme == value } ?: PURPLE
                }
            }
        }
    }
}