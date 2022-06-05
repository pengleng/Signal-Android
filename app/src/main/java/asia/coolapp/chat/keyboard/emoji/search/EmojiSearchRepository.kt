package asia.coolapp.chat.keyboard.emoji.search

import android.content.Context
import android.net.Uri
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.components.emoji.Emoji
import asia.coolapp.chat.components.emoji.EmojiPageModel
import asia.coolapp.chat.components.emoji.RecentEmojiPageModel
import asia.coolapp.chat.database.EmojiSearchDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.emoji.EmojiSource
import asia.coolapp.chat.util.TextSecurePreferences
import java.util.function.Consumer

private const val MINIMUM_QUERY_THRESHOLD = 1
private const val EMOJI_SEARCH_LIMIT = 20

class EmojiSearchRepository(private val context: Context) {

  private val emojiSearchDatabase: EmojiSearchDatabase = SignalDatabase.emojiSearch

  fun submitQuery(query: String, includeRecents: Boolean, limit: Int = EMOJI_SEARCH_LIMIT, consumer: Consumer<EmojiPageModel>) {
    if (query.length < MINIMUM_QUERY_THRESHOLD && includeRecents) {
      consumer.accept(RecentEmojiPageModel(context, TextSecurePreferences.RECENT_STORAGE_KEY))
    } else {
      SignalExecutors.SERIAL.execute {
        val emoji: List<String> = emojiSearchDatabase.query(query, limit)

        val displayEmoji: List<Emoji> = emoji
          .mapNotNull { canonical -> EmojiSource.latest.canonicalToVariations[canonical] }
          .map { Emoji(it) }

        consumer.accept(EmojiSearchResultsPageModel(emoji, displayEmoji))
      }
    }
  }

  private class EmojiSearchResultsPageModel(
    private val emoji: List<String>,
    private val displayEmoji: List<Emoji>
  ) : EmojiPageModel {
    override fun getKey(): String = ""

    override fun getIconAttr(): Int = -1

    override fun getEmoji(): List<String> = emoji

    override fun getDisplayEmoji(): List<Emoji> = displayEmoji

    override fun getSpriteUri(): Uri? = null

    override fun isDynamic(): Boolean = false
  }
}
