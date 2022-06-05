package asia.coolapp.chat.components.settings.app.privacy

import android.content.Context
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.DistributionListPartialRecord
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.jobs.MultiDeviceConfigurationUpdateJob
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.storage.StorageSyncHelper
import asia.coolapp.chat.util.TextSecurePreferences

class PrivacySettingsRepository {

  private val context: Context = ApplicationDependencies.getApplication()

  fun getBlockedCount(consumer: (Int) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      val recipientDatabase = SignalDatabase.recipients

      consumer(recipientDatabase.getBlocked().count)
    }
  }

  fun getPrivateStories(consumer: (List<DistributionListPartialRecord>) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      consumer(SignalDatabase.distributionLists.getCustomListsForUi())
    }
  }

  fun syncReadReceiptState() {
    SignalExecutors.BOUNDED.execute {
      SignalDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
      ApplicationDependencies.getJobManager().add(
        MultiDeviceConfigurationUpdateJob(
          TextSecurePreferences.isReadReceiptsEnabled(context),
          TextSecurePreferences.isTypingIndicatorsEnabled(context),
          TextSecurePreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
          SignalStore.settings().isLinkPreviewsEnabled
        )
      )
    }
  }

  fun syncTypingIndicatorsState() {
    val enabled = TextSecurePreferences.isTypingIndicatorsEnabled(context)

    SignalDatabase.recipients.markNeedsSync(Recipient.self().id)
    StorageSyncHelper.scheduleSyncForDataChange()
    ApplicationDependencies.getJobManager().add(
      MultiDeviceConfigurationUpdateJob(
        TextSecurePreferences.isReadReceiptsEnabled(context),
        enabled,
        TextSecurePreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
        SignalStore.settings().isLinkPreviewsEnabled
      )
    )

    if (!enabled) {
      ApplicationDependencies.getTypingStatusRepository().clear()
    }
  }
}
