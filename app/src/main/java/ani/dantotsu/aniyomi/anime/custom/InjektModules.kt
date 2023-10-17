package ani.dantotsu.aniyomi.anime.custom

import android.app.Application
import ani.dantotsu.aniyomi.anime.AnimeExtensionManager
import ani.dantotsu.aniyomi.core.preference.PreferenceStore
import ani.dantotsu.aniyomi.domain.base.BasePreferences
import ani.dantotsu.aniyomi.domain.source.service.SourcePreferences
import ani.dantotsu.aniyomi.core.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        addSingletonFactory { NetworkHelper(app) }

        addSingletonFactory { AnimeExtensionManager(app) }

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
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