package eu.kanade.tachiyomi.extension.anime.model

sealed class AnimeLoadResult {
    class Success(val extension: AnimeExtension.Installed) : AnimeLoadResult()
    class Untrusted(val extension: AnimeExtension.Untrusted) : AnimeLoadResult()
    data object Error : AnimeLoadResult()
}
