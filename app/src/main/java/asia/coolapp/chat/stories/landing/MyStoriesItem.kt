package asia.coolapp.chat.stories.landing

import android.view.View
import asia.coolapp.chat.R
import asia.coolapp.chat.avatar.view.AvatarView
import asia.coolapp.chat.badges.BadgeImageView
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

/**
 * Item displayed on an empty Stories landing page allowing the user to add a new story.
 */
object MyStoriesItem {

  fun register(mappingAdapter: MappingAdapter) {
    mappingAdapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.stories_landing_item_my_stories))
  }

  class Model(
    val onClick: () -> Unit
  ) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean = true
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val avatarView: AvatarView = itemView.findViewById(R.id.avatar)
    private val badgeView: BadgeImageView = itemView.findViewById(R.id.badge)

    override fun bind(model: Model) {
      itemView.setOnClickListener { model.onClick() }

      avatarView.displayProfileAvatar(Recipient.self())
      badgeView.setBadgeFromRecipient(Recipient.self())
    }
  }
}
