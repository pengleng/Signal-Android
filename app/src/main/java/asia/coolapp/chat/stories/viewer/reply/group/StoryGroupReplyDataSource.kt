package asia.coolapp.chat.stories.viewer.reply.group

import org.signal.paging.PagedDataSource
import asia.coolapp.chat.conversation.ConversationMessage
import asia.coolapp.chat.database.MmsDatabase
import asia.coolapp.chat.database.MmsSmsColumns
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.MmsMessageRecord
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.recipients.Recipient

class StoryGroupReplyDataSource(private val parentStoryId: Long) : PagedDataSource<StoryGroupReplyItemData.Key, StoryGroupReplyItemData> {
  override fun size(): Int {
    return SignalDatabase.mms.getNumberOfStoryReplies(parentStoryId)
  }

  override fun load(start: Int, length: Int, cancellationSignal: PagedDataSource.CancellationSignal): MutableList<StoryGroupReplyItemData> {
    val results: MutableList<StoryGroupReplyItemData> = ArrayList(length)
    SignalDatabase.mms.getStoryReplies(parentStoryId).use { cursor ->
      cursor.moveToPosition(start - 1)
      val reader = MmsDatabase.Reader(cursor)
      while (cursor.moveToNext() && cursor.position < start + length) {
        results.add(readRowFromRecord(reader.current as MmsMessageRecord))
      }
    }

    return results
  }

  override fun load(key: StoryGroupReplyItemData.Key?): StoryGroupReplyItemData? {
    throw UnsupportedOperationException()
  }

  override fun getKey(data: StoryGroupReplyItemData): StoryGroupReplyItemData.Key {
    return data.key
  }

  private fun readRowFromRecord(record: MmsMessageRecord): StoryGroupReplyItemData {
    return if (MmsSmsColumns.Types.isStoryReaction(record.type)) {
      readReactionFromRecord(record)
    } else {
      readTextFromRecord(record)
    }
  }

  private fun readReactionFromRecord(record: MmsMessageRecord): StoryGroupReplyItemData {
    return StoryGroupReplyItemData(
      key = StoryGroupReplyItemData.Key.Reaction(record.id),
      sender = if (record.isOutgoing) Recipient.self() else record.individualRecipient,
      sentAtMillis = record.dateSent,
      replyBody = StoryGroupReplyItemData.ReplyBody.Reaction(record.body)
    )
  }

  private fun readTextFromRecord(record: MmsMessageRecord): StoryGroupReplyItemData {
    return StoryGroupReplyItemData(
      key = StoryGroupReplyItemData.Key.Text(record.id),
      sender = if (record.isOutgoing) Recipient.self() else record.individualRecipient,
      sentAtMillis = record.dateSent,
      replyBody = StoryGroupReplyItemData.ReplyBody.Text(
        ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(ApplicationDependencies.getApplication(), record)
      )
    )
  }
}
