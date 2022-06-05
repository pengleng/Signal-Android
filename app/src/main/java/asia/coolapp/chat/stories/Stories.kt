package asia.coolapp.chat.stories

import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentManager
import io.reactivex.rxjava3.core.Completable
import asia.coolapp.chat.R
import asia.coolapp.chat.contacts.HeaderAction
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.mediasend.v2.stories.ChooseStoryTypeBottomSheet
import asia.coolapp.chat.mms.OutgoingSecureMediaMessage
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.recipients.RecipientUtil
import asia.coolapp.chat.sms.MessageSender
import asia.coolapp.chat.util.BottomSheetUtil
import asia.coolapp.chat.util.FeatureFlags

object Stories {
  @JvmStatic
  fun isFeatureAvailable(): Boolean {
    return FeatureFlags.stories() && Recipient.self().storiesCapability == Recipient.Capability.SUPPORTED
  }

  @JvmStatic
  fun isFeatureEnabled(): Boolean {
    return isFeatureAvailable() && !SignalStore.storyValues().isFeatureDisabled
  }

  fun getHeaderAction(fragmentManager: FragmentManager): HeaderAction {
    return HeaderAction(
      R.string.ContactsCursorLoader_new_story,
      R.drawable.ic_plus_20
    ) {
      ChooseStoryTypeBottomSheet().show(fragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG)
    }
  }

  @WorkerThread
  fun sendTextStories(messages: List<OutgoingSecureMediaMessage>): Completable {
    return Completable.create { emitter ->
      MessageSender.sendMediaBroadcast(ApplicationDependencies.getApplication(), messages, listOf(), listOf())
      emitter.onComplete()
    }
  }

  @JvmStatic
  fun getRecipientsToSendTo(messageId: Long, sentTimestamp: Long, allowsReplies: Boolean): List<Recipient> {
    val recipientIds: List<RecipientId> = SignalDatabase.storySends.getRecipientsToSendTo(messageId, sentTimestamp, allowsReplies)

    return RecipientUtil.getEligibleForSending(recipientIds.map(Recipient::resolved))
  }
}
