package asia.coolapp.chat.releasechannel

import asia.coolapp.chat.attachments.PointerAttachment
import asia.coolapp.chat.database.MessageDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.databaseprotos.BodyRangeList
import asia.coolapp.chat.mms.IncomingMediaMessage
import asia.coolapp.chat.recipients.RecipientId
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentRemoteId
import java.util.Optional
import java.util.UUID

/**
 * One stop shop for inserting Release Channel messages.
 */
object ReleaseChannel {

  const val CDN_NUMBER = -1

  fun insertAnnouncement(
    recipientId: RecipientId,
    body: String,
    threadId: Long,
    image: String? = null,
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    serverUuid: String? = UUID.randomUUID().toString(),
    messageRanges: BodyRangeList? = null
  ): MessageDatabase.InsertResult? {

    val attachments: Optional<List<SignalServiceAttachment>> = if (image != null) {
      val attachment = SignalServiceAttachmentPointer(
        CDN_NUMBER,
        SignalServiceAttachmentRemoteId.from(""),
        "image/webp",
        null,
        Optional.empty(),
        Optional.empty(),
        imageWidth,
        imageHeight,
        Optional.empty(),
        Optional.of(image),
        false,
        false,
        false,
        Optional.empty(),
        Optional.empty(),
        System.currentTimeMillis()
      )

      Optional.of(listOf(attachment))
    } else {
      Optional.empty()
    }

    val message = IncomingMediaMessage(
      from = recipientId,
      sentTimeMillis = System.currentTimeMillis(),
      serverTimeMillis = System.currentTimeMillis(),
      receivedTimeMillis = System.currentTimeMillis(),
      body = body,
      attachments = PointerAttachment.forPointers(attachments),
      serverGuid = serverUuid,
      messageRanges = messageRanges
    )

    return SignalDatabase.mms.insertSecureDecryptedMessageInbox(message, threadId).orElse(null)
  }
}
