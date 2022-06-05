package asia.coolapp.chat.stories.settings.custom.name

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.DistributionListId

class EditStoryNameRepository {
  fun save(privateStoryId: DistributionListId, name: CharSequence): Completable {
    return Completable.create {
      if (privateStoryId == DistributionListId.MY_STORY) {
        error("Cannot set name for My Story")
      }

      if (SignalDatabase.distributionLists.setName(privateStoryId, name.toString())) {
        it.onComplete()
      } else {
        it.onError(Exception("Could not update story name."))
      }
    }.subscribeOn(Schedulers.io())
  }
}
