package asia.coolapp.chat.components.settings.conversation.preferences

import android.view.View
import androidx.core.view.ViewCompat
import asia.coolapp.chat.R
import asia.coolapp.chat.avatar.view.AvatarView
import asia.coolapp.chat.badges.BadgeImageView
import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.contacts.avatars.FallbackContactPhoto
import asia.coolapp.chat.contacts.avatars.FallbackPhoto
import asia.coolapp.chat.database.model.StoryViewState
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.util.ViewUtil
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

/**
 * Renders a large avatar (80dp) for a given Recipient.
 */
object AvatarPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.conversation_settings_avatar_preference_item))
  }

  class Model(
    val recipient: Recipient,
    val storyViewState: StoryViewState,
    val onAvatarClick: (View) -> Unit,
    val onBadgeClick: (Badge) -> Unit
  ) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return recipient == newItem.recipient
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return super.areContentsTheSame(newItem) && recipient.hasSameContent(newItem.recipient)
    }
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {
    private val avatar: AvatarView = itemView.findViewById<AvatarView>(R.id.bio_preference_avatar).apply {
      setFallbackPhotoProvider(AvatarPreferenceFallbackPhotoProvider())
    }

    private val badge: BadgeImageView = itemView.findViewById(R.id.bio_preference_badge)

    init {
      ViewCompat.setTransitionName(avatar.parent as View, "avatar")
    }

    override fun bind(model: Model) {
      if (model.recipient.isSelf) {
        badge.setBadge(null)
        badge.setOnClickListener(null)
      } else {
        badge.setBadgeFromRecipient(model.recipient)
        badge.setOnClickListener {
          val badge = model.recipient.badges.firstOrNull()
          if (badge != null) {
            model.onBadgeClick(badge)
          }
        }
      }

      avatar.setStoryRingFromState(model.storyViewState)
      avatar.displayChatAvatar(model.recipient)
      avatar.disableQuickContact()
      avatar.setOnClickListener { model.onAvatarClick(avatar) }
    }
  }

  private class AvatarPreferenceFallbackPhotoProvider : Recipient.FallbackPhotoProvider() {
    override fun getPhotoForGroup(): FallbackContactPhoto {
      return FallbackPhoto(R.drawable.ic_group_outline_40, ViewUtil.dpToPx(8))
    }
  }
}
