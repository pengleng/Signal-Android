package asia.coolapp.chat.components.settings.app.wrapped

import androidx.fragment.app.Fragment
import asia.coolapp.chat.R
import asia.coolapp.chat.delete.DeleteAccountFragment

class WrappedDeleteAccountFragment : SettingsWrapperFragment() {
  override fun getFragment(): Fragment {
    toolbar.setTitle(R.string.preferences__delete_account)
    return DeleteAccountFragment()
  }
}
