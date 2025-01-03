package ani.dantotsu.settings.saving

import android.content.SharedPreferences
import android.util.Base64
import androidx.lifecycle.LiveData
import ani.dantotsu.util.Logger
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

abstract class SharedPreferenceLiveData<T>(
    val sharedPrefs: SharedPreferences,
    val key: String,
    val defValue: T
) : LiveData<T>() {

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == this.key) {
                value = getValueFromPreferences(key, defValue)
            }
        }

    abstract fun getValueFromPreferences(key: String, defValue: T): T

    override fun onActive() {
        super.onActive()
        value = getValueFromPreferences(key, defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        super.onInactive()
    }
}

class SharedPreferenceIntLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Int) :
    SharedPreferenceLiveData<Int>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Int): Int =
        sharedPrefs.getInt(key, defValue)
}

class SharedPreferenceStringLiveData(
    sharedPrefs: SharedPreferences,
    key: String,
    defValue: String
) :
    SharedPreferenceLiveData<String>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: String): String =
        sharedPrefs.getString(key, defValue).toString()
}

class SharedPreferenceBooleanLiveData(
    sharedPrefs: SharedPreferences,
    key: String,
    defValue: Boolean
) :
    SharedPreferenceLiveData<Boolean>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Boolean): Boolean =
        sharedPrefs.getBoolean(key, defValue)
}

class SharedPreferenceFloatLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Float) :
    SharedPreferenceLiveData<Float>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Float): Float =
        sharedPrefs.getFloat(key, defValue)
}

class SharedPreferenceLongLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Long) :
    SharedPreferenceLiveData<Long>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Long): Long =
        sharedPrefs.getLong(key, defValue)
}

class SharedPreferenceStringSetLiveData(
    sharedPrefs: SharedPreferences,
    key: String,
    defValue: Set<String>
) :
    SharedPreferenceLiveData<Set<String>>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Set<String>): Set<String> =
        sharedPrefs.getStringSet(key, defValue)?.toSet() ?: defValue
}

@Suppress("UNCHECKED_CAST")
class SharedPreferenceClassLiveData<T>(
    sharedPrefs: SharedPreferences,
    key: String,
    defValue: T
) : SharedPreferenceLiveData<T>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: T): T {
        return try {
            val serialized = sharedPrefs.getString(key, null)
            if (serialized != null) {
                val data = Base64.decode(serialized, Base64.DEFAULT)
                val bis = ByteArrayInputStream(data)
                val ois = ObjectInputStream(bis)
                val obj = ois.readObject() as T
                obj
            } else {
                Logger.log("Serialized data is null (key: $key)")
                defValue
            }
        } catch (e: java.io.InvalidClassException) {
            Logger.log(e)
            try {
                sharedPrefs.edit().remove(key).apply()
                defValue
            } catch (e: Exception) {
                Logger.log(e)
                defValue
            }
        } catch (e: Exception) {
            Logger.log(e)
            defValue
        }
    }
}

@Suppress("unused")
fun SharedPreferences.intLiveData(key: String, defValue: Int): SharedPreferenceLiveData<Int> {
    return SharedPreferenceIntLiveData(this, key, defValue)
}

@Suppress("unused")

fun SharedPreferences.stringLiveData(
    key: String,
    defValue: String
): SharedPreferenceLiveData<String> {
    return SharedPreferenceStringLiveData(this, key, defValue)
}

@Suppress("unused")
fun SharedPreferences.booleanLiveData(
    key: String,
    defValue: Boolean
): SharedPreferenceLiveData<Boolean> {
    return SharedPreferenceBooleanLiveData(this, key, defValue)
}

@Suppress("unused")
fun SharedPreferences.floatLiveData(key: String, defValue: Float): SharedPreferenceLiveData<Float> {
    return SharedPreferenceFloatLiveData(this, key, defValue)
}

@Suppress("unused")
fun SharedPreferences.longLiveData(key: String, defValue: Long): SharedPreferenceLiveData<Long> {
    return SharedPreferenceLongLiveData(this, key, defValue)
}

@Suppress("unused")
fun SharedPreferences.stringSetLiveData(
    key: String,
    defValue: Set<String>
): SharedPreferenceLiveData<Set<String>> {
    return SharedPreferenceStringSetLiveData(this, key, defValue)
}