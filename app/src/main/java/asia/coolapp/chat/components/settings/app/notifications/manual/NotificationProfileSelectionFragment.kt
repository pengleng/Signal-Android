package asia.coolapp.chat.components.settings.app.notifications.manual

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import org.signal.core.util.DimensionUnit
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsBottomSheetFragment
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.app.AppSettingsActivity
import asia.coolapp.chat.components.settings.app.notifications.manual.models.NotificationProfileSelection
import asia.coolapp.chat.components.settings.app.notifications.profiles.NotificationProfilesRepository
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.notifications.profiles.NotificationProfile
import asia.coolapp.chat.notifications.profiles.NotificationProfiles
import asia.coolapp.chat.util.BottomSheetUtil

/**
 * BottomSheetDialogFragment that allows a user to select a notification profile to manually enable/disable.
 */
class NotificationProfileSelectionFragment : DSLSettingsBottomSheetFragment() {

  private val viewModel: NotificationProfileSelectionViewModel by viewModels(
    factoryProducer = {
      NotificationProfileSelectionViewModel.Factory(NotificationProfilesRepository())
    }
  )

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    NotificationProfileSelection.register(adapter)

    recyclerView.itemAnimator = null

    viewModel.state.observe(viewLifecycleOwner) {
      adapter.submitList(getConfiguration(it).toMappingModelList())
    }
  }

  private fun getConfiguration(state: NotificationProfileSelectionState): DSLConfiguration {
    val activeProfile: NotificationProfile? = NotificationProfiles.getActiveProfile(state.notificationProfiles)

    return configure {

      state.notificationProfiles.sortedDescending().forEach { profile ->
        customPref(
          NotificationProfileSelection.Entry(
            isOn = profile == activeProfile,
            summary = if (profile == activeProfile) DSLSettingsText.from(NotificationProfiles.getActiveProfileDescription(requireContext(), profile)) else DSLSettingsText.from(R.string.NotificationProfileDetails__off),
            notificationProfile = profile,
            isExpanded = profile.id == state.expandedId,
            timeSlotB = state.timeSlotB,
            onRowClick = viewModel::toggleEnabled,
            onTimeSlotAClick = viewModel::enableForOneHour,
            onTimeSlotBClick = viewModel::enableUntil,
            onToggleClick = viewModel::setExpanded,
            onViewSettingsClick = { navigateToSettings(it) }
          )
        )
        space(DimensionUnit.DP.toPixels(16f).toInt())
      }

      customPref(
        NotificationProfileSelection.New(
          onClick = {
            startActivity(AppSettingsActivity.createNotificationProfile(requireContext()))
            dismissAllowingStateLoss()
          }
        )
      )

      space(DimensionUnit.DP.toPixels(20f).toInt())
    }
  }

  private fun navigateToSettings(notificationProfile: NotificationProfile) {
    startActivity(AppSettingsActivity.notificationProfileDetails(requireContext(), notificationProfile.id))
    dismissAllowingStateLoss()
  }

  companion object {
    @JvmStatic
    fun show(fragmentManager: FragmentManager) {
      NotificationProfileSelectionFragment().show(fragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG)
    }
  }
}
