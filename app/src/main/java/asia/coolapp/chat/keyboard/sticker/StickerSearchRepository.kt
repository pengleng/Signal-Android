package asia.coolapp.chat.keyboard.sticker

import android.content.Context
import androidx.annotation.WorkerThread
import asia.coolapp.chat.components.emoji.EmojiUtil
import asia.coolapp.chat.database.EmojiSearchDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.StickerDatabase
import asia.coolapp.chat.database.StickerDatabase.StickerRecordReader
import asia.coolapp.chat.database.model.StickerRecord

private const val RECENT_LIMIT = 24
private const val EMOJI_SEARCH_RESULTS_LIMIT = 20

class StickerSearchRepository(context: Context) {

  private val emojiSearchDatabase: EmojiSearchDatabase = SignalDatabase.emojiSearch
  private val stickerDatabase: StickerDatabase = SignalDatabase.stickers

  @WorkerThread
  fun search(query: String): List<StickerRecord> {
    if (query.isEmpty()) {
      return StickerRecordReader(stickerDatabase.getRecentlyUsedStickers(RECENT_LIMIT)).readAll()
    }

    val maybeEmojiQuery: List<StickerRecord> = findStickersForEmoji(query)
    val searchResults: List<StickerRecord> = emojiSearchDatabase.query(query, EMOJI_SEARCH_RESULTS_LIMIT)
      .map { findStickersForEmoji(it) }
      .flatten()

    return maybeEmojiQuery + searchResults
  }

  @WorkerThread
  private fun findStickersForEmoji(emoji: String): List<StickerRecord> {
    val searchEmoji: String = EmojiUtil.getCanonicalRepresentation(emoji)

    return EmojiUtil.getAllRepresentations(searchEmoji)
      .filterNotNull()
      .map { candidate -> StickerRecordReader(stickerDatabase.getStickersByEmoji(candidate)).readAll() }
      .flatten()
  }
}

private fun StickerRecordReader.readAll(): List<StickerRecord> {
  val stickers: MutableList<StickerRecord> = mutableListOf()
  use { reader ->
    var record: StickerRecord? = reader.next
    while (record != null) {
      stickers.add(record)
      record = reader.next
    }
  }
  return stickers
}
