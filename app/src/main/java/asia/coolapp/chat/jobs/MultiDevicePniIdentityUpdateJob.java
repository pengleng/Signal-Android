package asia.coolapp.chat.jobs;


import androidx.annotation.NonNull;

import com.google.protobuf.ByteString;

import org.signal.core.util.logging.Log;
import org.signal.libsignal.protocol.IdentityKeyPair;
import asia.coolapp.chat.crypto.UnidentifiedAccessUtil;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.net.NotPushRegisteredException;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.push.exceptions.ServerRejectedException;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.SyncMessage.PniIdentity;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * As part of the PNI migration, linked devices will need to be told what their PNI identity key is. This job is sent either in response to a request from
 * a linked device or as part of a migration when we start using PNIs.
 */
public class MultiDevicePniIdentityUpdateJob extends BaseJob {

  private static final String TAG = Log.tag(MultiDevicePniIdentityUpdateJob.class);

  public static final String KEY = "MultiDevicePniIdentityUpdateJob";

  public MultiDevicePniIdentityUpdateJob() {
    this(new Parameters.Builder()
                       .setQueue("__MULTI_DEVICE_PNI_IDENTITY_UPDATE_JOB__")
                       .setMaxInstancesForFactory(1)
                       .addConstraint(NetworkConstraint.KEY)
                       .setLifespan(TimeUnit.DAYS.toMillis(1))
                       .setMaxAttempts(Parameters.UNLIMITED)
                       .build());
  }

  private MultiDevicePniIdentityUpdateJob(@NonNull Parameters parameters) {
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

    IdentityKeyPair            pniIdentityKeyPair = SignalStore.account().getPniIdentityKey();
    SignalServiceSyncMessage   syncMessage        = SignalServiceSyncMessage.forPniIdentity(PniIdentity.newBuilder()
                                                                                                       .setPublicKey(ByteString.copyFrom(pniIdentityKeyPair.getPublicKey().serialize()))
                                                                                                       .setPrivateKey(ByteString.copyFrom(pniIdentityKeyPair.getPrivateKey().serialize()))
                                                                                                       .build());

    ApplicationDependencies.getSignalServiceMessageSender().sendSyncMessage(syncMessage, UnidentifiedAccessUtil.getAccessForSync(context));
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    return e instanceof PushNetworkException;
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<MultiDevicePniIdentityUpdateJob> {
    @Override
    public @NonNull MultiDevicePniIdentityUpdateJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new MultiDevicePniIdentityUpdateJob(parameters);
    }
  }
}
