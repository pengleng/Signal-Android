package asia.coolapp.chat.database.model

import asia.coolapp.chat.recipients.RecipientId
import org.whispersystems.signalservice.api.push.DistributionId

/**
 * Represents an entry in the [org.coolapp.chat.database.DistributionListDatabase].
 */
data class DistributionListRecord(
  val id: DistributionListId,
  val name: String,
  val distributionId: DistributionId,
  val allowsReplies: Boolean,
  val members: List<RecipientId>
)
