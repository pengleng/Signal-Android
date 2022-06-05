package asia.coolapp.chat.stories.settings.custom

import asia.coolapp.chat.database.model.DistributionListRecord

data class PrivateStorySettingsState(
  val privateStory: DistributionListRecord? = null,
  val areRepliesAndReactionsEnabled: Boolean = false
)
