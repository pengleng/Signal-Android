package asia.coolapp.chat.components.settings.app.subscription.receipts.list

import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.database.model.DonationReceiptRecord

data class DonationReceiptBadge(
  val type: DonationReceiptRecord.Type,
  val level: Int,
  val badge: Badge
)
