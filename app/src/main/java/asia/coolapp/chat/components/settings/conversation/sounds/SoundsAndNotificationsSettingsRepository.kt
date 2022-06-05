package asia.coolapp.chat.components.settings.conversation.sounds

import android.content.Context
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.database.RecipientDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.notifications.NotificationChannels
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId

class SoundsAndNotificationsSettingsRepository(private val context: Context) {

  fun ensureCustomChannelConsistency(complete: () -> Unit) {
    SignalExecutors.BOUNDED.execute {
      if (NotificationChannels.supported()) {
        NotificationChannels.ensureCustomChannelConsistency(context)
      }
      complete()
    }
  }

  fun setMuteUntil(recipientId: RecipientId, muteUntil: Long) {
    SignalExecutors.BOUNDED.execute {
      SignalDatabase.recipients.setMuted(recipientId, muteUntil)
    }
  }

  fun setMentionSetting(recipientId: RecipientId, mentionSetting: RecipientDatabase.MentionSetting) {
    SignalExecutors.BOUNDED.execute {
      SignalDatabase.recipients.setMentionSetting(recipientId, mentionSetting)
    }
  }

  fun hasCustomNotificationSettings(recipientId: RecipientId, consumer: (Boolean) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      val recipient = Recipient.resolved(recipientId)
      consumer(
        if (recipient.notificationChannel != null || !NotificationChannels.supported()) {
          true
        } else {
          NotificationChannels.updateWithShortcutBasedChannel(context, recipient)
        }
      )
    }
  }
}
