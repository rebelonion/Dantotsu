package ani.dantotsu.settings.saving.internal

import kotlin.reflect.KClass


data class Pref(
    val prefLocation: Location,
    val type: KClass<*>,
    val default: Any
)
enum class Location(val location: String) {
    General("ani.dantotsu.general"),
    UI("ani.dantotsu.ui"),
    Anime("ani.dantotsu.anime"),
    Manga("ani.dantotsu.manga"),
    Player("ani.dantotsu.player"),
    Reader("ani.dantotsu.reader"),
    NovelReader("ani.dantotsu.novelReader"),
    Irrelevant("ani.dantotsu.irrelevant"),
    AnimeDownloads("animeDownloads"),  //different for legacy reasons
    Protected("ani.dantotsu.protected")
}
