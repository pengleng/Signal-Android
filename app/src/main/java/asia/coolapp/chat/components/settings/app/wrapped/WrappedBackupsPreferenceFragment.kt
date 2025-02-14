package asia.coolapp.chat.components.settings.app.wrapped

import androidx.fragment.app.Fragment
import asia.coolapp.chat.R
import asia.coolapp.chat.preferences.BackupsPreferenceFragment

class WrappedBackupsPreferenceFragment : SettingsWrapperFragment() {
  override fun getFragment(): Fragment {
    toolbar.setTitle(R.string.BackupsPreferenceFragment__chat_backups)
    return BackupsPreferenceFragment()
  }
}
