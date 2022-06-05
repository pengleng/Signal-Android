package asia.coolapp.chat.components.settings.conversation.permissions

import asia.coolapp.chat.groups.ui.GroupChangeFailureReason

sealed class PermissionsSettingsEvents {
  class GroupChangeError(val reason: GroupChangeFailureReason) : PermissionsSettingsEvents()
}
