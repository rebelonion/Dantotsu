package ani.dantotsu.settings

import java.io.Serializable

data class CurrentReaderSettings(
    var direction: Directions = Directions.TOP_TO_BOTTOM,
    var layout: Layouts = Layouts.CONTINUOUS,
    var dualPageMode: DualPageModes = DualPageModes.Automatic,
    var overScrollMode: Boolean = true,
    var trueColors: Boolean = false,
    var rotation: Boolean = true,
    var padding: Boolean = true,
    var hidePageNumbers: Boolean = false,
    var horizontalScrollBar: Boolean = true,
    var keepScreenOn: Boolean = false,
    var volumeButtons: Boolean = false,
    var wrapImages: Boolean = false,
    var longClickImage: Boolean = true,
    var cropBorders: Boolean = false,
    var cropBorderThreshold: Int = 10,
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

