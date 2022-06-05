package asia.coolapp.chat.stories.settings.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import asia.coolapp.chat.R
import asia.coolapp.chat.components.FixedRoundedCornerBottomSheetDialogFragment

class SignalConnectionsBottomSheetDialogFragment : FixedRoundedCornerBottomSheetDialogFragment() {

  override val peekHeightPercentage: Float = 1f

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.stories_signal_connection_bottom_sheet, container, false)
  }
}
