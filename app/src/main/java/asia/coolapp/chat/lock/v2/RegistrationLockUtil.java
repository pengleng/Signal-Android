package asia.coolapp.chat.lock.v2;

import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.util.TextSecurePreferences;

public final class RegistrationLockUtil {

  private RegistrationLockUtil() {}

  public static boolean userHasRegistrationLock(@NonNull Context context) {
    return TextSecurePreferences.isV1RegistrationLockEnabled(context) || SignalStore.kbsValues().isV2RegistrationLockEnabled();
  }
}
