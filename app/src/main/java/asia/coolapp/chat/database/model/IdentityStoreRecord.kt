package asia.coolapp.chat.database.model

import org.signal.libsignal.protocol.IdentityKey
import asia.coolapp.chat.database.IdentityDatabase
import asia.coolapp.chat.recipients.RecipientId

data class IdentityStoreRecord(
  val addressName: String,
  val identityKey: IdentityKey,
  val verifiedStatus: IdentityDatabase.VerifiedStatus,
  val firstUse: Boolean,
  val timestamp: Long,
  val nonblockingApproval: Boolean
) {
  fun toIdentityRecord(recipientId: RecipientId): IdentityRecord {
    return IdentityRecord(
      recipientId = recipientId,
      identityKey = identityKey,
      verifiedStatus = verifiedStatus,
      firstUse = firstUse,
      timestamp = timestamp,
      nonblockingApproval = nonblockingApproval
    )
  }
}
