package asia.coolapp.chat.keyboard.emoji

import asia.coolapp.chat.components.emoji.EmojiPageModel
import asia.coolapp.chat.components.emoji.EmojiPageViewGridAdapter
import asia.coolapp.chat.components.emoji.RecentEmojiPageModel
import asia.coolapp.chat.components.emoji.parsing.EmojiTree
import asia.coolapp.chat.emoji.EmojiCategory
import asia.coolapp.chat.emoji.EmojiSource
import asia.coolapp.chat.util.adapter.mapping.MappingModel

fun EmojiPageModel.toMappingModels(): List<MappingModel<*>> {
  val emojiTree: EmojiTree = EmojiSource.latest.emojiTree

  return displayEmoji.map {
    val isTextEmoji = EmojiCategory.EMOTICONS.key == key || (RecentEmojiPageModel.KEY == key && emojiTree.getEmoji(it.value, 0, it.value.length) == null)

    if (isTextEmoji) {
      EmojiPageViewGridAdapter.EmojiTextModel(key, it)
    } else {
      EmojiPageViewGridAdapter.EmojiModel(key, it)
    }
  }
}
