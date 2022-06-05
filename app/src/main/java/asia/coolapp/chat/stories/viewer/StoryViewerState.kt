package asia.coolapp.chat.stories.viewer

import asia.coolapp.chat.recipients.RecipientId

data class StoryViewerState(
  val pages: List<RecipientId> = emptyList(),
  val previousPage: Int = -1,
  val page: Int = -1
)
