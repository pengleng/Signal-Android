package asia.coolapp.chat.stories.landing

import asia.coolapp.chat.conversation.ConversationMessage
import asia.coolapp.chat.database.model.StoryViewState
import asia.coolapp.chat.recipients.Recipient

/**
 * Data required by each row of the Stories Landing Page for proper rendering.
 */
data class StoriesLandingItemData(
  val storyViewState: StoryViewState,
  val hasReplies: Boolean,
  val hasRepliesFromSelf: Boolean,
  val isHidden: Boolean,
  val primaryStory: ConversationMessage,
  val secondaryStory: ConversationMessage?,
  val storyRecipient: Recipient,
  val individualRecipient: Recipient = primaryStory.messageRecord.individualRecipient,
  val dateInMilliseconds: Long = primaryStory.messageRecord.dateSent
) : Comparable<StoriesLandingItemData> {
  override fun compareTo(other: StoriesLandingItemData): Int {
    return if (storyRecipient.isMyStory && !other.storyRecipient.isMyStory) {
      -1
    } else if (!storyRecipient.isMyStory && other.storyRecipient.isMyStory) {
      1
    } else {
      -dateInMilliseconds.compareTo(other.dateInMilliseconds)
    }
  }
}
