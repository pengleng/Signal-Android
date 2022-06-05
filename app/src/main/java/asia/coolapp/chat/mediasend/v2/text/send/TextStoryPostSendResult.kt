package asia.coolapp.chat.mediasend.v2.text.send

import asia.coolapp.chat.database.model.IdentityRecord

sealed class TextStoryPostSendResult {
  object Success : TextStoryPostSendResult()
  object Failure : TextStoryPostSendResult()
  data class UntrustedRecordsError(val untrustedRecords: List<IdentityRecord>) : TextStoryPostSendResult()
}
