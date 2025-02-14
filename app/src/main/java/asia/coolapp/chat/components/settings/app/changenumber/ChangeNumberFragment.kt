package asia.coolapp.chat.components.settings.app.changenumber

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import asia.coolapp.chat.LoggingFragment
import asia.coolapp.chat.R
import asia.coolapp.chat.util.navigation.safeNavigate

class ChangeNumberFragment : LoggingFragment(R.layout.fragment_change_phone_number) {
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

    view.findViewById<View>(R.id.change_phone_number_continue).setOnClickListener {
      findNavController().safeNavigate(R.id.action_changePhoneNumberFragment_to_enterPhoneNumberChangeFragment)
    }
  }
}
