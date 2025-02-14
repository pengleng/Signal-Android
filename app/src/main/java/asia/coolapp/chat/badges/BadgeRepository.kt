package asia.coolapp.chat.badges

import android.content.Context
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.database.RecipientDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.storage.StorageSyncHelper
import asia.coolapp.chat.util.ProfileUtil

class BadgeRepository(context: Context) {

  private val context = context.applicationContext

  fun setVisibilityForAllBadges(
    displayBadgesOnProfile: Boolean,
    selfBadges: List<Badge> = Recipient.self().badges
  ): Completable = Completable.fromAction {
    val recipientDatabase: RecipientDatabase = SignalDatabase.recipients
    val badges = selfBadges.map { it.copy(visible = displayBadgesOnProfile) }

    ProfileUtil.uploadProfileWithBadges(context, badges)
    SignalStore.donationsValues().setDisplayBadgesOnProfile(displayBadgesOnProfile)
    recipientDatabase.markNeedsSync(Recipient.self().id)
    StorageSyncHelper.scheduleSyncForDataChange()

    recipientDatabase.setBadges(Recipient.self().id, badges)
  }.subscribeOn(Schedulers.io())

  fun setFeaturedBadge(featuredBadge: Badge): Completable = Completable.fromAction {
    val badges = Recipient.self().badges
    val reOrderedBadges = listOf(featuredBadge.copy(visible = true)) + (badges.filterNot { it.id == featuredBadge.id })
    ProfileUtil.uploadProfileWithBadges(context, reOrderedBadges)

    val recipientDatabase: RecipientDatabase = SignalDatabase.recipients
    recipientDatabase.setBadges(Recipient.self().id, reOrderedBadges)
  }.subscribeOn(Schedulers.io())
}
