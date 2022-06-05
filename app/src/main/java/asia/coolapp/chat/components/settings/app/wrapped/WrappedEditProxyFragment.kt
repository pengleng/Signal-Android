package asia.coolapp.chat.components.settings.app.wrapped

import androidx.fragment.app.Fragment
import asia.coolapp.chat.R
import asia.coolapp.chat.preferences.EditProxyFragment

class WrappedEditProxyFragment : SettingsWrapperFragment() {
  override fun getFragment(): Fragment {
    toolbar.setTitle(R.string.preferences_use_proxy)
    return EditProxyFragment()
  }
}
