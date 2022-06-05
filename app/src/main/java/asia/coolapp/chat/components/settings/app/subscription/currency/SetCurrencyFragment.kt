package asia.coolapp.chat.components.settings.app.subscription.currency

import androidx.fragment.app.viewModels
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsBottomSheetFragment
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.app.subscription.DonationPaymentComponent
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.util.fragments.requireListener
import java.util.Locale

/**
 * Simple fragment for selecting a currency for Donations
 */
class SetCurrencyFragment : DSLSettingsBottomSheetFragment() {

  private lateinit var donationPaymentComponent: DonationPaymentComponent

  private val viewModel: SetCurrencyViewModel by viewModels(
    factoryProducer = {
      val args = SetCurrencyFragmentArgs.fromBundle(requireArguments())
      SetCurrencyViewModel.Factory(args.isBoost, args.supportedCurrencyCodes.toList())
    }
  )

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    donationPaymentComponent = requireListener()

    viewModel.state.observe(viewLifecycleOwner) { state ->
      adapter.submitList(getConfiguration(state).toMappingModelList())
    }
  }

  private fun getConfiguration(state: SetCurrencyState): DSLConfiguration {
    return configure {
      state.currencies.forEach { currency ->
        clickPref(
          title = DSLSettingsText.from(currency.getDisplayName(Locale.getDefault())),
          summary = DSLSettingsText.from(currency.currencyCode),
          onClick = {
            viewModel.setSelectedCurrency(currency.currencyCode)
            donationPaymentComponent.donationPaymentRepository.scheduleSyncForAccountRecordChange()
            dismissAllowingStateLoss()
          }
        )
      }
    }
  }
}
