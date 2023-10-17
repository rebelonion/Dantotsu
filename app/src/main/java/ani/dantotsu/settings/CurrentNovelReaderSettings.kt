package ani.dantotsu.settings

import java.io.Serializable

data class CurrentNovelReaderSettings(
    var currentThemeName: String = "Default",
    var layout: Layouts = Layouts.PAGED,
    var dualPageMode: CurrentReaderSettings.DualPageModes = CurrentReaderSettings.DualPageModes.Automatic,
    var lineHeight: Float = 1.4f,
    var margin: Float = 0.06f,
    var justify: Boolean = true,
    var hyphenation: Boolean = true,
    var useDarkTheme: Boolean = false,
    var invert: Boolean = false,
    var maxInlineSize: Int = 720,
    var maxBlockSize: Int = 1440,
    var horizontalScrollBar: Boolean = true,
    var keepScreenOn: Boolean = false,
    var volumeButtons: Boolean = false,
) : Serializable {

    enum class Layouts(val string: String) {
        PAGED("Paged"),
        SCROLLED("Scrolled");

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }
}
