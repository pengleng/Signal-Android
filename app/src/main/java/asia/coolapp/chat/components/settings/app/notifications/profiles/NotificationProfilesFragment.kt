package asia.coolapp.chat.components.settings.app.notifications.profiles

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import asia.coolapp.chat.R
import asia.coolapp.chat.components.emoji.EmojiUtil
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsFragment
import asia.coolapp.chat.components.settings.DSLSettingsIcon
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.NO_TINT
import asia.coolapp.chat.components.settings.app.notifications.profiles.models.NoNotificationProfiles
import asia.coolapp.chat.components.settings.app.notifications.profiles.models.NotificationProfilePreference
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.components.settings.conversation.preferences.LargeIconClickPreference
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.megaphone.Megaphones
import asia.coolapp.chat.notifications.profiles.NotificationProfile
import asia.coolapp.chat.notifications.profiles.NotificationProfiles
import asia.coolapp.chat.util.LifecycleDisposable
import asia.coolapp.chat.util.navigation.safeNavigate

/**
 * Primary entry point for Notification Profiles. When user has no profiles, shows empty state, otherwise shows
 * all current profiles.
 */
class NotificationProfilesFragment : DSLSettingsFragment() {

  private val viewModel: NotificationProfilesViewModel by viewModels(
    factoryProducer = { NotificationProfilesViewModel.Factory() }
  )

  private val lifecycleDisposable = LifecycleDisposable()
  private var toolbar: Toolbar? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ApplicationDependencies.getMegaphoneRepository().markFinished(Megaphones.Event.NOTIFICATION_PROFILES)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    toolbar = view.findViewById(R.id.toolbar)

    lifecycleDisposable.bindTo(viewLifecycleOwner.lifecycle)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    toolbar = null
  }

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    NoNotificationProfiles.register(adapter)
    LargeIconClickPreference.register(adapter)
    NotificationProfilePreference.register(adapter)

    lifecycleDisposable += viewModel.getProfiles()
      .subscribe { profiles ->
        if (profiles.isEmpty()) {
          toolbar?.title = ""
        } else {
          toolbar?.setTitle(R.string.NotificationsSettingsFragment__notification_profiles)
        }
        adapter.submitList(getConfiguration(profiles).toMappingModelList())
      }
  }

  private fun getConfiguration(profiles: List<NotificationProfile>): DSLConfiguration {
    return configure {
      if (profiles.isEmpty()) {
        customPref(
          NoNotificationProfiles.Model(
            onClick = { findNavController().safeNavigate(R.id.action_notificationProfilesFragment_to_editNotificationProfileFragment) }
          )
        )
      } else {
        sectionHeaderPref(R.string.NotificationProfilesFragment__profiles)

        customPref(
          LargeIconClickPreference.Model(
            title = DSLSettingsText.from(R.string.NotificationProfilesFragment__new_profile),
            icon = DSLSettingsIcon.from(R.drawable.add_to_a_group, NO_TINT),
            onClick = { findNavController().safeNavigate(R.id.action_notificationProfilesFragment_to_editNotificationProfileFragment) }
          )
        )

        val activeProfile: NotificationProfile? = NotificationProfiles.getActiveProfile(profiles)
        profiles.sortedDescending().forEach { profile ->
          customPref(
            NotificationProfilePreference.Model(
              title = DSLSettingsText.from(profile.name),
              summary = if (profile == activeProfile) DSLSettingsText.from(NotificationProfiles.getActiveProfileDescription(requireContext(), profile)) else null,
              icon = if (profile.emoji.isNotEmpty()) EmojiUtil.convertToDrawable(requireContext(), profile.emoji)?.let { DSLSettingsIcon.from(it) } else DSLSettingsIcon.from(R.drawable.ic_moon_24, NO_TINT),
              color = profile.color,
              onClick = {
                findNavController().safeNavigate(NotificationProfilesFragmentDirections.actionNotificationProfilesFragmentToNotificationProfileDetailsFragment(profile.id))
              }
            )
          )
        }
      }
    }
  }
}
