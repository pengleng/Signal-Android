package asia.coolapp.chat.registration.fragments;

import androidx.lifecycle.ViewModelProvider;

import asia.coolapp.chat.R;
import asia.coolapp.chat.registration.viewmodel.BaseRegistrationViewModel;
import asia.coolapp.chat.registration.viewmodel.RegistrationViewModel;

public class AccountLockedFragment extends BaseAccountLockedFragment {

  public AccountLockedFragment() {
    super(R.layout.account_locked_fragment);
  }

  @Override
  protected BaseRegistrationViewModel getViewModel() {
    return new ViewModelProvider(requireActivity()).get(RegistrationViewModel.class);
  }

  @Override
  protected void onNext() {
    requireActivity().finish();
  }
}
