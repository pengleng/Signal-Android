package asia.coolapp.chat.badges.models

import android.view.View
import asia.coolapp.chat.R
import asia.coolapp.chat.badges.BadgeImageView
import asia.coolapp.chat.components.AvatarImageView
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

object BadgePreview {

  fun register(mappingAdapter: MappingAdapter) {
    mappingAdapter.registerFactory(Model::class.java, LayoutFactory({ ViewHolder(it) }, R.layout.featured_badge_preview_preference))
    mappingAdapter.registerFactory(SubscriptionModel::class.java, LayoutFactory({ ViewHolder(it) }, R.layout.subscription_flow_badge_preview_preference))
  }

  abstract class BadgeModel<T : BadgeModel<T>> : PreferenceModel<T>() {
    abstract val badge: Badge?
  }

  data class Model(override val badge: Badge?) : BadgeModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return true
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return super.areContentsTheSame(newItem) && badge == newItem.badge
    }

    override fun getChangePayload(newItem: Model): Any? {
      return Unit
    }
  }

  data class SubscriptionModel(override val badge: Badge?) : BadgeModel<SubscriptionModel>() {
    override fun areItemsTheSame(newItem: SubscriptionModel): Boolean {
      return true
    }

    override fun areContentsTheSame(newItem: SubscriptionModel): Boolean {
      return super.areContentsTheSame(newItem) && badge == newItem.badge
    }

    override fun getChangePayload(newItem: SubscriptionModel): Any? {
      return Unit
    }
  }

  class ViewHolder<T : BadgeModel<T>>(itemView: View) : MappingViewHolder<T>(itemView) {

    private val avatar: AvatarImageView = itemView.findViewById(R.id.avatar)
    private val badge: BadgeImageView = itemView.findViewById(R.id.badge)

    override fun bind(model: T) {
      if (payload.isEmpty()) {
        avatar.setRecipient(Recipient.self())
        avatar.disableQuickContact()
      }

      badge.setBadge(model.badge)
    }
  }
}
