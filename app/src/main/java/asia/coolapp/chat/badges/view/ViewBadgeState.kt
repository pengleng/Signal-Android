package asia.coolapp.chat.badges.view

import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.recipients.Recipient

data class ViewBadgeState(
  val allBadgesVisibleOnProfile: List<Badge> = listOf(),
  val badgeLoadState: LoadState = LoadState.INIT,
  val selectedBadge: Badge? = null,
  val recipient: Recipient? = null
) {
  enum class LoadState {
    INIT,
    LOADED
  }
}
