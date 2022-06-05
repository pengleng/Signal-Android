package asia.coolapp.chat.badges.models

import android.view.View
import asia.coolapp.chat.R
import asia.coolapp.chat.badges.BadgeImageView
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

object ExpiredBadge {

  class Model(val badge: Badge) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return newItem.badge.id == badge.id
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return super.areContentsTheSame(newItem) && newItem.badge == badge
    }
  }

  class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val badge: BadgeImageView = itemView.findViewById(R.id.expired_badge)

    override fun bind(model: Model) {
      badge.setBadge(model.badge)
    }
  }

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory({ ViewHolder(it) }, R.layout.expired_badge_preference))
  }
}
