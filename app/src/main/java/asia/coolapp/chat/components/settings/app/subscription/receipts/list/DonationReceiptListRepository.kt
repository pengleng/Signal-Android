package asia.coolapp.chat.components.settings.app.subscription.receipts.list

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.badges.Badges
import asia.coolapp.chat.database.model.DonationReceiptRecord
import asia.coolapp.chat.dependencies.ApplicationDependencies
import java.util.Locale

class DonationReceiptListRepository {
  fun getBadges(): Single<List<DonationReceiptBadge>> {
    val boostBadges: Single<List<DonationReceiptBadge>> = ApplicationDependencies.getDonationsService().getBoostBadge(Locale.getDefault())
      .map { response ->
        if (response.result.isPresent) {
          listOf(DonationReceiptBadge(DonationReceiptRecord.Type.BOOST, -1, Badges.fromServiceBadge(response.result.get())))
        } else {
          emptyList()
        }
      }

    val subBadges: Single<List<DonationReceiptBadge>> = ApplicationDependencies.getDonationsService().getSubscriptionLevels(Locale.getDefault())
      .map { response ->
        if (response.result.isPresent) {
          response.result.get().levels.map {
            DonationReceiptBadge(
              level = it.key.toInt(),
              badge = Badges.fromServiceBadge(it.value.badge),
              type = DonationReceiptRecord.Type.RECURRING
            )
          }
        } else {
          emptyList()
        }
      }

    return boostBadges.zipWith(subBadges) { a, b -> a + b }.subscribeOn(Schedulers.io())
  }
}
