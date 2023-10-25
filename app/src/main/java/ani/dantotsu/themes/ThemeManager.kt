package ani.dantotsu.themes

import android.content.Context
import ani.dantotsu.R

class ThemeManager(private val context: Context) {
    fun applyTheme() {
        if(context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getBoolean("use_material_you", false)){
            return
        }
        when (context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getString("theme", "PURPLE")!!) {
            "PURPLE" -> {
                context.setTheme(R.style.Theme_Dantotsu_Purple)
            }
            //"MONOCHROME" -> {
            //    context.setTheme(R.style.Theme_Dantotsu_Monochrome)
            //}
            "BLUE" -> {
                context.setTheme(R.style.Theme_Dantotsu_Blue)
            }
            "GREEN" -> {
                context.setTheme(R.style.Theme_Dantotsu_Green)
            }
            "PINK" -> {
                context.setTheme(R.style.Theme_Dantotsu_Pink)
            }
            else -> {
                context.setTheme(R.style.Theme_Dantotsu_Purple)
            }
        }
    }

    companion object{
        enum class Theme(val theme: String) {
            PURPLE("PURPLE"),
            BLUE("BLUE"),
            GREEN("GREEN"),
            PINK("PINK");
            //MONOCHROME("MONOCHROME");

            companion object {
                fun fromString(value: String): Theme {
                    return values().find { it.theme == value } ?: PURPLE
                }
            }
        }
    }
}