package asia.coolapp.chat.components.settings.app.wrapped

import androidx.fragment.app.Fragment
import asia.coolapp.chat.preferences.StoragePreferenceFragment

class WrappedStoragePreferenceFragment : SettingsWrapperFragment() {
  override fun getFragment(): Fragment {
    return StoragePreferenceFragment()
  }
}
