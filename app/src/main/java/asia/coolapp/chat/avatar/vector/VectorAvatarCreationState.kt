package asia.coolapp.chat.avatar.vector

import asia.coolapp.chat.avatar.Avatar
import asia.coolapp.chat.avatar.AvatarColorItem
import asia.coolapp.chat.avatar.Avatars

data class VectorAvatarCreationState(
  val currentAvatar: Avatar.Vector,
) {
  fun colors(): List<AvatarColorItem> = Avatars.colors.map { AvatarColorItem(it, currentAvatar.color == it) }
}
