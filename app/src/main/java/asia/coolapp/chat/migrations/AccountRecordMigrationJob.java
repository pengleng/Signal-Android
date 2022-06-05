package asia.coolapp.chat.migrations;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobs.StorageSyncJob;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.recipients.Recipient;

/**
 * Marks the AccountRecord as dirty and runs a storage sync. Can be enqueued when we've added a new
 * attribute to the AccountRecord.
 */
public class AccountRecordMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(AccountRecordMigrationJob.class);

  public static final String KEY = "AccountRecordMigrationJob";

  AccountRecordMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private AccountRecordMigrationJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public boolean isUiBlocking() {
    return false;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void performMigration() {
    if (!SignalStore.account().isRegistered() || SignalStore.account().getAci() == null) {
      Log.w(TAG, "Not registered!");
      return;
    }

    SignalDatabase.recipients().markNeedsSync(Recipient.self().getId());
    ApplicationDependencies.getJobManager().add(new StorageSyncJob());
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static class Factory implements Job.Factory<AccountRecordMigrationJob> {
    @Override
    public @NonNull AccountRecordMigrationJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new AccountRecordMigrationJob(parameters);
    }
  }
}
