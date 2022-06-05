package asia.coolapp.chat.database.model

import asia.coolapp.chat.recipients.RecipientId
import org.whispersystems.signalservice.api.crypto.ContentHint
import org.whispersystems.signalservice.internal.push.SignalServiceProtos

/**
 * Model class for reading from the [org.coolapp.chat.database.MessageSendLogDatabase].
 */
data class MessageLogEntry(
  val recipientId: RecipientId,
  val dateSent: Long,
  val content: SignalServiceProtos.Content,
  val contentHint: ContentHint,
  val relatedMessages: List<MessageId>
) {
  val hasRelatedMessage: Boolean
    @JvmName("hasRelatedMessage")
    get() = relatedMessages.isNotEmpty()
}
