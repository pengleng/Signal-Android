package asia.coolapp.chat.components.settings.app.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.util.livedata.Store

class AccountSettingsViewModel : ViewModel() {
  private val store: Store<AccountSettingsState> = Store(getCurrentState())

  val state: LiveData<AccountSettingsState> = store.stateLiveData

  fun refreshState() {
    store.update { getCurrentState() }
  }

  private fun getCurrentState(): AccountSettingsState {
    return AccountSettingsState(
      hasPin = SignalStore.kbsValues().hasPin() && !SignalStore.kbsValues().hasOptedOut(),
      pinRemindersEnabled = SignalStore.pinValues().arePinRemindersEnabled(),
      registrationLockEnabled = SignalStore.kbsValues().isV2RegistrationLockEnabled
    )
  }
}
