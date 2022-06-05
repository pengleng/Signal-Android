package asia.coolapp.chat.stories.landing

import android.content.Context
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.conversation.ConversationMessage
import asia.coolapp.chat.database.DatabaseObserver
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.DistributionListId
import asia.coolapp.chat.database.model.MessageRecord
import asia.coolapp.chat.database.model.StoryViewState
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientForeverObserver
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.sms.MessageSender

class StoriesLandingRepository(context: Context) {

  private val context = context.applicationContext

  fun resend(story: MessageRecord): Completable {
    return Completable.fromAction {
      MessageSender.resend(context, story)
    }.subscribeOn(Schedulers.io())
  }

  fun getStories(): Observable<List<StoriesLandingItemData>> {
    return Observable.create<Observable<List<StoriesLandingItemData>>> { emitter ->
      val myStoriesId = SignalDatabase.recipients.getOrInsertFromDistributionListId(DistributionListId.MY_STORY)
      val myStories = Recipient.resolved(myStoriesId)

      fun refresh() {
        val storyMap = mutableMapOf<Recipient, List<MessageRecord>>()
        SignalDatabase.mms.allStories.use {
          while (it.next != null) {
            val messageRecord = it.current
            val recipient = if (messageRecord.isOutgoing && !messageRecord.recipient.isGroup) {
              myStories
            } else if (messageRecord.isOutgoing && messageRecord.recipient.isGroup) {
              messageRecord.recipient
            } else {
              SignalDatabase.threads.getRecipientForThreadId(messageRecord.threadId)!!
            }

            storyMap[recipient] = (storyMap[recipient] ?: emptyList()) + messageRecord
          }
        }

        val data: List<Observable<StoriesLandingItemData>> = storyMap.map { (sender, records) -> createStoriesLandingItemData(sender, records) }
        if (data.isEmpty()) {
          emitter.onNext(Observable.just(emptyList()))
        } else {
          emitter.onNext(Observable.combineLatest(data) { it.toList() as List<StoriesLandingItemData> })
        }
      }

      val observer = DatabaseObserver.Observer {
        refresh()
      }

      ApplicationDependencies.getDatabaseObserver().registerConversationListObserver(observer)
      emitter.setCancellable {
        ApplicationDependencies.getDatabaseObserver().unregisterObserver(observer)
      }

      refresh()
    }.switchMap { it }.subscribeOn(Schedulers.io())
  }

  private fun createStoriesLandingItemData(sender: Recipient, messageRecords: List<MessageRecord>): Observable<StoriesLandingItemData> {
    val itemDataObservable = Observable.create<StoriesLandingItemData> { emitter ->
      fun refresh(sender: Recipient) {
        val itemData = StoriesLandingItemData(
          storyRecipient = sender,
          storyViewState = StoryViewState.NONE,
          hasReplies = messageRecords.any { SignalDatabase.mms.getNumberOfStoryReplies(it.id) > 0 },
          hasRepliesFromSelf = messageRecords.any { SignalDatabase.mms.hasSelfReplyInStory(it.id) },
          isHidden = sender.shouldHideStory(),
          primaryStory = ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(context, messageRecords.first()),
          secondaryStory = if (sender.isMyStory) messageRecords.drop(1).firstOrNull()?.let {
            ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(context, it)
          } else null
        )

        emitter.onNext(itemData)
      }

      val newRepliesObserver = DatabaseObserver.Observer {
        Recipient.live(sender.id).refresh()
      }

      val recipientChangedObserver = RecipientForeverObserver {
        refresh(it)
      }

      ApplicationDependencies.getDatabaseObserver().registerConversationObserver(messageRecords.first().threadId, newRepliesObserver)
      val liveRecipient = Recipient.live(sender.id)
      liveRecipient.observeForever(recipientChangedObserver)

      emitter.setCancellable {
        ApplicationDependencies.getDatabaseObserver().unregisterObserver(newRepliesObserver)
        liveRecipient.removeForeverObserver(recipientChangedObserver)
      }

      refresh(sender)
    }

    val storyViewedStateObservable = StoryViewState.getForRecipientId(if (sender.isMyStory) Recipient.self().id else sender.id)

    return Observable.combineLatest(itemDataObservable, storyViewedStateObservable) { data, state ->
      data.copy(storyViewState = state)
    }
  }

  fun setHideStory(recipientId: RecipientId, hideStory: Boolean): Completable {
    return Completable.fromAction {
      SignalDatabase.recipients.setHideStory(recipientId, hideStory)
    }.subscribeOn(Schedulers.io())
  }
}
