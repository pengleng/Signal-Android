package asia.coolapp.chat.components.settings.app.notifications.profiles

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.kotlin.subscribeBy
import asia.coolapp.chat.R
import asia.coolapp.chat.components.emoji.EmojiUtil
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsFragment
import asia.coolapp.chat.components.settings.DSLSettingsIcon
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.NO_TINT
import asia.coolapp.chat.components.settings.app.notifications.profiles.models.NotificationProfileAddMembers
import asia.coolapp.chat.components.settings.app.notifications.profiles.models.NotificationProfilePreference
import asia.coolapp.chat.components.settings.app.notifications.profiles.models.NotificationProfileRecipient
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.components.settings.conversation.preferences.LargeIconClickPreference
import asia.coolapp.chat.components.settings.conversation.preferences.RecipientPreference
import asia.coolapp.chat.notifications.profiles.NotificationProfile
import asia.coolapp.chat.notifications.profiles.NotificationProfileSchedule
import asia.coolapp.chat.notifications.profiles.NotificationProfiles
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.util.LifecycleDisposable
import asia.coolapp.chat.util.SpanUtil
import asia.coolapp.chat.util.formatHours
import asia.coolapp.chat.util.navigation.safeNavigate
import asia.coolapp.chat.util.orderOfDaysInWeek
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private const val MEMBER_COUNT_TO_SHOW_EXPAND: Int = 5

class NotificationProfileDetailsFragment : DSLSettingsFragment() {

  private val viewModel: NotificationProfileDetailsViewModel by viewModels(factoryProducer = this::createFactory)

  private val lifecycleDisposable = LifecycleDisposable()
  private var toolbar: Toolbar? = null

  private fun createFactory(): ViewModelProvider.Factory {
    return NotificationProfileDetailsViewModel.Factory(NotificationProfileDetailsFragmentArgs.fromBundle(requireArguments()).profileId)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    toolbar = view.findViewById(R.id.toolbar)
    toolbar?.inflateMenu(R.menu.notification_profile_details)

    lifecycleDisposable.bindTo(viewLifecycleOwner.lifecycle)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    toolbar = null
  }

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    NotificationProfilePreference.register(adapter)
    NotificationProfileAddMembers.register(adapter)
    NotificationProfileRecipient.register(adapter)
    LargeIconClickPreference.register(adapter)

