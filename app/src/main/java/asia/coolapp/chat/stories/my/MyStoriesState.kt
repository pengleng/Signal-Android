package asia.coolapp.chat.stories.my

import asia.coolapp.chat.conversation.ConversationMessage

data class MyStoriesState(
  val distributionSets: List<DistributionSet> = emptyList()
) {

  data class DistributionSet(
    val label: String?,
    val stories: List<ConversationMessage>
  )
}
