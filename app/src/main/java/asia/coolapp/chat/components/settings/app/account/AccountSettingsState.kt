package asia.coolapp.chat.components.settings.app.account

data class AccountSettingsState(
  val hasPin: Boolean,
  val pinRemindersEnabled: Boolean,
  val registrationLockEnabled: Boolean
)
