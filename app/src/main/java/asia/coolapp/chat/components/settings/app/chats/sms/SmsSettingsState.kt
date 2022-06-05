package asia.coolapp.chat.components.settings.app.chats.sms

data class SmsSettingsState(
  val useAsDefaultSmsApp: Boolean,
  val smsDeliveryReportsEnabled: Boolean,
  val wifiCallingCompatibilityEnabled: Boolean
)
