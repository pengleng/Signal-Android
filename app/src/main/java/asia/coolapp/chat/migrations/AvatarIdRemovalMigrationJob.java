package asia.coolapp.chat.migrations;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobs.RefreshOwnProfileJob;

/**
 * We just want to make sure that the user has a profile avatar set in the RecipientDatabase, so
 * we're refreshing their own profile.
 */
public class AvatarIdRemovalMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(AvatarIdRemovalMigrationJob.class);

  public static final String KEY = "AvatarIdRemovalMigrationJob";

  AvatarIdRemovalMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private AvatarIdRemovalMigrationJob(@NonNull Parameters parameters) {
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
    ApplicationDependencies.getJobManager().add(new RefreshOwnProfileJob());
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static class Factory implements Job.Factory<AvatarIdRemovalMigrationJob> {
    @Override
    public @NonNull AvatarIdRemovalMigrationJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new AvatarIdRemovalMigrationJob(parameters);
    }
  }
}
