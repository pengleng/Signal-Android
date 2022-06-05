package asia.coolapp.chat.avatar.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import asia.coolapp.chat.R
import asia.coolapp.chat.components.AvatarImageView
import asia.coolapp.chat.database.model.StoryViewState
import asia.coolapp.chat.mms.GlideRequests
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.stories.Stories
import asia.coolapp.chat.util.visible

/**
 * AvatarView encapsulating the AvatarImageView and decorations.
 */
class AvatarView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

  init {
    inflate(context, R.layout.avatar_view, this)

    isClickable = false
  }

  private val avatar: AvatarImageView = findViewById<AvatarImageView>(R.id.avatar_image_view).apply {
    initialize(context, attrs)
  }

  private val storyRing: View = findViewById(R.id.avatar_story_ring)

  private fun showStoryRing(hasUnreadStory: Boolean) {
    if (!Stories.isFeatureEnabled()) {
      return
    }

    storyRing.visible = true
    storyRing.isActivated = hasUnreadStory

    avatar.scaleX = 0.82f
    avatar.scaleY = 0.82f
  }

  private fun hideStoryRing() {
    storyRing.visible = false

    avatar.scaleX = 1f
    avatar.scaleY = 1f
  }

  fun setStoryRingFromState(storyViewState: StoryViewState) {
    when (storyViewState) {
      StoryViewState.NONE -> hideStoryRing()
      StoryViewState.UNVIEWED -> showStoryRing(true)
      StoryViewState.VIEWED -> showStoryRing(false)
    }
  }

  /**
   * Displays Note-to-Self
   */
  fun displayChatAvatar(recipient: Recipient) {
    avatar.setAvatar(recipient)
  }

  /**
   * Displays Note-to-Self
   */
  fun displayChatAvatar(requestManager: GlideRequests, recipient: Recipient, isQuickContactEnabled: Boolean) {
    avatar.setAvatar(requestManager, recipient, isQuickContactEnabled)
  }

  /**
   * Displays Profile image
   */
  fun displayProfileAvatar(recipient: Recipient) {
    avatar.setRecipient(recipient)
  }

  fun setFallbackPhotoProvider(fallbackPhotoProvider: Recipient.FallbackPhotoProvider) {
    avatar.setFallbackPhotoProvider(fallbackPhotoProvider)
  }

  fun disableQuickContact() {
    avatar.disableQuickContact()
  }
}
