package asia.coolapp.chat.registration;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.signal.core.util.logging.Log;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.SignalProtocolStore;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.util.KeyHelper;
import org.signal.libsignal.zkgroup.profiles.ProfileKey;
import asia.coolapp.chat.crypto.PreKeyUtil;
import asia.coolapp.chat.crypto.ProfileKeyUtil;
import asia.coolapp.chat.crypto.SenderKeyUtil;
import asia.coolapp.chat.crypto.storage.PreKeyMetadataStore;
import asia.coolapp.chat.crypto.storage.SignalServiceAccountDataStoreImpl;
import asia.coolapp.chat.database.IdentityDatabase;
import asia.coolapp.chat.database.RecipientDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.JobManager;
import asia.coolapp.chat.jobs.DirectoryRefreshJob;
import asia.coolapp.chat.jobs.RotateCertificateJob;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.pin.PinState;
import asia.coolapp.chat.push.AccountManagerFactory;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.registration.VerifyAccountRepository.VerifyAccountWithRegistrationLockResponse;
import asia.coolapp.chat.service.DirectoryRefreshListener;
import asia.coolapp.chat.service.RotateSignedPreKeyListener;
import asia.coolapp.chat.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.KbsPinData;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.ACI;
import org.whispersystems.signalservice.api.push.PNI;
import org.whispersystems.signalservice.api.push.ServiceIdType;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.internal.ServiceResponse;
import org.whispersystems.signalservice.internal.push.VerifyAccountResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Operations required for finalizing the registration of an account. This is
 * to be used after verifying the code and registration lock (if necessary) with
 * the server and being issued a UUID.
 */
public final class RegistrationRepository {

  private static final String TAG = Log.tag(RegistrationRepository.class);

  private final Application context;

  public RegistrationRepository(@NonNull Application context) {
    this.context = context;
  }

  public int getRegistrationId() {
    int registrationId = SignalStore.account().getRegistrationId();
    if (registrationId == 0) {
      registrationId = KeyHelper.generateRegistrationId(false);
      SignalStore.account().setRegistrationId(registrationId);
    }
    return registrationId;
  }

  public @NonNull ProfileKey getProfileKey(@NonNull String e164) {
    ProfileKey profileKey = findExistingProfileKey(e164);

    if (profileKey == null) {
      profileKey = ProfileKeyUtil.createNew();
      Log.i(TAG, "No profile key found, created a new one");
    }

    return profileKey;
  }

  public Single<ServiceResponse<VerifyAccountResponse>> registerAccountWithoutRegistrationLock(@NonNull RegistrationData registrationData,
                                                                                               @NonNull VerifyAccountResponse response)
  {
    return registerAccount(registrationData, response, null, null);
  }

  public Single<ServiceResponse<VerifyAccountResponse>> registerAccountWithRegistrationLock(@NonNull RegistrationData registrationData,
                                                                                            @NonNull VerifyAccountWithRegistrationLockResponse response,
                                                                                            @NonNull String pin)
  {
    return registerAccount(registrationData, response.getVerifyAccountResponse(), pin, response.getKbsData());
  }

  private Single<ServiceResponse<VerifyAccountResponse>> registerAccount(@NonNull RegistrationData registrationData,
                                                                         @NonNull VerifyAccountResponse response,
                                                                         @Nullable String pin,
                                                                         @Nullable KbsPinData kbsData)
  {
    return Single.<ServiceResponse<VerifyAccountResponse>>fromCallable(() -> {
      try {
        registerAccountInternal(registrationData, response, pin, kbsData);

        JobManager jobManager = ApplicationDependencies.getJobManager();
        jobManager.add(new DirectoryRefreshJob(false));
        jobManager.add(new RotateCertificateJob());

        DirectoryRefreshListener.schedule(context);
        RotateSignedPreKeyListener.schedule(context);

        return ServiceResponse.forResult(response, 200, null);
      } catch (IOException e) {
        return ServiceResponse.forUnknownError(e);
      }
    }).subscribeOn(Schedulers.io());
  }

