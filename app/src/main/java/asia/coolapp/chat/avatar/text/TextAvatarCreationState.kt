package asia.coolapp.chat.avatar.text

import asia.coolapp.chat.avatar.Avatar
import asia.coolapp.chat.avatar.AvatarColorItem
import asia.coolapp.chat.avatar.Avatars

data class TextAvatarCreationState(
  val currentAvatar: Avatar.Text,
) {
  fun colors(): List<AvatarColorItem> = Avatars.colors.map { AvatarColorItem(it, currentAvatar.color == it) }
}
