package asia.coolapp.chat.contacts.selection

import android.os.Bundle
import asia.coolapp.chat.contacts.ContactsCursorLoader
import asia.coolapp.chat.groups.SelectionLimits
import asia.coolapp.chat.recipients.RecipientId

data class ContactSelectionArguments(
  val displayMode: Int = ContactsCursorLoader.DisplayMode.FLAG_ALL,
  val isRefreshable: Boolean = true,
  val displayRecents: Boolean = false,
  val selectionLimits: SelectionLimits? = null,
  val currentSelection: List<RecipientId> = emptyList(),
  val displaySelectionCount: Boolean = true,
  val canSelectSelf: Boolean = selectionLimits == null,
  val displayChips: Boolean = true,
  val recyclerPadBottom: Int = -1,
  val recyclerChildClipping: Boolean = true
) {

  fun toArgumentBundle(): Bundle {
    return Bundle().apply {
      putInt(DISPLAY_MODE, displayMode)
      putBoolean(REFRESHABLE, isRefreshable)
      putBoolean(RECENTS, displayRecents)
      putParcelable(SELECTION_LIMITS, selectionLimits)
      putBoolean(HIDE_COUNT, !displaySelectionCount)
      putBoolean(CAN_SELECT_SELF, canSelectSelf)
      putBoolean(DISPLAY_CHIPS, displayChips)
      putInt(RV_PADDING_BOTTOM, recyclerPadBottom)
      putBoolean(RV_CLIP, recyclerChildClipping)
      putParcelableArrayList(CURRENT_SELECTION, ArrayList(currentSelection))
    }
  }

  companion object {
    const val DISPLAY_MODE = "display_mode"
    const val REFRESHABLE = "refreshable"
    const val RECENTS = "recents"
    const val SELECTION_LIMITS = "selection_limits"
    const val CURRENT_SELECTION = "current_selection"
    const val HIDE_COUNT = "hide_count"
    const val CAN_SELECT_SELF = "can_select_self"
    const val DISPLAY_CHIPS = "display_chips"
    const val RV_PADDING_BOTTOM = "recycler_view_padding_bottom"
    const val RV_CLIP = "recycler_view_clipping"
  }
}
