package asia.coolapp.chat.components.settings.models

import android.view.View
import com.google.android.material.button.MaterialButton
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLSettingsIcon
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

object Button {

  fun register(mappingAdapter: MappingAdapter) {
    mappingAdapter.registerFactory(Model.Primary::class.java, LayoutFactory({ ViewHolder(it) as MappingViewHolder<Model.Primary> }, R.layout.dsl_button_primary))
    mappingAdapter.registerFactory(Model.SecondaryNoOutline::class.java, LayoutFactory({ ViewHolder(it) as MappingViewHolder<Model.SecondaryNoOutline> }, R.layout.dsl_button_secondary))
  }

  sealed class Model<T : Model<T>>(
    title: DSLSettingsText?,
    icon: DSLSettingsIcon?,
    isEnabled: Boolean,
    val onClick: () -> Unit
  ) : PreferenceModel<T>(
    title = title,
    icon = icon,
    isEnabled = isEnabled
  ) {
    class Primary(
      title: DSLSettingsText?,
      icon: DSLSettingsIcon?,
      isEnabled: Boolean,
      onClick: () -> Unit
    ) : Model<Primary>(title, icon, isEnabled, onClick)

    class SecondaryNoOutline(
      title: DSLSettingsText?,
      icon: DSLSettingsIcon?,
      isEnabled: Boolean,
      onClick: () -> Unit
    ) : Model<SecondaryNoOutline>(title, icon, isEnabled, onClick)
  }

  class ViewHolder(itemView: View) : MappingViewHolder<Model<*>>(itemView) {

    private val button: MaterialButton = itemView as MaterialButton

    override fun bind(model: Model<*>) {
      button.text = model.title?.resolve(context)
      button.setOnClickListener {
        model.onClick()
      }
      button.icon = model.icon?.resolve(context)
      button.isEnabled = model.isEnabled
    }
  }
}
