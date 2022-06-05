package asia.coolapp.chat.database.model

import asia.coolapp.chat.recipients.RecipientId

/** A model for [org.coolapp.chat.database.PendingRetryReceiptDatabase] */
data class PendingRetryReceiptModel(
  val id: Long,
  val author: RecipientId,
  val authorDevice: Int,
  val sentTimestamp: Long,
  val receivedTimestamp: Long,
  val threadId: Long
)
