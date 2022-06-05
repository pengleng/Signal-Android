package asia.coolapp.chat.stories.viewer.reply.direct

import asia.coolapp.chat.database.model.MessageRecord
import asia.coolapp.chat.recipients.Recipient

data class StoryDirectReplyState(
  val recipient: Recipient? = null,
  val storyRecord: MessageRecord? = null
)
