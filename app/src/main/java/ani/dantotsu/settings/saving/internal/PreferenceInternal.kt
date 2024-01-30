package ani.dantotsu.settings.saving.internal

import kotlin.reflect.KClass


data class Pref(
    val prefLocation: Location,
    val type: KClass<*>
)
enum class Location(val location: String) {
    General("ani.dantotsu.general"),
    Anime("ani.dantotsu.anime"),
    Manga("ani.dantotsu.manga"),
    Player("ani.dantotsu.player"),
    Reader("ani.dantotsu.reader"),
    Irrelevant("ani.dantotsu.irrelevant"),
    AnimeDownloads("animeDownloads"),  //different for legacy reasons
    Protected("ani.dantotsu.protected")
}
