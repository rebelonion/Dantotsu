package ani.dantotsu.settings

import java.io.Serializable

data class ReaderSettings(
    var showSource: Boolean = true,
    var showSystemBars: Boolean = false,

    var autoDetectWebtoon: Boolean = true,
    var default: CurrentReaderSettings = CurrentReaderSettings(),

    var askIndividual: Boolean = true,
    var updateForH: Boolean = false
) : Serializable