  @WorkerThread
  private void registerAccountInternal(@NonNull RegistrationData registrationData,
                                       @NonNull VerifyAccountResponse response,
                                       @Nullable String pin,
                                       @Nullable KbsPinData kbsData)
      throws IOException
  {
    ACI     aci    = ACI.parseOrThrow(response.getUuid());
    PNI     pni    = PNI.parseOrThrow(response.getPni());
    boolean hasPin = response.isStorageCapable();

    SignalStore.account().setAci(aci);
    SignalStore.account().setPni(pni);

    ApplicationDependencies.getProtocolStore().aci().sessions().archiveAllSessions();
    ApplicationDependencies.getProtocolStore().pni().sessions().archiveAllSessions();
    SenderKeyUtil.clearAllState();

    SignalServiceAccountManager       accountManager   = AccountManagerFactory.createAuthenticated(context, aci, pni, registrationData.getE164(), SignalServiceAddress.DEFAULT_DEVICE_ID, registrationData.getPassword());
    SignalServiceAccountDataStoreImpl aciProtocolStore = ApplicationDependencies.getProtocolStore().aci();
    SignalServiceAccountDataStoreImpl pniProtocolStore = ApplicationDependencies.getProtocolStore().pni();

    generateAndRegisterPreKeys(ServiceIdType.ACI, accountManager, aciProtocolStore, SignalStore.account().aciPreKeys());
    generateAndRegisterPreKeys(ServiceIdType.PNI, accountManager, pniProtocolStore, SignalStore.account().pniPreKeys());

    if (registrationData.isFcm()) {
      accountManager.setGcmId(Optional.ofNullable(registrationData.getFcmToken()));
    }

    RecipientDatabase recipientDatabase = SignalDatabase.recipients();
    RecipientId       selfId            = Recipient.externalPush(aci, registrationData.getE164(), true).getId();

    recipientDatabase.setProfileSharing(selfId, true);
    recipientDatabase.markRegisteredOrThrow(selfId, aci);
    recipientDatabase.setPni(selfId, pni);
    recipientDatabase.setProfileKey(selfId, registrationData.getProfileKey());

    ApplicationDependencies.getRecipientCache().clearSelf();

    SignalStore.account().setE164(registrationData.getE164());
    SignalStore.account().setFcmToken(registrationData.getFcmToken());
    SignalStore.account().setFcmEnabled(registrationData.isFcm());

    long now = System.currentTimeMillis();
    saveOwnIdentityKey(selfId, aciProtocolStore, now);
    saveOwnIdentityKey(selfId, pniProtocolStore, now);

    SignalStore.account().setServicePassword(registrationData.getPassword());
    SignalStore.account().setRegistered(true);
    TextSecurePreferences.setPromptedPushRegistration(context, true);
    TextSecurePreferences.setUnauthorizedReceived(context, false);

    PinState.onRegistration(context, kbsData, pin, hasPin);
  }

  private void generateAndRegisterPreKeys(@NonNull ServiceIdType serviceIdType,
                                          @NonNull SignalServiceAccountManager accountManager,
                                          @NonNull SignalProtocolStore protocolStore,
                                          @NonNull PreKeyMetadataStore metadataStore)
      throws IOException
  {
    SignedPreKeyRecord signedPreKey   = PreKeyUtil.generateAndStoreSignedPreKey(protocolStore, metadataStore, true);
    List<PreKeyRecord> oneTimePreKeys = PreKeyUtil.generateAndStoreOneTimePreKeys(protocolStore, metadataStore);

    accountManager.setPreKeys(serviceIdType, protocolStore.getIdentityKeyPair().getPublicKey(), signedPreKey, oneTimePreKeys);
    metadataStore.setSignedPreKeyRegistered(true);
  }

  private void saveOwnIdentityKey(@NonNull RecipientId selfId, @NonNull SignalServiceAccountDataStoreImpl protocolStore, long now) {
    protocolStore.identities().saveIdentityWithoutSideEffects(selfId,
                                                              protocolStore.getIdentityKeyPair().getPublicKey(),
                                                              IdentityDatabase.VerifiedStatus.VERIFIED,
                                                              true,
                                                              now,
                                                              true);
  }

  @WorkerThread
  private static @Nullable ProfileKey findExistingProfileKey(@NonNull String e164number) {
    RecipientDatabase     recipientDatabase = SignalDatabase.recipients();
    Optional<RecipientId> recipient         = recipientDatabase.getByE164(e164number);

    if (recipient.isPresent()) {
      return ProfileKeyUtil.profileKeyOrNull(Recipient.resolved(recipient.get()).getProfileKey());
    }

    return null;
  }
}
