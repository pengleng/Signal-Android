package asia.coolapp.chat.components.settings.app.changenumber

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import asia.coolapp.chat.LoggingFragment
import asia.coolapp.chat.R
import asia.coolapp.chat.components.LabeledEditText
import asia.coolapp.chat.components.settings.app.changenumber.ChangeNumberUtil.getViewModel
import asia.coolapp.chat.components.settings.app.changenumber.ChangeNumberViewModel.ContinueStatus
import asia.coolapp.chat.registration.fragments.CountryPickerFragment
import asia.coolapp.chat.registration.fragments.CountryPickerFragmentArgs
import asia.coolapp.chat.registration.util.RegistrationNumberInputController
import asia.coolapp.chat.util.Dialogs
import asia.coolapp.chat.util.navigation.safeNavigate

private const val OLD_NUMBER_COUNTRY_SELECT = "old_number_country"
private const val NEW_NUMBER_COUNTRY_SELECT = "new_number_country"

class ChangeNumberEnterPhoneNumberFragment : LoggingFragment(R.layout.fragment_change_number_enter_phone_number) {

  private lateinit var scrollView: ScrollView

  private lateinit var oldNumberCountrySpinner: Spinner
  private lateinit var oldNumberCountryCode: LabeledEditText
  private lateinit var oldNumber: LabeledEditText

  private lateinit var newNumberCountrySpinner: Spinner
  private lateinit var newNumberCountryCode: LabeledEditText
  private lateinit var newNumber: LabeledEditText

  private lateinit var viewModel: ChangeNumberViewModel

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel = getViewModel(this)

    val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    toolbar.setTitle(R.string.ChangeNumberEnterPhoneNumberFragment__change_number)
    toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

    view.findViewById<View>(R.id.change_number_enter_phone_number_continue).setOnClickListener {
      onContinue()
    }

    scrollView = view.findViewById(R.id.change_number_enter_phone_number_scroll)

    oldNumberCountrySpinner = view.findViewById(R.id.change_number_enter_phone_number_old_number_spinner)
    oldNumberCountryCode = view.findViewById(R.id.change_number_enter_phone_number_old_number_country_code)
    oldNumber = view.findViewById(R.id.change_number_enter_phone_number_old_number_number)

    val oldController = RegistrationNumberInputController(
      requireContext(),
      oldNumberCountryCode,
      oldNumber,
      oldNumberCountrySpinner,
      false,
      object : RegistrationNumberInputController.Callbacks {
        override fun onNumberFocused() {
          scrollView.postDelayed({ scrollView.smoothScrollTo(0, oldNumber.bottom) }, 250)
        }

        override fun onNumberInputNext(view: View) {
          newNumberCountryCode.requestFocus()
        }

        override fun onNumberInputDone(view: View) = Unit

        override fun onPickCountry(view: View) {
          val arguments: CountryPickerFragmentArgs = CountryPickerFragmentArgs.Builder().setResultKey(OLD_NUMBER_COUNTRY_SELECT).build()

          findNavController().safeNavigate(R.id.action_enterPhoneNumberChangeFragment_to_countryPickerFragment, arguments.toBundle())
        }

        override fun setNationalNumber(number: String) {
          viewModel.setOldNationalNumber(number)
        }

        override fun setCountry(countryCode: Int) {
          viewModel.setOldCountry(countryCode)
        }
      }
    )

    newNumberCountrySpinner = view.findViewById(R.id.change_number_enter_phone_number_new_number_spinner)
    newNumberCountryCode = view.findViewById(R.id.change_number_enter_phone_number_new_number_country_code)
    newNumber = view.findViewById(R.id.change_number_enter_phone_number_new_number_number)

    val newController = RegistrationNumberInputController(
      requireContext(),
      newNumberCountryCode,
      newNumber,
      newNumberCountrySpinner,
      true,
      object : RegistrationNumberInputController.Callbacks {
        override fun onNumberFocused() {
          scrollView.postDelayed({ scrollView.smoothScrollTo(0, newNumber.bottom) }, 250)
        }

        override fun onNumberInputNext(view: View) = Unit

        override fun onNumberInputDone(view: View) {
          onContinue()
        }

        override fun onPickCountry(view: View) {
          val arguments: CountryPickerFragmentArgs = CountryPickerFragmentArgs.Builder().setResultKey(NEW_NUMBER_COUNTRY_SELECT).build()

          findNavController().safeNavigate(R.id.action_enterPhoneNumberChangeFragment_to_countryPickerFragment, arguments.toBundle())
        }

        override fun setNationalNumber(number: String) {
          viewModel.setNewNationalNumber(number)
        }

        override fun setCountry(countryCode: Int) {
          viewModel.setNewCountry(countryCode)
        }
      }
    )

    parentFragmentManager.setFragmentResultListener(OLD_NUMBER_COUNTRY_SELECT, this) { _, bundle ->
      viewModel.setOldCountry(bundle.getInt(CountryPickerFragment.KEY_COUNTRY_CODE), bundle.getString(CountryPickerFragment.KEY_COUNTRY))
    }

    parentFragmentManager.setFragmentResultListener(NEW_NUMBER_COUNTRY_SELECT, this) { _, bundle ->
      viewModel.setNewCountry(bundle.getInt(CountryPickerFragment.KEY_COUNTRY_CODE), bundle.getString(CountryPickerFragment.KEY_COUNTRY))
    }

    viewModel.getLiveOldNumber().observe(viewLifecycleOwner, oldController::updateNumber)
    viewModel.getLiveNewNumber().observe(viewLifecycleOwner, newController::updateNumber)
  }

  private fun onContinue() {
    if (TextUtils.isEmpty(oldNumberCountryCode.text)) {
      Toast.makeText(context, getString(R.string.ChangeNumberEnterPhoneNumberFragment__you_must_specify_your_old_number_country_code), Toast.LENGTH_LONG).show()
      return
    }

    if (TextUtils.isEmpty(oldNumber.text)) {
      Toast.makeText(context, getString(R.string.ChangeNumberEnterPhoneNumberFragment__you_must_specify_your_old_phone_number), Toast.LENGTH_LONG).show()
      return
    }

    if (TextUtils.isEmpty(newNumberCountryCode.text)) {
      Toast.makeText(context, getString(R.string.ChangeNumberEnterPhoneNumberFragment__you_must_specify_your_new_number_country_code), Toast.LENGTH_LONG).show()
      return
    }

    if (TextUtils.isEmpty(newNumber.text)) {
      Toast.makeText(context, getString(R.string.ChangeNumberEnterPhoneNumberFragment__you_must_specify_your_new_phone_number), Toast.LENGTH_LONG).show()
      return
    }

    when (viewModel.canContinue()) {
      ContinueStatus.CAN_CONTINUE -> findNavController().safeNavigate(R.id.action_enterPhoneNumberChangeFragment_to_changePhoneNumberConfirmFragment)
      ContinueStatus.INVALID_NUMBER -> {
        Dialogs.showAlertDialog(
          context, getString(R.string.RegistrationActivity_invalid_number), String.format(getString(R.string.RegistrationActivity_the_number_you_specified_s_is_invalid), viewModel.number.e164Number)
        )
      }
      ContinueStatus.OLD_NUMBER_DOESNT_MATCH -> {
        MaterialAlertDialogBuilder(requireContext())
          .setMessage(R.string.ChangeNumberEnterPhoneNumberFragment__the_phone_number_you_entered_doesnt_match_your_accounts)
          .setPositiveButton(android.R.string.ok, null)
          .show()
      }
    }
  }
}
