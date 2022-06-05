package asia.coolapp.chat.components.settings.app.data

import asia.coolapp.chat.mms.SentMediaQuality
import asia.coolapp.chat.webrtc.CallBandwidthMode

data class DataAndStorageSettingsState(
  val totalStorageUse: Long,
  val mobileAutoDownloadValues: Set<String>,
  val wifiAutoDownloadValues: Set<String>,
  val roamingAutoDownloadValues: Set<String>,
  val callBandwidthMode: CallBandwidthMode,
  val isProxyEnabled: Boolean,
  val sentMediaQuality: SentMediaQuality
)
