package eu.kanade.tachiyomi.animesource

import ani.dantotsu.aniyomi.animesource.AnimeSource
import ani.dantotsu.aniyomi.PreferenceScreen

interface ConfigurableAnimeSource : AnimeSource {

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
