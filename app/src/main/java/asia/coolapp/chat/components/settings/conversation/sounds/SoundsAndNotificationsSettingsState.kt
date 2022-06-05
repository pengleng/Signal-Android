package asia.coolapp.chat.components.settings.conversation.sounds

import asia.coolapp.chat.database.RecipientDatabase
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId

data class SoundsAndNotificationsSettingsState(
  val recipientId: RecipientId = Recipient.UNKNOWN.id,
  val muteUntil: Long = 0L,
  val mentionSetting: RecipientDatabase.MentionSetting = RecipientDatabase.MentionSetting.DO_NOT_NOTIFY,
  val hasCustomNotificationSettings: Boolean = false,
  val hasMentionsSupport: Boolean = false,
  val channelConsistencyCheckComplete: Boolean = false
)
