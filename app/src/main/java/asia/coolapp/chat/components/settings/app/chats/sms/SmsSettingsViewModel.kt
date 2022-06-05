package asia.coolapp.chat.components.settings.app.chats.sms

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.util.Util
import asia.coolapp.chat.util.livedata.Store

class SmsSettingsViewModel : ViewModel() {

  private val store = Store(
    SmsSettingsState(
      useAsDefaultSmsApp = Util.isDefaultSmsProvider(ApplicationDependencies.getApplication()),
      smsDeliveryReportsEnabled = SignalStore.settings().isSmsDeliveryReportsEnabled,
      wifiCallingCompatibilityEnabled = SignalStore.settings().isWifiCallingCompatibilityModeEnabled
    )
  )

  val state: LiveData<SmsSettingsState> = store.stateLiveData

  fun setSmsDeliveryReportsEnabled(enabled: Boolean) {
    store.update { it.copy(smsDeliveryReportsEnabled = enabled) }
    SignalStore.settings().isSmsDeliveryReportsEnabled = enabled
  }

  fun setWifiCallingCompatibilityEnabled(enabled: Boolean) {
    store.update { it.copy(wifiCallingCompatibilityEnabled = enabled) }
    SignalStore.settings().isWifiCallingCompatibilityModeEnabled = enabled
  }

  fun checkSmsEnabled() {
    store.update { it.copy(useAsDefaultSmsApp = Util.isDefaultSmsProvider(ApplicationDependencies.getApplication())) }
  }
}
