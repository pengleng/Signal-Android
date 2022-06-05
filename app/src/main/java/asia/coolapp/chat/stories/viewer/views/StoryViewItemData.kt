package asia.coolapp.chat.stories.viewer.views

import asia.coolapp.chat.recipients.Recipient

data class StoryViewItemData(
  val recipient: Recipient,
  val timeViewedInMillis: Long
)
