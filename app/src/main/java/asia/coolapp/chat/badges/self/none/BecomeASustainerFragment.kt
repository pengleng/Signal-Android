package asia.coolapp.chat.badges.self.none

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import org.signal.core.util.DimensionUnit
import asia.coolapp.chat.R
import asia.coolapp.chat.badges.models.BadgePreview
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsBottomSheetFragment
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.app.AppSettingsActivity
import asia.coolapp.chat.components.settings.app.subscription.SubscriptionsRepository
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.util.BottomSheetUtil

class BecomeASustainerFragment : DSLSettingsBottomSheetFragment() {

  private val viewModel: BecomeASustainerViewModel by viewModels(
    factoryProducer = {
      BecomeASustainerViewModel.Factory(SubscriptionsRepository(ApplicationDependencies.getDonationsService()))
    }
  )

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    BadgePreview.register(adapter)

    viewModel.state.observe(viewLifecycleOwner) {
      adapter.submitList(getConfiguration(it).toMappingModelList())
    }
  }

  private fun getConfiguration(state: BecomeASustainerState): DSLConfiguration {
    return configure {
      customPref(BadgePreview.Model(badge = state.badge))

      sectionHeaderPref(
        title = DSLSettingsText.from(
          R.string.BecomeASustainerFragment__get_badges,
          DSLSettingsText.CenterModifier,
          DSLSettingsText.Title2BoldModifier
        )
      )

      space(DimensionUnit.DP.toPixels(8f).toInt())

      noPadTextPref(
        title = DSLSettingsText.from(
          R.string.BecomeASustainerFragment__signal_is_a_non_profit,
          DSLSettingsText.CenterModifier
        )
      )

      space(DimensionUnit.DP.toPixels(77f).toInt())

      primaryButton(
        text = DSLSettingsText.from(
          R.string.BecomeASustainerMegaphone__become_a_sustainer
        ),
        onClick = {
          requireActivity().finish()
          requireActivity().startActivity(AppSettingsActivity.subscriptions(requireContext()).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
      )

      space(DimensionUnit.DP.toPixels(8f).toInt())
    }
  }

  companion object {
    @JvmStatic
    fun show(fragmentManager: FragmentManager) {
      BecomeASustainerFragment().show(fragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG)
    }
  }
}
