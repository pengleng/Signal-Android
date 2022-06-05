package asia.coolapp.chat.conversation.mutiselect

import android.view.View
import asia.coolapp.chat.conversation.ConversationMessage
import asia.coolapp.chat.conversation.colors.Colorizable
import asia.coolapp.chat.giph.mp4.GiphyMp4Playable

interface Multiselectable : Colorizable, GiphyMp4Playable {
  val conversationMessage: ConversationMessage

  fun getTopBoundaryOfMultiselectPart(multiselectPart: MultiselectPart): Int

  fun getBottomBoundaryOfMultiselectPart(multiselectPart: MultiselectPart): Int

  fun getMultiselectPartForLatestTouch(): MultiselectPart

  fun getHorizontalTranslationTarget(): View?

  fun hasNonSelectableMedia(): Boolean
}
