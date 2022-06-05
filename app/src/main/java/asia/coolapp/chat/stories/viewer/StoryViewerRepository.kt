package asia.coolapp.chat.stories.viewer

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.DistributionListId
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId

class StoryViewerRepository {
  fun getStories(): Single<List<RecipientId>> {
    return Single.fromCallable {
      val recipients = SignalDatabase.mms.allStoriesRecipientsList
      val resolved = recipients.map { Recipient.resolved(it) }

      val doNotCollapse: List<RecipientId> = resolved
        .filterNot { it.isDistributionList || it.shouldHideStory() }
        .map { it.id }

      val myStory: RecipientId = SignalDatabase.recipients.getOrInsertFromDistributionListId(DistributionListId.MY_STORY)

      val myStoriesCount = SignalDatabase.mms.getAllOutgoingStories(true).use {
        var count = 0
        while (it.next != null) {
          if (!it.current.recipient.isGroup) {
            count++
          }
        }

        count
      }

      if (myStoriesCount > 0) {
        listOf(myStory) + doNotCollapse
      } else {
        doNotCollapse
      }
    }.subscribeOn(Schedulers.io())
  }
}
