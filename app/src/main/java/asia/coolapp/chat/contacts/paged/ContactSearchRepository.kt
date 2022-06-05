package asia.coolapp.chat.contacts.paged

import io.reactivex.rxjava3.core.Single
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId

class ContactSearchRepository {
  fun filterOutUnselectableContactSearchKeys(contactSearchKeys: Set<ContactSearchKey>): Single<Set<ContactSearchSelectionResult>> {
    return Single.fromCallable {
      contactSearchKeys.map {
        val isSelectable = when (it) {
          is ContactSearchKey.Expand -> false
          is ContactSearchKey.Header -> false
          is ContactSearchKey.KnownRecipient -> canSelectRecipient(it.recipientId)
          is ContactSearchKey.Story -> canSelectRecipient(it.recipientId)
        }
        ContactSearchSelectionResult(it, isSelectable)
      }.toSet()
    }
  }

  private fun canSelectRecipient(recipientId: RecipientId): Boolean {
    val recipient = Recipient.resolved(recipientId)
    return if (recipient.isPushV2Group) {
      val record = SignalDatabase.groups.getGroup(recipient.requireGroupId())
      !(record.isPresent && record.get().isAnnouncementGroup && !record.get().isAdmin(Recipient.self()))
    } else {
      true
    }
  }
}
