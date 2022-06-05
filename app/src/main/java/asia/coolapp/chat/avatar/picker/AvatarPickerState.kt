package asia.coolapp.chat.avatar.picker

import asia.coolapp.chat.avatar.Avatar

data class AvatarPickerState(
  val currentAvatar: Avatar? = null,
  val selectableAvatars: List<Avatar> = listOf(),
  val canSave: Boolean = false,
  val canClear: Boolean = false,
  val isCleared: Boolean = false
)
