package ani.dantotsu.settings.saving

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import ani.dantotsu.settings.saving.internal.Compat
import ani.dantotsu.settings.saving.internal.Location
import ani.dantotsu.settings.saving.internal.PreferencePackager
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object PrefManager {

    private var generalPreferences: SharedPreferences? = null
    private var uiPreferences: SharedPreferences? = null
    private var playerPreferences: SharedPreferences? = null
    private var readerPreferences: SharedPreferences? = null
    private var irrelevantPreferences: SharedPreferences? = null
    private var animeDownloadsPreferences: SharedPreferences? = null
    private var protectedPreferences: SharedPreferences? = null

    fun init(context: Context) {  //must be called in Application class or will crash
        if (generalPreferences != null) return
        generalPreferences =
            context.getSharedPreferences(Location.General.location, Context.MODE_PRIVATE)
        uiPreferences =
            context.getSharedPreferences(Location.UI.location, Context.MODE_PRIVATE)
        playerPreferences =
            context.getSharedPreferences(Location.Player.location, Context.MODE_PRIVATE)
        readerPreferences =
            context.getSharedPreferences(Location.Reader.location, Context.MODE_PRIVATE)
        irrelevantPreferences =
            context.getSharedPreferences(Location.Irrelevant.location, Context.MODE_PRIVATE)
        animeDownloadsPreferences =
            context.getSharedPreferences(Location.AnimeDownloads.location, Context.MODE_PRIVATE)
        protectedPreferences =
            context.getSharedPreferences(Location.Protected.location, Context.MODE_PRIVATE)
        Compat.importOldPrefs(context)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> setVal(prefName: PrefName, value: T?) {
        val pref = getPrefLocation(prefName.data.prefLocation)
        with(pref.edit()) {
            when (value) {
                is Boolean -> putBoolean(prefName.name, value)
                is Int -> putInt(prefName.name, value)
                is Float -> putFloat(prefName.name, value)
                is Long -> putLong(prefName.name, value)
                is String -> putString(prefName.name, value)
                is Set<*> -> putStringSet(prefName.name, value as Set<String>)
                null -> remove(prefName.name)
                else -> serializeClass(prefName.name, value, prefName.data.prefLocation)
            }
            apply()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getVal(prefName: PrefName, default: T): T {
        return try {
            val pref = getPrefLocation(prefName.data.prefLocation)
            when (prefName.data.type) {
                Boolean::class -> pref.getBoolean(prefName.name, default as Boolean) as T
                Int::class -> pref.getInt(prefName.name, default as Int) as T
                Float::class -> pref.getFloat(prefName.name, default as Float) as T
                Long::class -> pref.getLong(prefName.name, default as Long) as T
                String::class -> pref.getString(prefName.name, default as String?) as T
                Set::class -> pref.getStringSet(prefName.name, default as Set<String>) as T

                List::class -> deserializeClass(
                    prefName.name,
                    default,
                    prefName.data.prefLocation
                ) as T

                else -> throw IllegalArgumentException("Type not supported")
            }
        } catch (e: Exception) {
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getVal(prefName: PrefName): T {
        return try {
            val pref = getPrefLocation(prefName.data.prefLocation)
            when (prefName.data.type) {
                Boolean::class -> pref.getBoolean(
                    prefName.name,
                    prefName.data.default as Boolean
                ) as T

                Int::class -> pref.getInt(prefName.name, prefName.data.default as Int) as T
                Float::class -> pref.getFloat(prefName.name, prefName.data.default as Float) as T
                Long::class -> pref.getLong(prefName.name, prefName.data.default as Long) as T
                String::class -> pref.getString(
                    prefName.name,
                    prefName.data.default as String?
                ) as T

                Set::class -> pref.getStringSet(
                    prefName.name,
                    prefName.data.default as Set<String>
                ) as T

                List::class -> deserializeClass(
                    prefName.name,
                    prefName.data.default,
                    prefName.data.prefLocation
                ) as T

                else -> throw IllegalArgumentException("Type not supported")
            }
        } catch (e: Exception) {
            prefName.data.default as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getNullableVal(
        prefName: PrefName,
        default: T?
    ): T? {  //Strings don't necessarily need to use this one
        return try {
            val pref = getPrefLocation(prefName.data.prefLocation)
            when (prefName.data.type) {
                Boolean::class -> pref.getBoolean(prefName.name, default as Boolean) as T?
                Int::class -> pref.getInt(prefName.name, default as Int) as T?
                Float::class -> pref.getFloat(prefName.name, default as Float) as T?
                Long::class -> pref.getLong(prefName.name, default as Long) as T?
                String::class -> pref.getString(prefName.name, default as String?) as T?
                Set::class -> pref.getStringSet(prefName.name, default as Set<String>) as T?

                else -> deserializeClass(prefName.name, default, prefName.data.prefLocation)
            }
        } catch (e: Exception) {
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCustomVal(key: String, default: T): T {
        return try {
            when (default) {
                is Boolean -> irrelevantPreferences!!.getBoolean(key, default) as T
                is Int -> irrelevantPreferences!!.getInt(key, default) as T
                is Float -> irrelevantPreferences!!.getFloat(key, default) as T
                is Long -> irrelevantPreferences!!.getLong(key, default) as T
                is String -> irrelevantPreferences!!.getString(key, default) as T
                is Set<*> -> irrelevantPreferences!!.getStringSet(key, default as Set<String>) as T
                else -> throw IllegalArgumentException("Type not supported")
            }
        } catch (e: Exception) {
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getNullableCustomVal(key: String, default: T?, clazz: Class<T>): T? {
        return try {
            when {
                clazz.isAssignableFrom(Boolean::class.java) -> irrelevantPreferences!!.getBoolean(
                    key,
                    default as? Boolean ?: false
                ) as T?

                clazz.isAssignableFrom(Int::class.java) -> irrelevantPreferences!!.getInt(
                    key,
                    default as? Int ?: 0
                ) as T?

                clazz.isAssignableFrom(Float::class.java) -> irrelevantPreferences!!.getFloat(
                    key,
                    default as? Float ?: 0f
                ) as T?

                clazz.isAssignableFrom(Long::class.java) -> irrelevantPreferences!!.getLong(
                    key,
                    default as? Long ?: 0L
                ) as T?

                clazz.isAssignableFrom(String::class.java) -> irrelevantPreferences!!.getString(
                    key,
                    default as? String
                ) as T?

                clazz.isAssignableFrom(Set::class.java) -> irrelevantPreferences!!.getStringSet(
                    key,
                    default as? Set<String> ?: setOf()
                ) as T?

                else -> deserializeClass(key, default, Location.Irrelevant)
            }
        } catch (e: Exception) {
            default
        }
    }


    fun removeVal(prefName: PrefName) {
        val pref = getPrefLocation(prefName.data.prefLocation)
        with(pref.edit()) {
            remove(prefName.name)
            apply()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> setCustomVal(key: String, value: T?) {
        //for custom force irrelevant
        with(irrelevantPreferences!!.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value as Boolean)
                is Int -> putInt(key, value as Int)
                is Float -> putFloat(key, value as Float)
                is Long -> putLong(key, value as Long)
                is String -> putString(key, value as String)
                is Set<*> -> putStringSet(key, value as Set<String>)
                null -> remove(key)
                else -> serializeClass(key, value, Location.Irrelevant)
            }
            apply()
        }
    }

    fun removeCustomVal(key: String) {
        //for custom force irrelevant
        with(irrelevantPreferences!!.edit()) {
            remove(key)
            apply()
        }
    }

    /**
     * Retrieves all SharedPreferences entries with keys starting with the specified prefix.
     *
     * @param prefix The prefix to filter keys.
     * @return A map containing key-value pairs that match the prefix.
     */
    fun getAllCustomValsForMedia(prefix: String): Map<String, Any?> {
        val prefs = irrelevantPreferences ?: return emptyMap()
        val allEntries = mutableMapOf<String, Any?>()

        prefs.all.forEach { (key, value) ->
            if (key.startsWith(prefix)) {
                allEntries[key] = value
            }
        }

        return allEntries
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getLiveVal(prefName: PrefName, default: T): SharedPreferenceLiveData<T> {
        val pref = getPrefLocation(prefName.data.prefLocation)
        return when (prefName.data.type) {
            Boolean::class -> SharedPreferenceBooleanLiveData(
                pref,
                prefName.name,
                default as Boolean
            ) as SharedPreferenceLiveData<T>

            Int::class -> SharedPreferenceIntLiveData(
                pref,
                prefName.name,
                default as Int
            ) as SharedPreferenceLiveData<T>

            Float::class -> SharedPreferenceFloatLiveData(
                pref,
                prefName.name,
                default as Float
            ) as SharedPreferenceLiveData<T>

            Long::class -> SharedPreferenceLongLiveData(
                pref,
                prefName.name,
                default as Long
            ) as SharedPreferenceLiveData<T>

            String::class -> SharedPreferenceStringLiveData(
                pref,
                prefName.name,
                default as String
            ) as SharedPreferenceLiveData<T>

            Set::class -> SharedPreferenceStringSetLiveData(
                pref,
                prefName.name,
                default as Set<String>
            ) as SharedPreferenceLiveData<T>

            else -> SharedPreferenceClassLiveData(
                pref,
                prefName.name,
                default
            )
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

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> SharedPreferenceLiveData<*>.asLiveClass(): SharedPreferenceClassLiveData<T> =
        this as? SharedPreferenceClassLiveData<T>
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<T>")

    fun getAnimeDownloadPreferences(): SharedPreferences =
        animeDownloadsPreferences!!  //needs to be used externally

    fun exportAllPrefs(prefLocation: List<Location>): String {
        return PreferencePackager.pack(
            prefLocation.associateWith { getPrefLocation(it) }
        )
    }


    /**
     * @param prefs Map of preferences to import
     * @param prefLocation Location to import to
     * @return true if successful, false if error
     */

    @Suppress("UNCHECKED_CAST")
    fun importAllPrefs(prefs: Map<String, *>, prefLocation: Location): Boolean {
        val pref = getPrefLocation(prefLocation)
        var hadError = false
        pref.edit().clear().apply()
        with(pref.edit()) {
            prefs.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Float -> putFloat(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                    is HashSet<*> -> putStringSet(key, value as Set<String>)
                    is ArrayList<*> -> putStringSet(key, arrayListToSet(value))
                    is Set<*> -> putStringSet(key, value as Set<String>)
                    else -> hadError = true
                }
            }
            apply()
            return if (hadError) {
                snackString("Error importing preferences")
                false
            } else {
                snackString("Preferences imported")
                true
            }
        }
    }

    private fun arrayListToSet(arrayList: ArrayList<*>): Set<String> {
        return arrayList.map { it.toString() }.toSet()
    }

    private fun getPrefLocation(prefLoc: Location): SharedPreferences {
        return when (prefLoc) {
            Location.General -> generalPreferences
            Location.UI -> uiPreferences
            Location.Player -> playerPreferences
            Location.Reader -> readerPreferences
            Location.NovelReader -> readerPreferences
            Location.Irrelevant -> irrelevantPreferences
            Location.AnimeDownloads -> animeDownloadsPreferences
            Location.Protected -> protectedPreferences
        }!!
    }

    private fun <T> serializeClass(key: String, value: T, location: Location) {
        val pref = getPrefLocation(location)
        try {
            val bos = ByteArrayOutputStream()
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(value)
            }

            val serialized = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT)
            pref.edit().putString(key, serialized).apply()
        } catch (e: Exception) {
            snackString("Error serializing preference: ${e.message}")
            Logger.log(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> deserializeClass(key: String, default: T?, location: Location): T? {
        return try {
            val pref = getPrefLocation(location)
            val serialized = pref.getString(key, null)
            if (serialized != null) {
                val data = Base64.decode(serialized, Base64.DEFAULT)
                val bis = ByteArrayInputStream(data)
                val ois = ObjectInputStream(bis)
                val obj = ois.readObject() as T?
                obj
            } else {
                Logger.log("Serialized data is null (key: $key)")
                default
            }
        } catch (e: java.io.InvalidClassException) {
            Logger.log(e)
            try {
                getPrefLocation(location).edit().remove(key).apply()
                default
            } catch (e: Exception) {
                Logger.log(e)
                default
            }
        } catch (e: Exception) {
            Logger.log(e)
            default
        }
    }
}