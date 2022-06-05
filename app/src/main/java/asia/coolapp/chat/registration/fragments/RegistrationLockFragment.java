package asia.coolapp.chat.registration.fragments;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.R;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobs.StorageAccountRestoreJob;
import asia.coolapp.chat.jobs.StorageSyncJob;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.registration.viewmodel.BaseRegistrationViewModel;
import asia.coolapp.chat.registration.viewmodel.RegistrationViewModel;
import asia.coolapp.chat.util.CommunicationActions;
import asia.coolapp.chat.util.FeatureFlags;
import asia.coolapp.chat.util.Stopwatch;
import asia.coolapp.chat.util.SupportEmailUtil;
import asia.coolapp.chat.util.concurrent.SimpleTask;
import asia.coolapp.chat.util.navigation.SafeNavigation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static asia.coolapp.chat.util.CircularProgressButtonUtil.cancelSpinning;

public final class RegistrationLockFragment extends BaseRegistrationLockFragment {

  private static final String TAG = Log.tag(RegistrationLockFragment.class);

  public RegistrationLockFragment() {
    super(R.layout.fragment_registration_lock);
  }

  @Override
  protected BaseRegistrationViewModel getViewModel() {
    return new ViewModelProvider(requireActivity()).get(RegistrationViewModel.class);
  }

  @Override
  protected void navigateToAccountLocked() {
    SafeNavigation.safeNavigate(Navigation.findNavController(requireView()), RegistrationLockFragmentDirections.actionAccountLocked());
  }

  @Override
  protected void handleSuccessfulPinEntry(@NonNull String pin) {
    SignalStore.pinValues().setKeyboardType(getPinEntryKeyboardType());

    SimpleTask.run(() -> {
      SignalStore.onboarding().clearAll();

      Stopwatch stopwatch = new Stopwatch("RegistrationLockRestore");

      ApplicationDependencies.getJobManager().runSynchronously(new StorageAccountRestoreJob(), StorageAccountRestoreJob.LIFESPAN);
      stopwatch.split("AccountRestore");

      ApplicationDependencies.getJobManager().runSynchronously(new StorageSyncJob(), TimeUnit.SECONDS.toMillis(10));
      stopwatch.split("ContactRestore");

      try {
        FeatureFlags.refreshSync();
      } catch (IOException e) {
        Log.w(TAG, "Failed to refresh flags.", e);
      }
      stopwatch.split("FeatureFlags");

      stopwatch.stop(TAG);

      return null;
    }, none -> {
      cancelSpinning(pinButton);
      SafeNavigation.safeNavigate(Navigation.findNavController(requireView()), RegistrationLockFragmentDirections.actionSuccessfulRegistration());
    });
  }

  @Override
  protected void sendEmailToSupport() {
    int subject = R.string.RegistrationLockFragment__signal_registration_need_help_with_pin_for_android_v2_pin;

    String body = SupportEmailUtil.generateSupportEmailBody(requireContext(),
                                                            subject,
                                                            null,
                                                            null);
    CommunicationActions.openEmail(requireContext(),
                                   SupportEmailUtil.getSupportEmailAddress(requireContext()),
                                   getString(subject),
                                   body);
  }
}
