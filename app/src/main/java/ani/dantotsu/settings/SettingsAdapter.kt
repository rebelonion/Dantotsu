package ani.dantotsu.settings

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.setAnimation

class SettingsAdapter(private val settings: ArrayList<Settings>) : RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>(){
    inner class SettingsViewHolder(val binding: ItemSettingsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                ContextCompat.startActivity(
                    binding.root.context, Intent(binding.root.context, settings[bindingAdapterPosition].activity),
                         null
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        return SettingsViewHolder(
            ItemSettingsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val b = holder.binding
        setAnimation(b.root.context, b.root)
        val settings = settings[position]
        b.settingsTitle.text = settings.name
        b.settingsDesc.text = settings.desc
        b.settingsIcon.setImageDrawable(ContextCompat.getDrawable(b.root.context, settings.icon))
    }

    override fun getItemCount(): Int = settings.size
}