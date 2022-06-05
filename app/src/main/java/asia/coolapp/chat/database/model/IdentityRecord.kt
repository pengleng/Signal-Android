package asia.coolapp.chat.database.model

import org.signal.libsignal.protocol.IdentityKey
import asia.coolapp.chat.database.IdentityDatabase
import asia.coolapp.chat.recipients.RecipientId

data class IdentityRecord(
  val recipientId: RecipientId,
  val identityKey: IdentityKey,
  val verifiedStatus: IdentityDatabase.VerifiedStatus,
  @get:JvmName("isFirstUse")
  val firstUse: Boolean,
  val timestamp: Long,
  @get:JvmName("isApprovedNonBlocking")
  val nonblockingApproval: Boolean
)
