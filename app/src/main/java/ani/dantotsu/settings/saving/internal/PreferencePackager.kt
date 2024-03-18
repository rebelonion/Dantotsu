package ani.dantotsu.settings.saving.internal

import android.content.SharedPreferences
import ani.dantotsu.settings.saving.PrefManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencePackager {
    //map one or more preference maps for import/export

    companion object {

        /**
         * @return a json string of the packed preferences
         */
        fun pack(map: Map<Location, SharedPreferences>): String {
            val prefsMap = packagePreferences(map)
            val gson = Gson()
            return gson.toJson(prefsMap)
        }

        /**
         * @return true if successful, false if error
         */
        fun unpack(decryptedJson: String): Boolean {
            val gson = Gson()
            val type = object :
                TypeToken<Map<String, Map<String, Map<String, Any>>>>() {}.type  //oh god...
            val rawPrefsMap: Map<String, Map<String, Map<String, Any>>> =
                gson.fromJson(decryptedJson, type)


            val deserializedMap = mutableMapOf<String, Map<String, Any?>>()

            rawPrefsMap.forEach { (prefName, prefValueMap) ->
                val innerMap = mutableMapOf<String, Any?>()

                prefValueMap.forEach { (key, typeValueMap) ->

                    val typeName = typeValueMap["type"] as? String
                    val value = typeValueMap["value"]

                    innerMap[key] =
                        when (typeName) {  //weirdly null sometimes so cast to string
                            "kotlin.Int" -> (value as? Double)?.toInt()
                            "kotlin.String" -> value.toString()
                            "kotlin.Boolean" -> value as? Boolean
                            "kotlin.Float" -> value.toString().toFloatOrNull()
                            "kotlin.Long" -> (value as? Double)?.toLong()
                            "java.util.HashSet" -> value as? ArrayList<*>
                            else -> null
                        }
                }
                deserializedMap[prefName] = innerMap
            }
            return unpackagePreferences(deserializedMap)
        }

        /**
         * @return a map of location names to a map of preference names to their values
         */
        private fun packagePreferences(map: Map<Location, SharedPreferences>): Map<String, Map<String, *>> {
            val result = mutableMapOf<String, Map<String, *>>()
            for ((location, preferences) in map) {
                val prefMap = mutableMapOf<String, Any>()
                preferences.all.forEach { (key, value) ->
                    val typeValueMap = mapOf(
                        "type" to value?.javaClass?.kotlin?.qualifiedName,
                        "value" to value
                    )
                    prefMap[key] = typeValueMap
                }
                result[location.name] = prefMap
            }
            return result
        }

        /**
         * @return true if successful, false if error
         */
        private fun unpackagePreferences(map: Map<String, Map<String, *>>): Boolean {
            var success = true
            map.forEach { (location, prefMap) ->
                val locationEnum = locationFromString(location)
                if (!PrefManager.importAllPrefs(prefMap, locationEnum))
                    success = false
            }
            return success
        }

        private fun locationFromString(location: String): Location {
            val loc = Location.entries.find { it.name == location }
            return loc ?: throw IllegalArgumentException("Location not found")
        }
    }
}