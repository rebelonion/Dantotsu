package ani.dantotsu.aniyomi.anime.custom


import android.app.Application
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.addons.torrent.TorrentAddonManager
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.media.manga.MangaCache
import ani.dantotsu.parsers.novel.NovelExtensionManager
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.anime.AndroidAnimeSourceManager
import eu.kanade.tachiyomi.source.manga.AndroidMangaSourceManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {
    @kotlin.OptIn(ExperimentalSerializationApi::class)
    @OptIn(UnstableApi::class)
    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        addSingletonFactory { DownloadsManager(app) }

        addSingletonFactory { NetworkHelper(app) }
        addSingletonFactory { NetworkHelper(app).client }

        addSingletonFactory { AnimeExtensionManager(app) }
        addSingletonFactory { MangaExtensionManager(app) }
        addSingletonFactory { NovelExtensionManager(app) }
        addSingletonFactory { TorrentAddonManager(app) }
        addSingletonFactory { DownloadAddonManager(app) }

        addSingletonFactory<AnimeSourceManager> { AndroidAnimeSourceManager(app, get()) }
        addSingletonFactory<MangaSourceManager> { AndroidMangaSourceManager(app, get()) }

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }

        addSingletonFactory { StandaloneDatabaseProvider(app) }

        addSingletonFactory { MangaCache() }

        ContextCompat.getMainExecutor(app).execute {
            get<AnimeSourceManager>()
            get<MangaSourceManager>()
        }
    }
}

class PreferenceModule(val application: Application) : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<PreferenceStore> {
            AndroidPreferenceStore(application)
        }

        addSingletonFactory {
            SourcePreferences(get())
        }

        addSingletonFactory {
            BasePreferences(application, get())
        }
    }
}