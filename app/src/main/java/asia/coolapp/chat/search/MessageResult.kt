package asia.coolapp.chat.search

import asia.coolapp.chat.recipients.Recipient

/**
 * Represents a search result for a message.
 */
data class MessageResult(
  val conversationRecipient: Recipient,
  val messageRecipient: Recipient,
  val body: String,
  val bodySnippet: String,
  val threadId: Long,
  val messageId: Long,
  val receivedTimestampMs: Long,
  val isMms: Boolean
)
