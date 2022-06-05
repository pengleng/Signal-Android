package asia.coolapp.chat.keyboard.emoji

import android.content.Context
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.components.emoji.EmojiPageModel
import asia.coolapp.chat.components.emoji.RecentEmojiPageModel
import asia.coolapp.chat.emoji.EmojiSource.Companion.latest
import asia.coolapp.chat.util.TextSecurePreferences
import java.util.function.Consumer

class EmojiKeyboardPageRepository(private val context: Context) {
  fun getEmoji(consumer: Consumer<List<EmojiPageModel>>) {
    SignalExecutors.BOUNDED.execute {
      val list = mutableListOf<EmojiPageModel>()
      list += RecentEmojiPageModel(context, TextSecurePreferences.RECENT_STORAGE_KEY)
      list += latest.displayPages
      consumer.accept(list)
    }
  }
}
