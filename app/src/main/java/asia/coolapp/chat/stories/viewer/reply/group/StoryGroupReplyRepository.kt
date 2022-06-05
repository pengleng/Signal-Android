package asia.coolapp.chat.stories.viewer.reply.group

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.paging.LivePagedData
import org.signal.paging.PagedData
import org.signal.paging.PagingConfig
import asia.coolapp.chat.database.DatabaseObserver
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.recipients.RecipientId

class StoryGroupReplyRepository {

  fun getPagedReplies(parentStoryId: Long): Observable<LivePagedData<StoryGroupReplyItemData.Key, StoryGroupReplyItemData>> {
    return Observable.create<LivePagedData<StoryGroupReplyItemData.Key, StoryGroupReplyItemData>> { emitter ->
      fun refresh() {
        emitter.onNext(PagedData.createForLiveData(StoryGroupReplyDataSource(parentStoryId), PagingConfig.Builder().build()))
      }

      val observer = DatabaseObserver.Observer {
        refresh()
      }

      val messageObserver = DatabaseObserver.MessageObserver {
        refresh()
      }

      val threadId = SignalDatabase.mms.getThreadIdForMessage(parentStoryId)

      ApplicationDependencies.getDatabaseObserver().registerMessageInsertObserver(threadId, messageObserver)
      ApplicationDependencies.getDatabaseObserver().registerConversationObserver(threadId, observer)

      emitter.setCancellable {
        ApplicationDependencies.getDatabaseObserver().unregisterObserver(observer)
        ApplicationDependencies.getDatabaseObserver().unregisterObserver(messageObserver)
      }

      refresh()
    }.subscribeOn(Schedulers.io())
  }

  fun getStoryOwner(storyId: Long): Single<RecipientId> {
    return Single.fromCallable {
      SignalDatabase.mms.getMessageRecord(storyId).individualRecipient.id
    }.subscribeOn(Schedulers.io())
  }
}
