package asia.coolapp.chat.migrations;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobs.RefreshAttributesJob;
import asia.coolapp.chat.jobs.RefreshOwnProfileJob;
import asia.coolapp.chat.jobs.StorageForcePushJob;
import asia.coolapp.chat.keyvalue.SignalStore;

/**
 * We changed some details of what it means to opt-out of a PIN. This ensures that users who went
 * through the previous opt-out flow are now in the same state as users who went through the new
 * opt-out flow.
 */
public final class PinOptOutMigration extends MigrationJob {

  private static final String TAG = Log.tag(PinOptOutMigration.class);

  public static final String KEY = "PinOptOutMigration";

  PinOptOutMigration() {
    this(new Parameters.Builder().build());
  }

  private PinOptOutMigration(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  boolean isUiBlocking() {
    return false;
  }

  @Override
  void performMigration() {
    if (SignalStore.kbsValues().hasOptedOut() && SignalStore.kbsValues().hasPin()) {
      Log.w(TAG, "Discovered a legacy opt-out user! Resetting the state.");

      SignalStore.kbsValues().optOut();
      ApplicationDependencies.getJobManager().startChain(new RefreshAttributesJob())
                                             .then(new RefreshOwnProfileJob())
                                             .then(new StorageForcePushJob())
                                             .enqueue();
    } else if (SignalStore.kbsValues().hasOptedOut()) {
      Log.i(TAG, "Discovered an opt-out user, but they're already in a good state. No action required.");
    } else {
      Log.i(TAG, "Discovered a normal PIN user. No action required.");
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  public static class Factory implements Job.Factory<PinOptOutMigration> {
    @Override
    public @NonNull PinOptOutMigration create(@NonNull Parameters parameters, @NonNull Data data) {
      return new PinOptOutMigration(parameters);
    }
  }
}
