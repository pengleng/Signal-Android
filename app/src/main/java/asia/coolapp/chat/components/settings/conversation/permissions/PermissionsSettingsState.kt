package asia.coolapp.chat.components.settings.conversation.permissions

data class PermissionsSettingsState(
  val selfCanEditSettings: Boolean = false,
  val nonAdminCanAddMembers: Boolean = false,
  val nonAdminCanEditGroupInfo: Boolean = false,
  val announcementGroupPermissionEnabled: Boolean = false,
  val announcementGroup: Boolean = false
)
