package ani.dantotsu.settings

import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import java.io.Serializable

data class CurrentNovelReaderSettings(
    var currentThemeName: String = PrefManager.getVal(PrefName.CurrentThemeName),
    var layout: Layouts = Layouts[PrefManager.getVal(PrefName.LayoutNovel)]
        ?: Layouts.PAGED,
    var dualPageMode: CurrentReaderSettings.DualPageModes = CurrentReaderSettings.DualPageModes[PrefManager.getVal(
        PrefName.DualPageModeNovel
    )]
        ?: CurrentReaderSettings.DualPageModes.Automatic,
    var lineHeight: Float = PrefManager.getVal(PrefName.LineHeight),
    var margin: Float = PrefManager.getVal(PrefName.Margin),
    var justify: Boolean = PrefManager.getVal(PrefName.Justify),
    var hyphenation: Boolean = PrefManager.getVal(PrefName.Hyphenation),
    var useDarkTheme: Boolean = PrefManager.getVal(PrefName.UseDarkThemeNovel),
    var useOledTheme: Boolean = PrefManager.getVal(PrefName.UseOledThemeNovel),
    var invert: Boolean = PrefManager.getVal(PrefName.Invert),
    var maxInlineSize: Int = PrefManager.getVal(PrefName.MaxInlineSize),
    var maxBlockSize: Int = PrefManager.getVal(PrefName.MaxBlockSize),
    var horizontalScrollBar: Boolean = PrefManager.getVal(PrefName.HorizontalScrollBarNovel),
    var keepScreenOn: Boolean = PrefManager.getVal(PrefName.KeepScreenOnNovel),
    var volumeButtons: Boolean = PrefManager.getVal(PrefName.VolumeButtonsNovel)
) : Serializable {

    enum class Layouts(val string: String) {
        PAGED("Paged"),
        SCROLLED("Scrolled");

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }
}
