package asia.coolapp.chat.stories.settings.select

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.core.util.CursorUtil
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.database.RecipientDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.DistributionListId
import asia.coolapp.chat.recipients.RecipientId

class BaseStoryRecipientSelectionRepository {
  fun updateDistributionListMembership(distributionListId: DistributionListId, recipients: Set<RecipientId>) {
    SignalExecutors.BOUNDED.execute {
      val currentRecipients = SignalDatabase.distributionLists.getRawMembers(distributionListId).toSet()
      val oldNotNew = currentRecipients - recipients
      val newNotOld = recipients - currentRecipients

      oldNotNew.forEach {
        SignalDatabase.distributionLists.removeMemberFromList(distributionListId, it)
      }

      newNotOld.forEach {
        SignalDatabase.distributionLists.addMemberToList(distributionListId, it)
      }
    }
  }

  fun getListMembers(distributionListId: DistributionListId): Single<Set<RecipientId>> {
    return Single.fromCallable {
      SignalDatabase.distributionLists.getRawMembers(distributionListId).toSet()
    }.subscribeOn(Schedulers.io())
  }

  fun getAllSignalContacts(): Single<Set<RecipientId>> {
    return Single.fromCallable {
      SignalDatabase.recipients.getSignalContacts(false)?.use {
        val recipientSet = mutableSetOf<RecipientId>()
        while (it.moveToNext()) {
          recipientSet.add(RecipientId.from(CursorUtil.requireLong(it, RecipientDatabase.ID)))
        }

        recipientSet
      } ?: emptySet()
    }.subscribeOn(Schedulers.io())
  }
}
