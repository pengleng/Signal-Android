package asia.coolapp.chat.stories.viewer.text

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.MmsMessageRecord

class StoryTextPostRepository {
  fun getRecord(recordId: Long): Single<MmsMessageRecord> {
    return Single.fromCallable {
      SignalDatabase.mms.getMessageRecord(recordId) as MmsMessageRecord
    }.subscribeOn(Schedulers.io())
  }
}
