package asia.coolapp.chat.keyboard.emoji

import asia.coolapp.chat.components.emoji.EmojiEventListener
import asia.coolapp.chat.keyboard.emoji.search.EmojiSearchFragment

interface EmojiKeyboardCallback :
  EmojiEventListener,
  EmojiKeyboardPageFragment.Callback,
  EmojiSearchFragment.Callback
