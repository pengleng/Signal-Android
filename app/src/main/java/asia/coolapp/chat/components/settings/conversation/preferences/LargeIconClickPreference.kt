package asia.coolapp.chat.components.settings.conversation.preferences

import android.view.View
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLSettingsIcon
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.components.settings.PreferenceViewHolder
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter

/**
 * Renders a preference line item with a larger (40dp) icon
 */
object LargeIconClickPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.large_icon_preference_item))
  }

  class Model(
    override val title: DSLSettingsText?,
    override val icon: DSLSettingsIcon,
    override val summary: DSLSettingsText? = null,
    val onClick: () -> Unit
  ) : PreferenceModel<Model>()

  private class ViewHolder(itemView: View) : PreferenceViewHolder<Model>(itemView) {
    override fun bind(model: Model) {
      super.bind(model)
      itemView.setOnClickListener { model.onClick() }
    }
  }
}
