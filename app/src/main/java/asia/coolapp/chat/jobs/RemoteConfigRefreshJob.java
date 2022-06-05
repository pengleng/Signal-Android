package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.util.FeatureFlags;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RemoteConfigRefreshJob extends BaseJob {

  private static final String TAG = Log.tag(RemoteConfigRefreshJob.class);

  public static final String KEY = "RemoteConfigRefreshJob";

  public RemoteConfigRefreshJob() {
    this(new Job.Parameters.Builder()
                           .setQueue("RemoteConfigRefreshJob")
                           .setMaxInstancesForFactory(1)
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .build());
  }

  private RemoteConfigRefreshJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull Data serialize() {
    return Data.EMPTY;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws Exception {
    if (!SignalStore.account().isRegistered()) {
      Log.w(TAG, "Not registered. Skipping.");
      return;
    }

    Map<String, Object> config = ApplicationDependencies.getSignalServiceAccountManager().getRemoteConfig();
    FeatureFlags.update(config);
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<RemoteConfigRefreshJob> {
    @Override
    public @NonNull RemoteConfigRefreshJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new RemoteConfigRefreshJob(parameters);
    }
  }
}
