package asia.coolapp.chat.stories.tabs

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.DatabaseObserver
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.dependencies.ApplicationDependencies

class ConversationListTabRepository {

  fun getNumberOfUnreadConversations(): Observable<Long> {
    return Observable.create<Long> {
      val listener = DatabaseObserver.Observer {
        it.onNext(SignalDatabase.threads.tabBarUnreadCount)
      }

      ApplicationDependencies.getDatabaseObserver().registerConversationListObserver(listener)
      it.setCancellable { ApplicationDependencies.getDatabaseObserver().unregisterObserver(listener) }
      it.onNext(SignalDatabase.threads.tabBarUnreadCount)
    }.subscribeOn(Schedulers.io())
  }

  fun getNumberOfUnseenStories(): Observable<Long> {
    return Observable.create<Long> {
      val listener = DatabaseObserver.Observer {
        it.onNext(SignalDatabase.mms.unreadStoryCount)
      }

      ApplicationDependencies.getDatabaseObserver().registerConversationListObserver(listener)
      it.setCancellable { ApplicationDependencies.getDatabaseObserver().unregisterObserver(listener) }
      it.onNext(SignalDatabase.mms.unreadStoryCount)
    }.subscribeOn(Schedulers.io())
  }
}
