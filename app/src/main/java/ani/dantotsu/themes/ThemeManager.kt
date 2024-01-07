package ani.dantotsu.themes

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import ani.dantotsu.R
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions


class ThemeManager(private val context: Context) {
    fun applyTheme(fromImage: Bitmap? = null) {
        val useOLED = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            .getBoolean("use_oled", false) && isDarkThemeActive(context)
        val useCustomTheme = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            .getBoolean("use_custom_theme", false)
        val customTheme = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            .getInt("custom_theme_int", 16712221)
        val useSource = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            .getBoolean("use_source_theme", false)
        val useMaterial = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            .getBoolean("use_material_you", false)
        if (useSource) {
            val returnedEarly = applyDynamicColors(
                useMaterial,
                context,
                useOLED,
                fromImage,
                useCustom = if (useCustomTheme) customTheme else null
            )
            if (!returnedEarly) return
        } else if (useCustomTheme) {
            val returnedEarly =
                applyDynamicColors(useMaterial, context, useOLED, useCustom = customTheme)
            if (!returnedEarly) return
        } else {
            val returnedEarly = applyDynamicColors(useMaterial, context, useOLED, useCustom = null)
            if (!returnedEarly) return
        }
        val theme = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            .getString("theme", "PURPLE")!!

        val themeToApply = when (theme) {
            "BLUE" -> if (useOLED) R.style.Theme_Dantotsu_BlueOLED else R.style.Theme_Dantotsu_Blue
            "GREEN" -> if (useOLED) R.style.Theme_Dantotsu_GreenOLED else R.style.Theme_Dantotsu_Green
            "PURPLE" -> if (useOLED) R.style.Theme_Dantotsu_PurpleOLED else R.style.Theme_Dantotsu_Purple
            "PINK" -> if (useOLED) R.style.Theme_Dantotsu_PinkOLED else R.style.Theme_Dantotsu_Pink
            "SAIKOU" -> if (useOLED) R.style.Theme_Dantotsu_SaikouOLED else R.style.Theme_Dantotsu_Saikou
            "RED" -> if (useOLED) R.style.Theme_Dantotsu_RedOLED else R.style.Theme_Dantotsu_Red
            "LAVENDER" -> if (useOLED) R.style.Theme_Dantotsu_LavenderOLED else R.style.Theme_Dantotsu_Lavender
            "OCEAN" -> if (useOLED) R.style.Theme_Dantotsu_OceanOLED else R.style.Theme_Dantotsu_Ocean
            "MONOCHROME (BETA)" -> if (useOLED) R.style.Theme_Dantotsu_MonochromeOLED else R.style.Theme_Dantotsu_Monochrome
            else -> if (useOLED) R.style.Theme_Dantotsu_PurpleOLED else R.style.Theme_Dantotsu_Purple
        }

        context.setTheme(themeToApply)
    }

    private fun applyDynamicColors(
        useMaterialYou: Boolean,
        context: Context,
        useOLED: Boolean,
        bitmap: Bitmap? = null,
        useCustom: Int? = null
    ): Boolean {
        val builder = DynamicColorsOptions.Builder()
        var needMaterial = true

        // Set content-based source if a bitmap is provided
        if (bitmap != null) {
            builder.setContentBasedSource(bitmap)
            needMaterial = false
        } else if (useCustom != null) {
            builder.setContentBasedSource(useCustom)
            needMaterial = false
        }

        if (useOLED) {
            builder.setThemeOverlay(R.style.AppTheme_Amoled)
        }
        if (needMaterial && !useMaterialYou) return true

        // Build the options
        val options = builder.build()

        // Apply the dynamic colors to the activity
        val activity = context as Activity
        DynamicColors.applyToActivityIfAvailable(activity, options)

        if (useOLED) {
            val options2 = DynamicColorsOptions.Builder()
                .setThemeOverlay(R.style.AppTheme_Amoled)
                .build()
            DynamicColors.applyToActivityIfAvailable(activity, options2)
        }

        return false
    }

    private fun isDarkThemeActive(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }


    companion object {
        enum class Theme(val theme: String) {
            BLUE("BLUE"),
            GREEN("GREEN"),
            PURPLE("PURPLE"),
            PINK("PINK"),
            SAIKOU("SAIKOU"),
            RED("RED"),
            LAVENDER("LAVENDER"),
            OCEAN("OCEAN"),
            MONOCHROME("MONOCHROME (BETA)");

            companion object {
                fun fromString(value: String): Theme {
                    return values().find { it.theme == value } ?: PURPLE
                }
            }
        }
    }
}
