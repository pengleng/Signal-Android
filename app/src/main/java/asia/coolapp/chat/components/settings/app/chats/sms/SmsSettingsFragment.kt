package asia.coolapp.chat.components.settings.app.chats.sms

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsFragment
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.util.SmsUtil
import asia.coolapp.chat.util.Util
import asia.coolapp.chat.util.navigation.safeNavigate

private const val SMS_REQUEST_CODE: Short = 1234

class SmsSettingsFragment : DSLSettingsFragment(R.string.preferences__sms_mms) {

  private lateinit var viewModel: SmsSettingsViewModel

  override fun onResume() {
    super.onResume()
    viewModel.checkSmsEnabled()
  }

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    viewModel = ViewModelProvider(this)[SmsSettingsViewModel::class.java]

    viewModel.state.observe(viewLifecycleOwner) {
      adapter.submitList(getConfiguration(it).toMappingModelList())
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    SignalStore.settings().setDefaultSms(Util.isDefaultSmsProvider(requireContext()))
  }

  private fun getConfiguration(state: SmsSettingsState): DSLConfiguration {
    return configure {
      @Suppress("DEPRECATION")
      clickPref(
        title = DSLSettingsText.from(R.string.SmsSettingsFragment__use_as_default_sms_app),
        summary = DSLSettingsText.from(if (state.useAsDefaultSmsApp) R.string.arrays__enabled else R.string.arrays__disabled),
        onClick = {
          if (state.useAsDefaultSmsApp) {
            startDefaultAppSelectionIntent()
          } else {
            startActivityForResult(SmsUtil.getSmsRoleIntent(requireContext()), SMS_REQUEST_CODE.toInt())
          }
        }
      )

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__sms_delivery_reports),
        summary = DSLSettingsText.from(R.string.preferences__request_a_delivery_report_for_each_sms_message_you_send),
        isChecked = state.smsDeliveryReportsEnabled,
        onClick = {
          viewModel.setSmsDeliveryReportsEnabled(!state.smsDeliveryReportsEnabled)
        }
      )

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__support_wifi_calling),
        summary = DSLSettingsText.from(R.string.preferences__enable_if_your_device_supports_sms_mms_delivery_over_wifi),
        isChecked = state.wifiCallingCompatibilityEnabled,
        onClick = {
          viewModel.setWifiCallingCompatibilityEnabled(!state.wifiCallingCompatibilityEnabled)
        }
      )

      if (Build.VERSION.SDK_INT < 21) {
        clickPref(
          title = DSLSettingsText.from(R.string.preferences__advanced_mms_access_point_names),
          onClick = {
            Navigation.findNavController(requireView()).safeNavigate(R.id.action_smsSettingsFragment_to_mmsPreferencesFragment)
          }
        )
      }
    }
  }

  // Linter isn't smart enough to figure out the else only happens if API >= 24
  @SuppressLint("InlinedApi")
  @Suppress("DEPRECATION")
  private fun startDefaultAppSelectionIntent() {
    val intent: Intent = when {
      Build.VERSION.SDK_INT < 23 -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
      Build.VERSION.SDK_INT < 24 -> Intent(Settings.ACTION_SETTINGS)
      else -> Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    }

    startActivityForResult(intent, SMS_REQUEST_CODE.toInt())
  }
}
