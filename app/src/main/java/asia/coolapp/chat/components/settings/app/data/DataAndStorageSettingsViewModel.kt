package asia.coolapp.chat.components.settings.app.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.mms.SentMediaQuality
import asia.coolapp.chat.util.TextSecurePreferences
import asia.coolapp.chat.util.livedata.Store
import asia.coolapp.chat.webrtc.CallBandwidthMode

class DataAndStorageSettingsViewModel(
  private val sharedPreferences: SharedPreferences,
  private val repository: DataAndStorageSettingsRepository
) : ViewModel() {

  private val store = Store(getState())

  val state: LiveData<DataAndStorageSettingsState> = store.stateLiveData

  fun refresh() {
    repository.getTotalStorageUse { totalStorageUse ->
      store.update { getState().copy(totalStorageUse = totalStorageUse) }
    }
  }

  fun setMobileAutoDownloadValues(resultSet: Set<String>) {
    sharedPreferences.edit().putStringSet(TextSecurePreferences.MEDIA_DOWNLOAD_MOBILE_PREF, resultSet).apply()
    getStateAndCopyStorageUsage()
  }

  fun setWifiAutoDownloadValues(resultSet: Set<String>) {
    sharedPreferences.edit().putStringSet(TextSecurePreferences.MEDIA_DOWNLOAD_WIFI_PREF, resultSet).apply()
    getStateAndCopyStorageUsage()
  }

  fun setRoamingAutoDownloadValues(resultSet: Set<String>) {
    sharedPreferences.edit().putStringSet(TextSecurePreferences.MEDIA_DOWNLOAD_ROAMING_PREF, resultSet).apply()
    getStateAndCopyStorageUsage()
  }

  fun setCallBandwidthMode(callBandwidthMode: CallBandwidthMode) {
    SignalStore.settings().callBandwidthMode = callBandwidthMode
    ApplicationDependencies.getSignalCallManager().bandwidthModeUpdate()
    getStateAndCopyStorageUsage()
  }

  fun setSentMediaQuality(sentMediaQuality: SentMediaQuality) {
    SignalStore.settings().sentMediaQuality = sentMediaQuality
    getStateAndCopyStorageUsage()
  }

  private fun getStateAndCopyStorageUsage() {
    store.update { getState().copy(totalStorageUse = it.totalStorageUse) }
  }

  private fun getState() = DataAndStorageSettingsState(
    totalStorageUse = 0,
    mobileAutoDownloadValues = TextSecurePreferences.getMobileMediaDownloadAllowed(
      ApplicationDependencies.getApplication()
    ),
    wifiAutoDownloadValues = TextSecurePreferences.getWifiMediaDownloadAllowed(
      ApplicationDependencies.getApplication()
    ),
    roamingAutoDownloadValues = TextSecurePreferences.getRoamingMediaDownloadAllowed(
      ApplicationDependencies.getApplication()
    ),
    callBandwidthMode = SignalStore.settings().callBandwidthMode,
    isProxyEnabled = SignalStore.proxy().isProxyEnabled,
    sentMediaQuality = SignalStore.settings().sentMediaQuality
  )

  class Factory(
    private val sharedPreferences: SharedPreferences,
    private val repository: DataAndStorageSettingsRepository
  ) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(DataAndStorageSettingsViewModel(sharedPreferences, repository)))
    }
  }
}
