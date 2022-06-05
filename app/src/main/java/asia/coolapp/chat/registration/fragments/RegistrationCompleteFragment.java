package asia.coolapp.chat.registration.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ActivityNavigator;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.LoggingFragment;
import asia.coolapp.chat.MainActivity;
import asia.coolapp.chat.R;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobs.MultiDeviceProfileContentUpdateJob;
import asia.coolapp.chat.jobs.MultiDeviceProfileKeyUpdateJob;
import asia.coolapp.chat.jobs.ProfileUploadJob;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.lock.v2.CreateKbsPinActivity;
import asia.coolapp.chat.pin.PinRestoreActivity;
import asia.coolapp.chat.profiles.AvatarHelper;
import asia.coolapp.chat.profiles.edit.EditProfileActivity;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.registration.RegistrationUtil;
import asia.coolapp.chat.registration.viewmodel.RegistrationViewModel;

import java.util.Arrays;

public final class RegistrationCompleteFragment extends LoggingFragment {

  private static final String TAG = Log.tag(RegistrationCompleteFragment.class);

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_registration_blank, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    FragmentActivity      activity  = requireActivity();
    RegistrationViewModel viewModel = new ViewModelProvider(activity).get(RegistrationViewModel.class);

    if (SignalStore.storageService().needsAccountRestore()) {
      Log.i(TAG, "Performing pin restore");
      activity.startActivity(new Intent(activity, PinRestoreActivity.class));
    } else if (!viewModel.isReregister()) {
      boolean needsProfile = Recipient.self().getProfileName().isEmpty() || !AvatarHelper.hasAvatar(activity, Recipient.self().getId());
      boolean needsPin     = !SignalStore.kbsValues().hasPin();

      Log.i(TAG, "Pin restore flow not required." +
                 " profile name: "   + Recipient.self().getProfileName().isEmpty() +
                 " profile avatar: " + !AvatarHelper.hasAvatar(activity, Recipient.self().getId()) +
                 " needsPin:"        + needsPin);

      Intent startIntent = MainActivity.clearTop(activity);

      if (needsPin) {
        startIntent = chainIntents(CreateKbsPinActivity.getIntentForPinCreate(requireContext()), startIntent);
      }

      if (needsProfile) {
        startIntent = chainIntents(EditProfileActivity.getIntentForUserProfile(activity), startIntent);
      }

      if (!needsProfile && !needsPin) {
        ApplicationDependencies.getJobManager()
                               .startChain(new ProfileUploadJob())
                               .then(Arrays.asList(new MultiDeviceProfileKeyUpdateJob(), new MultiDeviceProfileContentUpdateJob()))
                               .enqueue();

        RegistrationUtil.maybeMarkRegistrationComplete(requireContext());
      }

      activity.startActivity(startIntent);
    }

    activity.finish();
    ActivityNavigator.applyPopAnimationsToPendingTransition(activity);
  }

  private static @NonNull Intent chainIntents(@NonNull Intent sourceIntent, @NonNull Intent nextIntent) {
    sourceIntent.putExtra("next_intent", nextIntent);
    return sourceIntent;
  }
}
