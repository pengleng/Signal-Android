package asia.coolapp.chat.stories.viewer.views

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.DatabaseObserver
import asia.coolapp.chat.database.GroupReceiptDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.recipients.Recipient

class StoryViewsRepository {
  fun getViews(storyId: Long): Observable<List<StoryViewItemData>> {
    return Observable.create<List<StoryViewItemData>> { emitter ->
      fun refresh() {
        emitter.onNext(
          SignalDatabase.groupReceipts.getGroupReceiptInfo(storyId).filter {
            it.status == GroupReceiptDatabase.STATUS_VIEWED
          }.map {
            StoryViewItemData(
              recipient = Recipient.resolved(it.recipientId),
              timeViewedInMillis = it.timestamp
            )
          }
        )
      }

      val observer = DatabaseObserver.MessageObserver { refresh() }

      ApplicationDependencies.getDatabaseObserver().registerMessageUpdateObserver(observer)
      emitter.setCancellable {
        ApplicationDependencies.getDatabaseObserver().unregisterObserver(observer)
      }

      refresh()
    }.subscribeOn(Schedulers.io())
  }
}
