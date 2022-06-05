package asia.coolapp.chat.badges.self.expired

import androidx.core.content.ContextCompat
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsBottomSheetFragment
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.components.settings.models.SplashImage
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.util.CommunicationActions

class CantProcessSubscriptionPaymentBottomSheetDialogFragment : DSLSettingsBottomSheetFragment() {
  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    SplashImage.register(adapter)
    adapter.submitList(getConfiguration().toMappingModelList())
  }

  private fun getConfiguration(): DSLConfiguration {
    return configure {
      customPref(SplashImage.Model(R.drawable.ic_card_process))

      sectionHeaderPref(
        title = DSLSettingsText.from(R.string.CantProcessSubscriptionPaymentBottomSheetDialogFragment__cant_process_subscription_payment, DSLSettingsText.CenterModifier)
      )

      textPref(
        summary = DSLSettingsText.from(
          requireContext().getString(R.string.CantProcessSubscriptionPaymentBottomSheetDialogFragment__were_having_trouble),
          DSLSettingsText.LearnMoreModifier(ContextCompat.getColor(requireContext(), R.color.signal_accent_primary)) {
            CommunicationActions.openBrowserLink(requireContext(), requireContext().getString(R.string.donation_decline_code_error_url))
          },
          DSLSettingsText.CenterModifier
        )
      )

      primaryButton(
        text = DSLSettingsText.from(android.R.string.ok)
      ) {
        dismissAllowingStateLoss()
      }

      secondaryButtonNoOutline(
        text = DSLSettingsText.from(R.string.CantProcessSubscriptionPaymentBottomSheetDialogFragment__dont_show_this_again)
      ) {
        SignalStore.donationsValues().showCantProcessDialog = false
      }
    }
  }
}
