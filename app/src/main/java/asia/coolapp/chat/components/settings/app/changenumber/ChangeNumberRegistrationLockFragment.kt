package asia.coolapp.chat.components.settings.app.changenumber

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.app.changenumber.ChangeNumberUtil.changeNumberSuccess
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.lock.PinHashing
import asia.coolapp.chat.registration.fragments.BaseRegistrationLockFragment
import asia.coolapp.chat.registration.viewmodel.BaseRegistrationViewModel
import asia.coolapp.chat.util.CircularProgressButtonUtil.cancelSpinning
import asia.coolapp.chat.util.CommunicationActions
import asia.coolapp.chat.util.SupportEmailUtil
import asia.coolapp.chat.util.navigation.safeNavigate

class ChangeNumberRegistrationLockFragment : BaseRegistrationLockFragment(R.layout.fragment_change_number_registration_lock) {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    toolbar.setNavigationOnClickListener { navigateUp() }

    requireActivity().onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          navigateUp()
        }
      }
    )
  }

  override fun getViewModel(): BaseRegistrationViewModel {
    return ChangeNumberUtil.getViewModel(this)
  }

  override fun navigateToAccountLocked() {
    findNavController().safeNavigate(ChangeNumberRegistrationLockFragmentDirections.actionChangeNumberRegistrationLockToChangeNumberAccountLocked())
  }

  override fun handleSuccessfulPinEntry(pin: String) {
    val pinsDiffer: Boolean = SignalStore.kbsValues().localPinHash?.let { !PinHashing.verifyLocalPinHash(it, pin) } ?: false

    cancelSpinning(pinButton)

    if (pinsDiffer) {
      findNavController().safeNavigate(ChangeNumberRegistrationLockFragmentDirections.actionChangeNumberRegistrationLockToChangeNumberPinDiffers())
    } else {
      changeNumberSuccess()
    }
  }

  override fun sendEmailToSupport() {
    val subject = R.string.ChangeNumberRegistrationLockFragment__signal_change_number_need_help_with_pin_for_android_v2_pin

    val body: String = SupportEmailUtil.generateSupportEmailBody(
      requireContext(),
      subject,
      null,
      null
    )

    CommunicationActions.openEmail(
      requireContext(),
      SupportEmailUtil.getSupportEmailAddress(requireContext()),
      getString(subject),
      body
    )
  }

  private fun navigateUp() {
    if (SignalStore.misc().isChangeNumberLocked) {
      startActivity(ChangeNumberLockActivity.createIntent(requireContext()))
    } else {
      findNavController().navigateUp()
    }
  }
}
