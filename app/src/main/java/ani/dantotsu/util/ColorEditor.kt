package ani.dantotsu.util

import android.graphics.Color
import kotlin.math.pow

class ColorEditor {
    companion object {
        fun oppositeColor(color: Int): Int {
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hsv[0] = (hsv[0] + 180) % 360
            return adjustColorForContrast(Color.HSVToColor(hsv), color)
        }

        fun generateColorPalette(
            baseColor: Int,
            size: Int,
            hueDelta: Float = 8f,
            saturationDelta: Float = 2.02f,
            valueDelta: Float = 2.02f
        ): List<Int> {
            val palette = mutableListOf<Int>()
            val hsv = FloatArray(3)
            Color.colorToHSV(baseColor, hsv)

            for (i in 0 until size) {
                val newHue =
                    (hsv[0] + hueDelta * i) % 360 // Ensure hue stays within the 0-360 range
                val newSaturation = (hsv[1] + saturationDelta * i).coerceIn(0f, 1f)
                val newValue = (hsv[2] + valueDelta * i).coerceIn(0f, 1f)

                val newHsv = floatArrayOf(newHue, newSaturation, newValue)
                palette.add(Color.HSVToColor(newHsv))
            }

            return palette
        }

        fun getLuminance(color: Int): Double {
            val r = Color.red(color) / 255.0
            val g = Color.green(color) / 255.0
            val b = Color.blue(color) / 255.0

            val rL = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
            val gL = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
            val bL = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

            return 0.2126 * rL + 0.7152 * gL + 0.0722 * bL
        }

        fun getContrastRatio(color1: Int, color2: Int): Double {
            val l1 = getLuminance(color1)
            val l2 = getLuminance(color2)

            return if (l1 > l2) (l1 + 0.05) / (l2 + 0.05) else (l2 + 0.05) / (l1 + 0.05)
        }

        fun adjustColorForContrast(originalColor: Int, backgroundColor: Int): Int {
            var adjustedColor = originalColor
            var contrastRatio = getContrastRatio(adjustedColor, backgroundColor)
            val isBackgroundDark = getLuminance(backgroundColor) < 0.5

            while (contrastRatio < 4.5) {
                // Adjust brightness by modifying the RGB values
                val r = Color.red(adjustedColor)
                val g = Color.green(adjustedColor)
                val b = Color.blue(adjustedColor)

                // Calculate the amount to adjust
                val adjustment = if (isBackgroundDark) 10 else -10

                // Adjust the color
                val newR = (r + adjustment).coerceIn(0, 255)
                val newG = (g + adjustment).coerceIn(0, 255)
                val newB = (b + adjustment).coerceIn(0, 255)

                adjustedColor = Color.rgb(newR, newG, newB)
                contrastRatio = getContrastRatio(adjustedColor, backgroundColor)

                // Break the loop if the color adjustment does not change (to avoid infinite loop)
                if (newR == r && newG == g && newB == b) {
                    break
                }
            }
            return adjustedColor
        }

        fun Int.toCssColor(): String {
            var base = "rgba("
            base += "${Color.red(this)}, "
            base += "${Color.green(this)}, "
            base += "${Color.blue(this)}, "
            base += "${Color.alpha(this) / 255.0})"
            return base
        }

        fun Int.toHexColor(): String {
            return String.format("#%06X", 0xFFFFFF and this)
        }
    }
}