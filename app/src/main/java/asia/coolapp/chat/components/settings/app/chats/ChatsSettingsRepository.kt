package asia.coolapp.chat.components.settings.app.chats

import android.content.Context
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.jobs.MultiDeviceConfigurationUpdateJob
import asia.coolapp.chat.jobs.MultiDeviceContactUpdateJob
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.storage.StorageSyncHelper
import asia.coolapp.chat.util.TextSecurePreferences

class ChatsSettingsRepository {

  private val context: Context = ApplicationDependencies.getApplication()

  fun syncLinkPreviewsState() {
    SignalExecutors.BOUNDED.execute {
      val isLinkPreviewsEnabled = SignalStore.settings().isLinkPreviewsEnabled

      SignalDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
      ApplicationDependencies.getJobManager().add(
        MultiDeviceConfigurationUpdateJob(
          TextSecurePreferences.isReadReceiptsEnabled(context),
          TextSecurePreferences.isTypingIndicatorsEnabled(context),
          TextSecurePreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
          isLinkPreviewsEnabled
        )
      )
    }
  }

  fun syncPreferSystemContactPhotos() {
    SignalExecutors.BOUNDED.execute {
      SignalDatabase.recipients.markNeedsSync(Recipient.self().id)
      ApplicationDependencies.getJobManager().add(MultiDeviceContactUpdateJob(true))
      StorageSyncHelper.scheduleSyncForDataChange()
    }
  }
}
