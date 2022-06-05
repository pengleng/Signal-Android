package asia.coolapp.chat.stories.viewer.reply.group

import asia.coolapp.chat.conversation.colors.NameColor
import asia.coolapp.chat.recipients.RecipientId

data class StoryGroupReplyState(
  val noReplies: Boolean = true,
  val nameColors: Map<RecipientId, NameColor> = emptyMap(),
  val loadState: LoadState = LoadState.INIT
) {
  enum class LoadState {
    INIT,
    READY
  }
}
