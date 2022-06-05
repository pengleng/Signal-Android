package asia.coolapp.chat.components.settings.app.wrapped

import androidx.fragment.app.Fragment
import asia.coolapp.chat.R
import asia.coolapp.chat.preferences.MmsPreferencesFragment

class WrappedMmsPreferencesFragment : SettingsWrapperFragment() {
  override fun getFragment(): Fragment {
    toolbar.setTitle(R.string.preferences__advanced_mms_access_point_names)
    return MmsPreferencesFragment()
  }
}
