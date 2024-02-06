package ani.dantotsu.settings.saving.internal

import kotlin.reflect.KClass


data class Pref(
    val prefLocation: Location,
    val type: KClass<*>,
    val default: Any
)

enum class Location(val location: String, val exportable: Boolean) {
    General("ani.dantotsu.general", true),
    UI("ani.dantotsu.ui", true),
    Player("ani.dantotsu.player", true),
    Reader("ani.dantotsu.reader", true),
    NovelReader("ani.dantotsu.novelReader", true),
    Irrelevant("ani.dantotsu.irrelevant", false),
    AnimeDownloads("animeDownloads", false),  //different for legacy reasons
    Protected("ani.dantotsu.protected", true),
}
