package ani.dantotsu.settings

import android.app.Activity

data class Settings(
    val name : String,
    val icon : Int,
    val desc: String,
    val activity: Class<out Activity>
)
