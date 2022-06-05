package asia.coolapp.chat.sms

import asia.coolapp.chat.mms.OutgoingSecureMediaMessage

class OutgoingStoryMessage(
  val outgoingSecureMediaMessage: OutgoingSecureMediaMessage,
  val preUploadResult: MessageSender.PreUploadResult
)
