package asia.coolapp.chat.components.emoji.parsing

import asia.coolapp.chat.emoji.EmojiPage

data class EmojiDrawInfo(val page: EmojiPage, val index: Int, private val emoji: String, val rawEmoji: String?, val jumboSheet: String?)
