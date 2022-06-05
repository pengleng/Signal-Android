package asia.coolapp.chat.components.settings.app.wrapped

import androidx.fragment.app.Fragment
import asia.coolapp.chat.R
import asia.coolapp.chat.preferences.AdvancedPinPreferenceFragment

class WrappedAdvancedPinPreferenceFragment : SettingsWrapperFragment() {
  override fun getFragment(): Fragment {
    toolbar.setTitle(R.string.preferences__advanced_pin_settings)
    return AdvancedPinPreferenceFragment()
  }
}
