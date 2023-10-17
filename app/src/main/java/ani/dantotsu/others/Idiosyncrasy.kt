@file:Suppress("UNCHECKED_CAST", "DEPRECATION")

package ani.dantotsu.others

import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.io.Serializable

inline fun <reified T : Serializable> Bundle.getSerialized(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.getSerializable(key, T::class.java)
    else
        this.getSerializable(key) as? T
}

inline fun <reified T : Serializable> Intent.getSerialized(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.getSerializableExtra(key, T::class.java)
    else
        this.getSerializableExtra(key) as? T
}