    viewModel.state.observe(viewLifecycleOwner) { state ->
      when (state) {
        is NotificationProfileDetailsViewModel.State.Valid -> {
          toolbar?.title = state.profile.name
          toolbar?.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_edit) {
              findNavController().safeNavigate(NotificationProfileDetailsFragmentDirections.actionNotificationProfileDetailsFragmentToEditNotificationProfileFragment().setProfileId(state.profile.id))
              true
            } else {
              false
            }
          }
          adapter.submitList(getConfiguration(state).toMappingModelList())
        }
        NotificationProfileDetailsViewModel.State.NotLoaded -> Unit
        NotificationProfileDetailsViewModel.State.Invalid -> requireActivity().onBackPressed()
      }
    }
  }

  private fun getConfiguration(state: NotificationProfileDetailsViewModel.State.Valid): DSLConfiguration {
    val (profile: NotificationProfile, recipients: List<Recipient>, isOn: Boolean, expanded: Boolean) = state

    return configure {

      customPref(
        NotificationProfilePreference.Model(
          title = DSLSettingsText.from(profile.name),
          summary = if (isOn) DSLSettingsText.from(NotificationProfiles.getActiveProfileDescription(requireContext(), profile)) else null,
          icon = if (profile.emoji.isNotEmpty()) EmojiUtil.convertToDrawable(requireContext(), profile.emoji)?.let { DSLSettingsIcon.from(it) } else DSLSettingsIcon.from(R.drawable.ic_moon_24, NO_TINT),
          color = profile.color,
          isOn = isOn,
          showSwitch = true,
          onClick = {
            lifecycleDisposable += viewModel.toggleEnabled(profile)
              .subscribe()
          }
        )
      )

      dividerPref()

      sectionHeaderPref(R.string.AddAllowedMembers__allowed_notifications)
      customPref(
        NotificationProfileAddMembers.Model(
          onClick = { id, currentSelection ->
            findNavController().safeNavigate(
              NotificationProfileDetailsFragmentDirections.actionNotificationProfileDetailsFragmentToSelectRecipientsFragment(id)
                .setCurrentSelection(currentSelection.toTypedArray())
            )
          },
          profileId = profile.id,
          currentSelection = profile.allowedMembers
        )
      )

      val membersToShow = if (expanded || recipients.size <= MEMBER_COUNT_TO_SHOW_EXPAND) {
        recipients
      } else {
        recipients.slice(0 until MEMBER_COUNT_TO_SHOW_EXPAND)
      }

      for (member in membersToShow) {
        customPref(
          NotificationProfileRecipient.Model(
            recipientModel = RecipientPreference.Model(
              recipient = member
            ),
            onRemoveClick = { id ->
              lifecycleDisposable += viewModel.removeMember(id)
                .subscribeBy(
                  onSuccess = { removed ->
                    view?.let { view ->
                      Snackbar.make(view, getString(R.string.NotificationProfileDetails__s_removed, removed.getDisplayName(requireContext())), Snackbar.LENGTH_LONG)
                        .setAction(R.string.NotificationProfileDetails__undo) { undoRemove(id) }
                        .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.core_ultramarine_light))
                        .setTextColor(Color.WHITE)
                        .show()
                    }
                  }
                )
            }
          )
        )
      }

      if (!expanded && membersToShow != recipients) {
        customPref(
          LargeIconClickPreference.Model(
            title = DSLSettingsText.from(R.string.NotificationProfileDetails__see_all),
            icon = DSLSettingsIcon.from(R.drawable.show_more, NO_TINT),
            onClick = viewModel::showAllMembers
          )
        )
      }

      dividerPref()
      sectionHeaderPref(R.string.NotificationProfileDetails__schedule)
      clickPref(
        title = DSLSettingsText.from(profile.schedule.describe()),
        summary = DSLSettingsText.from(if (profile.schedule.enabled) R.string.NotificationProfileDetails__on else R.string.NotificationProfileDetails__off),
        icon = DSLSettingsIcon.from(R.drawable.ic_recent_20, NO_TINT),
        onClick = {
          findNavController().safeNavigate(NotificationProfileDetailsFragmentDirections.actionNotificationProfileDetailsFragmentToEditNotificationProfileScheduleFragment(profile.id, false))
        }
      )

      dividerPref()
      sectionHeaderPref(R.string.NotificationProfileDetails__exceptions)
      switchPref(
        title = DSLSettingsText.from(R.string.NotificationProfileDetails__allow_all_calls),
        isChecked = profile.allowAllCalls,
        icon = DSLSettingsIcon.from(R.drawable.ic_phone_right_24),
        onClick = {
          lifecycleDisposable += viewModel.toggleAllowAllCalls()
            .subscribe()
        }
      )
      switchPref(
        title = DSLSettingsText.from(R.string.NotificationProfileDetails__notify_for_all_mentions),
        icon = DSLSettingsIcon.from(R.drawable.ic_at_24),
        isChecked = profile.allowAllMentions,
        onClick = {
          lifecycleDisposable += viewModel.toggleAllowAllMentions()
            .subscribe()
        }
      )

      dividerPref()
      clickPref(
        title = DSLSettingsText.from(R.string.NotificationProfileDetails__delete_profile, ContextCompat.getColor(requireContext(), R.color.signal_alert_primary)),
        icon = DSLSettingsIcon.from(R.drawable.ic_delete_24, R.color.signal_alert_primary),
        onClick = {
          MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.NotificationProfileDetails__permanently_delete_profile)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(
              SpanUtil.color(
                ContextCompat.getColor(requireContext(), R.color.signal_alert_primary),
                getString(R.string.NotificationProfileDetails__delete)
              )
            ) { _, _ -> deleteProfile() }
            .show()
        }
      )
    }
  }

  private fun deleteProfile() {
    lifecycleDisposable += viewModel.deleteProfile()
      .subscribe()
  }

  private fun undoRemove(id: RecipientId) {
    lifecycleDisposable += viewModel.addMember(id)
      .subscribe()
  }

  private fun NotificationProfileSchedule.describe(): String {
    if (!enabled) {
      return getString(R.string.NotificationProfileDetails__schedule)
    }

    val startTime = startTime().formatHours(requireContext())
    val endTime = endTime().formatHours(requireContext())

    val days = StringBuilder()
    if (daysEnabled.size == 7) {
      days.append(getString(R.string.NotificationProfileDetails__everyday))
    } else {
      for (day: DayOfWeek in Locale.getDefault().orderOfDaysInWeek()) {
        if (daysEnabled.contains(day)) {
          if (days.isNotEmpty()) {
            days.append(", ")
          }
          days.append(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        }
      }
    }

    return getString(R.string.NotificationProfileDetails__s_to_s, startTime, endTime).let { hours ->
      if (days.isNotEmpty()) "$hours\n$days" else hours
    }
  }
}
