package asia.coolapp.chat.stories.settings.story

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.DistributionListPartialRecord

class StorySettingsRepository {
  fun getPrivateStories(): Single<List<DistributionListPartialRecord>> {
    return Single.fromCallable {
      SignalDatabase.distributionLists.getCustomListsForUi()
    }.subscribeOn(Schedulers.io())
  }
}
