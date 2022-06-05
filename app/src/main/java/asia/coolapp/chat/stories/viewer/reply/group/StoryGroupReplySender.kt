package asia.coolapp.chat.stories.viewer.reply.group

import android.content.Context
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.Mention
import asia.coolapp.chat.database.model.ParentStoryId
import asia.coolapp.chat.database.model.StoryType
import asia.coolapp.chat.mms.OutgoingMediaMessage
import asia.coolapp.chat.sms.MessageSender

/**
 * Stateless message sender for Story Group replies and reactions.
 */
object StoryGroupReplySender {

  fun sendReply(context: Context, storyId: Long, body: CharSequence, mentions: List<Mention>): Completable {
    return sendInternal(context, storyId, body, mentions, false)
  }

  fun sendReaction(context: Context, storyId: Long, emoji: String): Completable {
    return sendInternal(context, storyId, emoji, emptyList(), true)
  }

  private fun sendInternal(context: Context, storyId: Long, body: CharSequence, mentions: List<Mention>, isReaction: Boolean): Completable {
    return Completable.create {

      val message = SignalDatabase.mms.getMessageRecord(storyId)
      val recipient = SignalDatabase.threads.getRecipientForThreadId(message.threadId)!!

      MessageSender.send(
        context,
        OutgoingMediaMessage(
          recipient,
          body.toString(),
          emptyList(),
          System.currentTimeMillis(),
          0,
          0L,
          false,
          0,
          StoryType.NONE,
          ParentStoryId.GroupReply(message.id),
          isReaction,
          null,
          emptyList(),
          emptyList(),
          mentions,
          emptySet(),
          emptySet()
        ),
        message.threadId,
        false,
        null
      ) {
        it.onComplete()
      }
    }.subscribeOn(Schedulers.io())
  }
}
