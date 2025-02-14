package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.KbsEnclave;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.JobManager;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.pin.KbsEnclaves;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedResponseException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Clears data from an old KBS enclave.
 */
public class ClearFallbackKbsEnclaveJob extends BaseJob {

  public static final String KEY = "ClearFallbackKbsEnclaveJob";

  private static final String TAG = Log.tag(ClearFallbackKbsEnclaveJob.class);

  private static final String KEY_ENCLAVE_NAME = "enclaveName";
  private static final String KEY_SERVICE_ID   = "serviceId";
  private static final String KEY_MR_ENCLAVE   = "mrEnclave";

  private final KbsEnclave enclave;

  ClearFallbackKbsEnclaveJob(@NonNull KbsEnclave enclave) {
    this(new Parameters.Builder()
                       .addConstraint(NetworkConstraint.KEY)
                       .setLifespan(TimeUnit.DAYS.toMillis(90))
                       .setMaxAttempts(Parameters.UNLIMITED)
                       .setQueue("ClearFallbackKbsEnclaveJob")
                       .build(),
        enclave);
  }

  public static void clearAll() {
    if (KbsEnclaves.fallbacks().isEmpty()) {
      Log.i(TAG, "No fallbacks!");
      return;
    }

    JobManager jobManager = ApplicationDependencies.getJobManager();

    for (KbsEnclave enclave : KbsEnclaves.fallbacks()) {
      jobManager.add(new ClearFallbackKbsEnclaveJob(enclave));
    }
  }

  private ClearFallbackKbsEnclaveJob(@NonNull Parameters parameters, @NonNull KbsEnclave enclave) {
    super(parameters);
    this.enclave = enclave;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public @NonNull Data serialize() {
    return new Data.Builder().putString(KEY_ENCLAVE_NAME, enclave.getEnclaveName())
                             .putString(KEY_SERVICE_ID, enclave.getServiceId())
                             .putString(KEY_MR_ENCLAVE, enclave.getMrEnclave())
                             .build();
  }

  @Override
  public void onRun() throws IOException, UnauthenticatedResponseException {
    Log.i(TAG, "Preparing to delete data from " + enclave.getEnclaveName());
    ApplicationDependencies.getKeyBackupService(enclave).newPinChangeSession().removePin();
    Log.i(TAG, "Successfully deleted the data from " + enclave.getEnclaveName());
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return true;
  }

  @Override
  public void onFailure() {
    throw new AssertionError("This job should never fail. " + getClass().getSimpleName());
  }

  public static class Factory implements Job.Factory<ClearFallbackKbsEnclaveJob> {
    @Override
    public @NonNull ClearFallbackKbsEnclaveJob create(@NonNull Parameters parameters, @NonNull Data data) {
      KbsEnclave enclave = new KbsEnclave(data.getString(KEY_ENCLAVE_NAME),
                                          data.getString(KEY_SERVICE_ID),
                                          data.getString(KEY_MR_ENCLAVE));

      return new ClearFallbackKbsEnclaveJob(parameters, enclave);
    }
  }
}
