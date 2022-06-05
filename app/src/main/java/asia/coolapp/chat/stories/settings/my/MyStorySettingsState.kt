package asia.coolapp.chat.stories.settings.my

data class MyStorySettingsState(
  val hiddenStoryFromCount: Int = 0,
  val areRepliesAndReactionsEnabled: Boolean = false
)
