package asia.coolapp.chat.stories.viewer.page

import android.net.Uri
import asia.coolapp.chat.attachments.Attachment
import asia.coolapp.chat.conversation.ConversationMessage
import asia.coolapp.chat.database.AttachmentDatabase
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.util.MediaUtil

/**
 * Each story is made up of a collection of posts
 */
class StoryPost(
  val id: Long,
  val sender: Recipient,
  val group: Recipient?,
  val distributionList: Recipient?,
  val viewCount: Int,
  val replyCount: Int,
  val dateInMilliseconds: Long,
  val content: Content,
  val conversationMessage: ConversationMessage,
  val allowsReplies: Boolean
) {
  sealed class Content(val uri: Uri?) {
    class AttachmentContent(val attachment: Attachment) : Content(attachment.uri) {
      override val transferState: Int = attachment.transferState

      override fun isVideo(): Boolean = MediaUtil.isVideo(attachment)
    }
    class TextContent(uri: Uri, val recordId: Long, hasBody: Boolean) : Content(uri) {
      override val transferState: Int = if (hasBody) AttachmentDatabase.TRANSFER_PROGRESS_DONE else AttachmentDatabase.TRANSFER_PROGRESS_FAILED

      override fun isVideo(): Boolean = false
    }

    abstract val transferState: Int

    abstract fun isVideo(): Boolean
  }
}
