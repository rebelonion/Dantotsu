package ani.dantotsu.settings

import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import java.io.Serializable

data class CurrentReaderSettings(
    var direction: Directions = Directions[PrefManager.getVal(PrefName.Direction)]
        ?: Directions.TOP_TO_BOTTOM,
    var layout: Layouts = Layouts[PrefManager.getVal(PrefName.LayoutReader)]
        ?: Layouts.CONTINUOUS,
    var dualPageMode: DualPageModes = DualPageModes[PrefManager.getVal(PrefName.DualPageModeReader)]
        ?: DualPageModes.Automatic,
    var overScrollMode: Boolean = PrefManager.getVal(PrefName.OverScrollMode),
    var trueColors: Boolean = PrefManager.getVal(PrefName.TrueColors),
    var rotation: Boolean = PrefManager.getVal(PrefName.Rotation),
    var padding: Boolean = PrefManager.getVal(PrefName.Padding),
    var hideScrollBar: Boolean = PrefManager.getVal(PrefName.HideScrollBar),
    var hidePageNumbers: Boolean = PrefManager.getVal(PrefName.HidePageNumbers),
    var horizontalScrollBar: Boolean = PrefManager.getVal(PrefName.HorizontalScrollBar),
    var keepScreenOn: Boolean = PrefManager.getVal(PrefName.KeepScreenOn),
    var volumeButtons: Boolean = PrefManager.getVal(PrefName.VolumeButtonsReader),
    var wrapImages: Boolean = PrefManager.getVal(PrefName.WrapImages),
    var longClickImage: Boolean = PrefManager.getVal(PrefName.LongClickImage),
    var cropBorders: Boolean = PrefManager.getVal(PrefName.CropBorders),
    var cropBorderThreshold: Int = PrefManager.getVal(PrefName.CropBorderThreshold)
) : Serializable {

    enum class Directions {
        TOP_TO_BOTTOM,
        RIGHT_TO_LEFT,
        BOTTOM_TO_TOP,
        LEFT_TO_RIGHT;

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }

    enum class Layouts {
        PAGED,
        CONTINUOUS_PAGED,
        CONTINUOUS;

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }

    enum class DualPageModes {
        No, Automatic, Force;

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }

    companion object {
        fun applyWebtoon(settings: CurrentReaderSettings) {
            settings.apply {
                layout = Layouts.CONTINUOUS
                direction = Directions.TOP_TO_BOTTOM
                dualPageMode = DualPageModes.No
                padding = false
            }
        }
    }
}

