package asia.coolapp.chat.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.util.TextSecurePreferences;

public class LogSectionPin implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "PIN STATE";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    return new StringBuilder().append("State: ").append(SignalStore.pinValues().getPinState()).append("\n")
                              .append("Last Successful Reminder Entry: ").append(SignalStore.pinValues().getLastSuccessfulEntryTime()).append("\n")
                              .append("Next Reminder Interval: ").append(SignalStore.pinValues().getCurrentInterval()).append("\n")
                              .append("ReglockV1: ").append(TextSecurePreferences.isV1RegistrationLockEnabled(context)).append("\n")
                              .append("ReglockV2: ").append(SignalStore.kbsValues().isV2RegistrationLockEnabled()).append("\n")
                              .append("Signal PIN: ").append(SignalStore.kbsValues().hasPin()).append("\n")
                              .append("Opted Out: ").append(SignalStore.kbsValues().hasOptedOut()).append("\n")
                              .append("Last Creation Failed: ").append(SignalStore.kbsValues().lastPinCreateFailed()).append("\n")
                              .append("Needs Account Restore: ").append(SignalStore.storageService().needsAccountRestore()).append("\n")
                              .append("PIN Required at Registration: ").append(SignalStore.registrationValues().pinWasRequiredAtRegistration()).append("\n")
                              .append("Registration Complete: ").append(SignalStore.registrationValues().isRegistrationComplete());

  }
}
