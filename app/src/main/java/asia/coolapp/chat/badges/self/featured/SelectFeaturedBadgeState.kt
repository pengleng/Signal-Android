package asia.coolapp.chat.badges.self.featured

import asia.coolapp.chat.badges.models.Badge

data class SelectFeaturedBadgeState(
  val stage: Stage = Stage.INIT,
  val selectedBadge: Badge? = null,
  val allUnlockedBadges: List<Badge> = listOf()
) {
  enum class Stage {
    INIT,
    READY,
    SAVING
  }
}
