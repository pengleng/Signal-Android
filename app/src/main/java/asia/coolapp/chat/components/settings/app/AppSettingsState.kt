package asia.coolapp.chat.components.settings.app

import asia.coolapp.chat.recipients.Recipient

data class AppSettingsState(
  val self: Recipient,
  val unreadPaymentsCount: Int,
  val hasActiveSubscription: Boolean
)
