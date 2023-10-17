package ani.dantotsu.settings

import java.io.Serializable

data class NovelReaderSettings(
    var showSource: Boolean = true,
    var showSystemBars: Boolean = false,
    var default: CurrentNovelReaderSettings = CurrentNovelReaderSettings(),
    var askIndividual: Boolean = true,
) : Serializable