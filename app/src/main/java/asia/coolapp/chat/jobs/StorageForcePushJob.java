package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import org.signal.core.util.logging.Log;
import org.signal.libsignal.protocol.InvalidKeyException;
import asia.coolapp.chat.database.RecipientDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.UnknownStorageIdDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.storage.StorageSyncHelper;
import asia.coolapp.chat.storage.StorageSyncModels;
import asia.coolapp.chat.storage.StorageSyncValidations;
import asia.coolapp.chat.transport.RetryLaterException;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.storage.SignalStorageManifest;
import org.whispersystems.signalservice.api.storage.SignalStorageRecord;
import org.whispersystems.signalservice.api.storage.StorageId;
import org.whispersystems.signalservice.api.storage.StorageKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Forces remote storage to match our local state. This should only be done when we detect that the
 * remote data is badly-encrypted (which should only happen after re-registering without a PIN).
 */
public class StorageForcePushJob extends BaseJob {

  public static final String KEY = "StorageForcePushJob";

  private static final String TAG = Log.tag(StorageForcePushJob.class);

  public StorageForcePushJob() {
    this(new Parameters.Builder().addConstraint(NetworkConstraint.KEY)
                                 .setQueue(StorageSyncJob.QUEUE_KEY)
                                 .setMaxInstancesForFactory(1)
                                 .setLifespan(TimeUnit.DAYS.toMillis(1))
                                 .build());
  }

  private StorageForcePushJob(@NonNull Parameters parameters) {
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
  protected void onRun() throws IOException, RetryLaterException {
    if (SignalStore.account().isLinkedDevice()) {
      Log.i(TAG, "Only the primary device can force push");
      return;
    }

    StorageKey                  storageServiceKey = SignalStore.storageService().getOrCreateStorageKey();
    SignalServiceAccountManager accountManager    = ApplicationDependencies.getSignalServiceAccountManager();
    RecipientDatabase           recipientDatabase = SignalDatabase.recipients();
    UnknownStorageIdDatabase    storageIdDatabase = SignalDatabase.unknownStorageIds();

    long                        currentVersion       = accountManager.getStorageManifestVersion();
    Map<RecipientId, StorageId> oldContactStorageIds = recipientDatabase.getContactStorageSyncIdsMap();

    long                        newVersion           = currentVersion + 1;
    Map<RecipientId, StorageId> newContactStorageIds = generateContactStorageIds(oldContactStorageIds);
    List<SignalStorageRecord>   inserts              = Stream.of(oldContactStorageIds.keySet())
                                                             .map(recipientDatabase::getRecordForSync)
                                                             .withoutNulls()
                                                             .map(s -> StorageSyncModels.localToRemoteRecord(s, Objects.requireNonNull(newContactStorageIds.get(s.getId())).getRaw()))
                                                             .toList();

    SignalStorageRecord accountRecord    = StorageSyncHelper.buildAccountRecord(context, Recipient.self().fresh());
    List<StorageId>     allNewStorageIds = new ArrayList<>(newContactStorageIds.values());

    inserts.add(accountRecord);
    allNewStorageIds.add(accountRecord.getId());

    SignalStorageManifest manifest = new SignalStorageManifest(newVersion, allNewStorageIds);
    StorageSyncValidations.validateForcePush(manifest, inserts, Recipient.self().fresh());

    try {
      if (newVersion > 1) {
        Log.i(TAG, String.format(Locale.ENGLISH, "Force-pushing data. Inserting %d IDs.", inserts.size()));
        if (accountManager.resetStorageRecords(storageServiceKey, manifest, inserts).isPresent()) {
          Log.w(TAG, "Hit a conflict. Trying again.");
          throw new RetryLaterException();
        }
      } else {
        Log.i(TAG, String.format(Locale.ENGLISH, "First version, normal push. Inserting %d IDs.", inserts.size()));
        if (accountManager.writeStorageRecords(storageServiceKey, manifest, inserts, Collections.emptyList()).isPresent()) {
          Log.w(TAG, "Hit a conflict. Trying again.");
          throw new RetryLaterException();
        }
      }
    } catch (InvalidKeyException e) {
      Log.w(TAG, "Hit an invalid key exception, which likely indicates a conflict.");
      throw new RetryLaterException(e);
    }

    Log.i(TAG, "Force push succeeded. Updating local manifest version to: " + newVersion);
    SignalStore.storageService().setManifest(manifest);
    recipientDatabase.applyStorageIdUpdates(newContactStorageIds);
    recipientDatabase.applyStorageIdUpdates(Collections.singletonMap(Recipient.self().getId(), accountRecord.getId()));
    storageIdDatabase.deleteAll();
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException || e instanceof RetryLaterException;
  }

  @Override
  public void onFailure() {
  }

  private static @NonNull Map<RecipientId, StorageId> generateContactStorageIds(@NonNull Map<RecipientId, StorageId> oldKeys) {
    Map<RecipientId, StorageId> out = new HashMap<>();

    for (Map.Entry<RecipientId, StorageId> entry : oldKeys.entrySet()) {
      out.put(entry.getKey(), entry.getValue().withNewBytes(StorageSyncHelper.generateKey()));
    }

    return out;
  }

  public static final class Factory implements Job.Factory<StorageForcePushJob> {
    @Override
    public @NonNull StorageForcePushJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new StorageForcePushJob(parameters);
    }
  }
}
