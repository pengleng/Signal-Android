package asia.coolapp.chat.stories.landing

data class StoriesLandingState(
  val storiesLandingItems: List<StoriesLandingItemData> = emptyList(),
  val displayMyStoryItem: Boolean = false,
  val isHiddenContentVisible: Boolean = false,
  val loadingState: LoadingState = LoadingState.INIT
) {
  enum class LoadingState {
    INIT,
    LOADED
  }

  val hasNoStories: Boolean = loadingState == LoadingState.LOADED && storiesLandingItems.isEmpty()
}
