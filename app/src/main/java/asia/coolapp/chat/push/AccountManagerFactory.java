package asia.coolapp.chat.push;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.security.ProviderInstaller;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.BuildConfig;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.util.FeatureFlags;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.ACI;
import org.whispersystems.signalservice.api.push.PNI;

public class AccountManagerFactory {

  private static final String TAG = Log.tag(AccountManagerFactory.class);

  public static @NonNull SignalServiceAccountManager createAuthenticated(@NonNull Context context,
                                                                         @NonNull ACI aci,
                                                                         @NonNull PNI pni,
                                                                         @NonNull String number,
                                                                         int deviceId,
                                                                         @NonNull String password)
  {
    if (ApplicationDependencies.getSignalServiceNetworkAccess().isCensored(number)) {
      SignalExecutors.BOUNDED.execute(() -> {
        try {
          ProviderInstaller.installIfNeeded(context);
        } catch (Throwable t) {
          Log.w(TAG, t);
        }
      });
    }

    return new SignalServiceAccountManager(ApplicationDependencies.getSignalServiceNetworkAccess().getConfiguration(number),
                                           aci,
                                           pni,
                                           number,
                                           deviceId,
                                           password,
                                           BuildConfig.SIGNAL_AGENT,
                                           FeatureFlags.okHttpAutomaticRetry(),
                                           FeatureFlags.groupLimits().getHardLimit());
  }

  /**
   * Should only be used during registration when you haven't yet been assigned an ACI.
   */
  public static @NonNull SignalServiceAccountManager createUnauthenticated(@NonNull Context context,
                                                                           @NonNull String number,
                                                                           int deviceId,
                                                                           @NonNull String password)
  {
    if (new SignalServiceNetworkAccess(context).isCensored(number)) {
      SignalExecutors.BOUNDED.execute(() -> {
        try {
          ProviderInstaller.installIfNeeded(context);
        } catch (Throwable t) {
          Log.w(TAG, t);
        }
      });
    }

    return new SignalServiceAccountManager(new SignalServiceNetworkAccess(context).getConfiguration(number),
                                           null,
                                           null,
                                           number,
                                           deviceId,
                                           password,
                                           BuildConfig.SIGNAL_AGENT,
                                           FeatureFlags.okHttpAutomaticRetry(),
                                           FeatureFlags.groupLimits().getHardLimit());
  }

}
