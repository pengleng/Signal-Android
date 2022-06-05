package asia.coolapp.chat.stories.viewer.reply.direct

import android.content.Context
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.MediaMmsMessageRecord
import asia.coolapp.chat.database.model.MessageRecord
import asia.coolapp.chat.database.model.ParentStoryId
import asia.coolapp.chat.database.model.StoryType
import asia.coolapp.chat.mms.OutgoingMediaMessage
import asia.coolapp.chat.mms.QuoteModel
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.sms.MessageSender

class StoryDirectReplyRepository(context: Context) {

  private val context = context.applicationContext

  fun getStoryPost(storyId: Long): Single<MessageRecord> {
    return Single.fromCallable {
      SignalDatabase.mms.getMessageRecord(storyId)
    }.subscribeOn(Schedulers.io())
  }

  fun send(storyId: Long, groupDirectReplyRecipientId: RecipientId?, charSequence: CharSequence, isReaction: Boolean): Completable {
    return Completable.create { emitter ->
      val message = SignalDatabase.mms.getMessageRecord(storyId) as MediaMmsMessageRecord
      val (recipient, threadId) = if (groupDirectReplyRecipientId == null) {
        message.recipient to message.threadId
      } else {
        val resolved = Recipient.resolved(groupDirectReplyRecipientId)
        resolved to SignalDatabase.threads.getOrCreateThreadIdFor(resolved)
      }

      val quoteAuthor: Recipient = when {
        groupDirectReplyRecipientId != null -> message.recipient
        message.isOutgoing -> Recipient.self()
        else -> message.individualRecipient
      }

      MessageSender.send(
        context,
        OutgoingMediaMessage(
          recipient,
          charSequence.toString(),
          emptyList(),
          System.currentTimeMillis(),
          0,
          0L,
          false,
          0,
          StoryType.NONE,
          ParentStoryId.DirectReply(storyId),
          isReaction,
          QuoteModel(message.dateSent, quoteAuthor.id, message.body, false, message.slideDeck.asAttachments(), null),
          emptyList(),
          emptyList(),
          emptyList(),
          emptySet(),
          emptySet()
        ),
        threadId,
        false,
        null
      ) {
        emitter.onComplete()
      }
    }.subscribeOn(Schedulers.io())
  }
}
