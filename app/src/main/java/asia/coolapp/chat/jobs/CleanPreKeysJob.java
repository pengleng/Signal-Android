package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import org.signal.libsignal.protocol.state.SignalProtocolStore;
import asia.coolapp.chat.crypto.PreKeyUtil;
import asia.coolapp.chat.crypto.storage.PreKeyMetadataStore;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.keyvalue.SignalStore;

/**
 * Deprecated. Only exists for previously-enqueued jobs. 
 * Use {@link PreKeyUtil#cleanSignedPreKeys(SignalProtocolStore, PreKeyMetadataStore)} instead.
 */
@Deprecated
public class CleanPreKeysJob extends BaseJob {

  public static final String KEY = "CleanPreKeysJob";

  private static final String TAG = Log.tag(CleanPreKeysJob.class);

  private CleanPreKeysJob(@NonNull Job.Parameters parameters) {
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
  public void onRun() {
    PreKeyUtil.cleanSignedPreKeys(ApplicationDependencies.getProtocolStore().aci(), SignalStore.account().aciPreKeys());
    PreKeyUtil.cleanSignedPreKeys(ApplicationDependencies.getProtocolStore().pni(), SignalStore.account().pniPreKeys());
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception throwable) {
    return false;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Failed to execute clean signed prekeys task.");
  }

  public static final class Factory implements Job.Factory<CleanPreKeysJob> {
    @Override
    public @NonNull CleanPreKeysJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new CleanPreKeysJob(parameters);
    }
  }
}
