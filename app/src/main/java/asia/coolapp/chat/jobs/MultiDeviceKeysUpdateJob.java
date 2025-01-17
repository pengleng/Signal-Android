package asia.coolapp.chat.jobs;


import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.crypto.UnidentifiedAccessUtil;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.net.NotPushRegisteredException;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.multidevice.KeysMessage;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.push.exceptions.ServerRejectedException;
import org.whispersystems.signalservice.api.storage.StorageKey;

import java.io.IOException;
import java.util.Optional;

public class MultiDeviceKeysUpdateJob extends BaseJob {

  public static final String KEY = "MultiDeviceKeysUpdateJob";

  private static final String TAG = Log.tag(MultiDeviceKeysUpdateJob.class);

  public MultiDeviceKeysUpdateJob() {
    this(new Parameters.Builder()
                           .setQueue("MultiDeviceKeysUpdateJob")
                           .setMaxInstancesForFactory(2)
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(10)
                           .build());

  }

  private MultiDeviceKeysUpdateJob(@NonNull Parameters parameters) {
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
  public void onRun() throws IOException, UntrustedIdentityException {
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    if (!TextSecurePreferences.isMultiDevice(context)) {
      Log.i(TAG, "Not multi device, aborting...");
      return;
    }

    if (SignalStore.account().isLinkedDevice()) {
      Log.i(TAG, "Not primary device, aborting...");
      return;
    }

    SignalServiceMessageSender messageSender     = ApplicationDependencies.getSignalServiceMessageSender();
    StorageKey                 storageServiceKey = SignalStore.storageService().getOrCreateStorageKey();

    messageSender.sendSyncMessage(SignalServiceSyncMessage.forKeys(new KeysMessage(Optional.ofNullable(storageServiceKey))),
                                  UnidentifiedAccessUtil.getAccessForSync(context));
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    return e instanceof PushNetworkException;
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<MultiDeviceKeysUpdateJob> {
    @Override
    public @NonNull MultiDeviceKeysUpdateJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new MultiDeviceKeysUpdateJob(parameters);
    }
  }
}
