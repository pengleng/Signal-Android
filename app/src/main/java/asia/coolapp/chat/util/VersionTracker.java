package asia.coolapp.chat.util;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobs.RemoteConfigRefreshJob;
import asia.coolapp.chat.jobs.RetrieveReleaseChannelJob;
import asia.coolapp.chat.keyvalue.SignalStore;

import java.util.concurrent.TimeUnit;

public class VersionTracker {

  private static final String TAG = Log.tag(VersionTracker.class);

  public static int getLastSeenVersion(@NonNull Context context) {
    return TextSecurePreferences.getLastVersionCode(context);
  }

  public static void updateLastSeenVersion(@NonNull Context context) {
    int currentVersionCode = Util.getCanonicalVersionCode();
    int lastVersionCode    = TextSecurePreferences.getLastVersionCode(context);

    if (currentVersionCode != lastVersionCode) {
      Log.i(TAG, "Upgraded from " + lastVersionCode + " to " + currentVersionCode);
      SignalStore.misc().clearClientDeprecated();
      ApplicationDependencies.getJobManager().add(new RemoteConfigRefreshJob());
      RetrieveReleaseChannelJob.enqueue(true);
      LocalMetrics.getInstance().clear();
    }

    TextSecurePreferences.setLastVersionCode(context, currentVersionCode);
  }

  public static long getDaysSinceFirstInstalled(Context context) {
    try {
      long installTimestamp = context.getPackageManager()
                                     .getPackageInfo(context.getPackageName(), 0)
                                     .firstInstallTime;

      return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installTimestamp);
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
      return 0;
    }
  }
}
