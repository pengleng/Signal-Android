package asia.coolapp.chat.service;


import android.content.Context;
import android.content.Intent;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.BuildConfig;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobs.UpdateApkJob;
import asia.coolapp.chat.util.TextSecurePreferences;

import java.util.concurrent.TimeUnit;

public class UpdateApkRefreshListener extends PersistentAlarmManagerListener {

  private static final String TAG = Log.tag(UpdateApkRefreshListener.class);

  private static final long INTERVAL = TimeUnit.HOURS.toMillis(6);

  @Override
  protected long getNextScheduledExecutionTime(Context context) {
    return TextSecurePreferences.getUpdateApkRefreshTime(context);
  }

  @Override
  protected long onAlarm(Context context, long scheduledTime) {
    Log.i(TAG, "onAlarm...");

    if (scheduledTime != 0 && BuildConfig.PLAY_STORE_DISABLED) {
      Log.i(TAG, "Queueing APK update job...");
      ApplicationDependencies.getJobManager().add(new UpdateApkJob());
    }

    long newTime = System.currentTimeMillis() + INTERVAL;
    TextSecurePreferences.setUpdateApkRefreshTime(context, newTime);

    return newTime;
  }

  public static void schedule(Context context) {
    new UpdateApkRefreshListener().onReceive(context, new Intent());
  }

}
