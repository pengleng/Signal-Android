package asia.coolapp.chat.components.settings.app.wrapped

import androidx.fragment.app.Fragment
import asia.coolapp.chat.R
import asia.coolapp.chat.help.HelpFragment

class WrappedHelpFragment : SettingsWrapperFragment() {
  override fun getFragment(): Fragment {
    toolbar.title = getString(R.string.preferences__help)

    val fragment = HelpFragment()
    fragment.arguments = arguments

    return fragment
  }
}
