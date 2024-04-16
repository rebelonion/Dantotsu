package ani.dantotsu.settings

import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.databinding.ItemSettingsSwitchBinding
import java.lang.reflect.Array

data class Settings(
    val type: Int,
    val name : String,
    val desc: String,
    val icon : Int,
    val onClick: ((ItemSettingsBinding) -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
    var isChecked : Boolean = false,
    val switch: ((isChecked:Boolean , view: ItemSettingsSwitchBinding ) -> Unit)? = null,
    val isVisible: Boolean = true,
    val isActivity: Boolean = false
)