package asia.coolapp.chat.stories.viewer.text

import asia.coolapp.chat.database.model.databaseprotos.StoryTextPost
import asia.coolapp.chat.linkpreview.LinkPreview

data class StoryTextPostState(
  val storyTextPost: StoryTextPost? = null,
  val linkPreview: LinkPreview? = null,
  val loadState: LoadState = LoadState.INIT
) {
  enum class LoadState {
    INIT,
    LOADED,
    FAILED
  }
}
