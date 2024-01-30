package ani.dantotsu.settings.saving

import android.content.Context
import android.content.SharedPreferences
import ani.dantotsu.settings.saving.internal.Location

object PrefWrapper {

    private var generalPreferences: SharedPreferences? = null
    private var animePreferences: SharedPreferences? = null
    private var mangaPreferences: SharedPreferences? = null
    private var playerPreferences: SharedPreferences? = null
    private var readerPreferences: SharedPreferences? = null
    private var irrelevantPreferences: SharedPreferences? = null
    private var animeDownloadsPreferences: SharedPreferences? = null
    private var protectedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        generalPreferences = context.getSharedPreferences(Location.General.location, Context.MODE_PRIVATE)
        animePreferences = context.getSharedPreferences(Location.Anime.location, Context.MODE_PRIVATE)
        mangaPreferences = context.getSharedPreferences(Location.Manga.location, Context.MODE_PRIVATE)
        playerPreferences = context.getSharedPreferences(Location.Player.location, Context.MODE_PRIVATE)
        readerPreferences = context.getSharedPreferences(Location.Reader.location, Context.MODE_PRIVATE)
        irrelevantPreferences = context.getSharedPreferences(Location.Irrelevant.location, Context.MODE_PRIVATE)
        animeDownloadsPreferences = context.getSharedPreferences(Location.AnimeDownloads.location, Context.MODE_PRIVATE)
        protectedPreferences = context.getSharedPreferences(Location.Protected.location, Context.MODE_PRIVATE)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> setVal(prefName: PrefName, value: T) {
        val pref = when (prefName.data.prefLocation) {
            Location.General -> generalPreferences
            Location.Anime -> animePreferences
            Location.Manga -> mangaPreferences
            Location.Player -> playerPreferences
            Location.Reader -> readerPreferences
            Location.Irrelevant -> irrelevantPreferences
            Location.AnimeDownloads -> animeDownloadsPreferences
            Location.Protected -> protectedPreferences
        }
        with(pref!!.edit()) {
            when (prefName.data.type) {
                Boolean::class -> putBoolean(prefName.name, value as Boolean)
                Int::class -> putInt(prefName.name, value as Int)
                Float::class -> putFloat(prefName.name, value as Float)
                Long::class -> putLong(prefName.name, value as Long)
                String::class -> putString(prefName.name, value as String)
                Set::class -> putStringSet(prefName.name, value as Set<String>)
                else -> throw IllegalArgumentException("Type not supported")
            }
            apply()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getVal(prefName: PrefName, default: T) : T {
        return try {
            val pref = when (prefName.data.prefLocation) {
                Location.General -> generalPreferences
                Location.Anime -> animePreferences
                Location.Manga -> mangaPreferences
                Location.Player -> playerPreferences
                Location.Reader -> readerPreferences
                Location.Irrelevant -> irrelevantPreferences
                Location.AnimeDownloads -> animeDownloadsPreferences
                Location.Protected -> protectedPreferences
            }
            when (prefName.data.type) {
                Boolean::class -> pref!!.getBoolean(prefName.name, default as Boolean) as T
                Int::class -> pref!!.getInt(prefName.name, default as Int) as T
                Float::class -> pref!!.getFloat(prefName.name, default as Float) as T
                Long::class -> pref!!.getLong(prefName.name, default as Long) as T
                String::class -> pref!!.getString(prefName.name, default as String) as T
                Set::class -> pref!!.getStringSet(prefName.name, default as Set<String>) as T
                else -> throw IllegalArgumentException("Type not supported")
            }
        } catch (e: Exception) {
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getLiveVal(prefName: PrefName, default: T) : SharedPreferenceLiveData<T> {
        val pref = when (prefName.data.prefLocation) {
            Location.General -> generalPreferences
            Location.Anime -> animePreferences
            Location.Manga -> mangaPreferences
            Location.Player -> playerPreferences
            Location.Reader -> readerPreferences
            Location.Irrelevant -> irrelevantPreferences
            Location.AnimeDownloads -> animeDownloadsPreferences
            Location.Protected -> protectedPreferences
        }
        return when (prefName.data.type) {
            Boolean::class -> SharedPreferenceBooleanLiveData(
                pref!!,
                prefName.name,
                default as Boolean
            ) as SharedPreferenceLiveData<T>
            Int::class -> SharedPreferenceIntLiveData(
                pref!!,
                prefName.name,
                default as Int
            ) as SharedPreferenceLiveData<T>
            Float::class -> SharedPreferenceFloatLiveData(
                pref!!,
                prefName.name,
                default as Float
            ) as SharedPreferenceLiveData<T>
            Long::class -> SharedPreferenceLongLiveData(
                pref!!,
                prefName.name,
                default as Long
            ) as SharedPreferenceLiveData<T>
            String::class -> SharedPreferenceStringLiveData(
                pref!!,
                prefName.name,
                default as String
            ) as SharedPreferenceLiveData<T>
            Set::class -> SharedPreferenceStringSetLiveData(
                pref!!,
                prefName.name,
                default as Set<String>
            ) as SharedPreferenceLiveData<T>
            else -> throw IllegalArgumentException("Type not supported")
        }
    }

    fun removeVal(prefName: PrefName) {
        val pref = when (prefName.data.prefLocation) {
            Location.General -> generalPreferences
            Location.Anime -> animePreferences
            Location.Manga -> mangaPreferences
            Location.Player -> playerPreferences
            Location.Reader -> readerPreferences
            Location.Irrelevant -> irrelevantPreferences
            Location.AnimeDownloads -> animeDownloadsPreferences
            Location.Protected -> protectedPreferences
        }
        with(pref!!.edit()) {
            remove(prefName.name)
            apply()
        }
    }

    fun SharedPreferenceLiveData<*>.asLiveBool(): SharedPreferenceBooleanLiveData =
        this as? SharedPreferenceBooleanLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Boolean>")

    fun SharedPreferenceLiveData<*>.asLiveInt(): SharedPreferenceIntLiveData =
        this as? SharedPreferenceIntLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Int>")

    fun SharedPreferenceLiveData<*>.asLiveFloat(): SharedPreferenceFloatLiveData =
        this as? SharedPreferenceFloatLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Float>")

    fun SharedPreferenceLiveData<*>.asLiveLong(): SharedPreferenceLongLiveData =
        this as? SharedPreferenceLongLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Long>")

    fun SharedPreferenceLiveData<*>.asLiveString(): SharedPreferenceStringLiveData =
        this as? SharedPreferenceStringLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<String>")

    fun SharedPreferenceLiveData<*>.asLiveStringSet(): SharedPreferenceStringSetLiveData =
        this as? SharedPreferenceStringSetLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Set<String>>")

    fun getAnimeDownloadPreferences(): SharedPreferences = animeDownloadsPreferences!!  //needs to be used externally
}