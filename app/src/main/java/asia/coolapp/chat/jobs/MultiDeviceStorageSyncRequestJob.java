package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.crypto.UnidentifiedAccessUtil;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.net.NotPushRegisteredException;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.push.exceptions.ServerRejectedException;

public class MultiDeviceStorageSyncRequestJob extends BaseJob {

  public static final String KEY = "MultiDeviceStorageSyncRequestJob";

  private static final String TAG = Log.tag(MultiDeviceStorageSyncRequestJob.class);

  public MultiDeviceStorageSyncRequestJob() {
    this(new Parameters.Builder()
                       .setQueue("MultiDeviceStorageSyncRequestJob")
                       .setMaxInstancesForFactory(2)
                       .addConstraint(NetworkConstraint.KEY)
                       .setMaxAttempts(10)
                       .build());
  }

  private MultiDeviceStorageSyncRequestJob(@NonNull Parameters parameters) {
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
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    if (!TextSecurePreferences.isMultiDevice(context)) {
      Log.i(TAG, "Not multi device, aborting...");
      return;
    }

    SignalServiceMessageSender messageSender = ApplicationDependencies.getSignalServiceMessageSender();

    messageSender.sendSyncMessage(SignalServiceSyncMessage.forFetchLatest(SignalServiceSyncMessage.FetchType.STORAGE_MANIFEST),
                                  UnidentifiedAccessUtil.getAccessForSync(context));
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    return e instanceof PushNetworkException;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Did not succeed!");
  }

  public static final class Factory implements Job.Factory<MultiDeviceStorageSyncRequestJob> {
    @Override
    public @NonNull MultiDeviceStorageSyncRequestJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new MultiDeviceStorageSyncRequestJob(parameters);
    }
  }
}
