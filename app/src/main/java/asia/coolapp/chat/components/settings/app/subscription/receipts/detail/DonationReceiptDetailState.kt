package asia.coolapp.chat.components.settings.app.subscription.receipts.detail

import asia.coolapp.chat.database.model.DonationReceiptRecord

data class DonationReceiptDetailState(
  val donationReceiptRecord: DonationReceiptRecord? = null,
  val subscriptionName: String? = null
)
