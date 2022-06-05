package asia.coolapp.chat.components.settings.app.subscription.receipts.detail

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.DonationReceiptRecord
import asia.coolapp.chat.dependencies.ApplicationDependencies
import java.util.Locale

class DonationReceiptDetailRepository {
  fun getSubscriptionLevelName(subscriptionLevel: Int): Single<String> {
    return ApplicationDependencies
      .getDonationsService()
      .getSubscriptionLevels(Locale.getDefault())
      .flatMap { it.flattenResult() }
      .map { it.levels[subscriptionLevel.toString()] ?: throw Exception("Subscription level $subscriptionLevel not found") }
      .map { it.name }
      .subscribeOn(Schedulers.io())
  }

  fun getDonationReceiptRecord(id: Long): Single<DonationReceiptRecord> {
    return Single.fromCallable<DonationReceiptRecord> {
      SignalDatabase.donationReceipts.getReceipt(id)
    }.subscribeOn(Schedulers.io())
  }
}
