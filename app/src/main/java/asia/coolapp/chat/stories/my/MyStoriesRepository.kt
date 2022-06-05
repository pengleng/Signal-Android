package asia.coolapp.chat.stories.my

import android.content.Context
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.conversation.ConversationMessage
import asia.coolapp.chat.database.DatabaseObserver
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.MessageRecord
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.sms.MessageSender

class MyStoriesRepository(context: Context) {

  private val context = context.applicationContext

  fun resend(story: MessageRecord): Completable {
    return Completable.fromAction {
      MessageSender.resend(context, story)
    }.subscribeOn(Schedulers.io())
  }

  fun getMyStories(): Observable<List<MyStoriesState.DistributionSet>> {
    return Observable.create { emitter ->
      fun refresh() {
        val storiesMap = mutableMapOf<Recipient, List<MessageRecord>>()
        SignalDatabase.mms.getAllOutgoingStories(true).use {
          while (it.next != null) {
            val messageRecord = it.current
            val currentList = storiesMap[messageRecord.recipient] ?: emptyList()
            storiesMap[messageRecord.recipient] = (currentList + messageRecord)
          }
        }

        emitter.onNext(storiesMap.map { (r, m) -> createDistributionSet(r, m) })
      }

      val observer = DatabaseObserver.Observer {
        refresh()
      }

      ApplicationDependencies.getDatabaseObserver().registerConversationListObserver(observer)
      emitter.setCancellable {
        ApplicationDependencies.getDatabaseObserver().unregisterObserver(observer)
      }

      refresh()
    }
  }

  private fun createDistributionSet(recipient: Recipient, messageRecords: List<MessageRecord>): MyStoriesState.DistributionSet {
    return MyStoriesState.DistributionSet(
      label = recipient.getDisplayName(context),
      stories = messageRecords.map {
        ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(context, it)
      }
    )
  }
}
