package asia.coolapp.chat.stories.settings.story

import asia.coolapp.chat.database.model.DistributionListPartialRecord

data class StorySettingsState(
  val privateStories: List<DistributionListPartialRecord> = emptyList()
)
