package asia.coolapp.chat.keyboard.sticker

import android.net.Uri
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.R
import asia.coolapp.chat.database.StickerDatabase
import asia.coolapp.chat.database.StickerDatabase.StickerPackRecordReader
import asia.coolapp.chat.database.StickerDatabase.StickerRecordReader
import asia.coolapp.chat.database.model.StickerPackRecord
import asia.coolapp.chat.database.model.StickerRecord
import java.util.function.Consumer

private const val RECENT_LIMIT = 24
private const val RECENT_PACK_ID = "RECENT"

class StickerKeyboardRepository(private val stickerDatabase: StickerDatabase) {
  fun getStickerPacks(consumer: Consumer<List<KeyboardStickerPack>>) {
    SignalExecutors.BOUNDED.execute {
      val packs: MutableList<KeyboardStickerPack> = mutableListOf()

      StickerPackRecordReader(stickerDatabase.installedStickerPacks).use { reader ->
        var pack: StickerPackRecord? = reader.next
        while (pack != null) {
          packs += KeyboardStickerPack(packId = pack.packId, title = pack.title.orElse(null), coverUri = pack.cover.uri)
          pack = reader.next
        }
      }

      val fullPacks: MutableList<KeyboardStickerPack> = packs.map { p ->
        val stickers: MutableList<StickerRecord> = mutableListOf()

        StickerRecordReader(stickerDatabase.getStickersForPack(p.packId)).use { reader ->
          var sticker: StickerRecord? = reader.next
          while (sticker != null) {
            stickers.add(sticker)
            sticker = reader.next
          }
        }

        p.copy(stickers = stickers)
      }.toMutableList()

      val recentStickerPack: KeyboardStickerPack = getRecentStickerPack()
      if (recentStickerPack.stickers.isNotEmpty()) {
        fullPacks.add(0, recentStickerPack)
      }
      consumer.accept(fullPacks)
    }
  }

  private fun getRecentStickerPack(): KeyboardStickerPack {
    val recentStickers: MutableList<StickerRecord> = mutableListOf()

    StickerRecordReader(stickerDatabase.getRecentlyUsedStickers(RECENT_LIMIT)).use { reader ->
      var recentSticker: StickerRecord? = reader.next
      while (recentSticker != null) {
        recentStickers.add(recentSticker)
        recentSticker = reader.next
      }
    }

    return KeyboardStickerPack(
      packId = RECENT_PACK_ID,
      title = null,
      titleResource = R.string.StickerKeyboard__recently_used,
      coverUri = null,
      coverResource = R.drawable.ic_recent_20,
      stickers = recentStickers
    )
  }

  data class KeyboardStickerPack(
    val packId: String,
    val title: String?,
    val titleResource: Int? = null,
    val coverUri: Uri?,
    val coverResource: Int? = null,
    val stickers: List<StickerRecord> = emptyList()
  )
}
