package asia.coolapp.chat.dependencies;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import org.signal.core.util.concurrent.DeadlockDetector;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.libsignal.zkgroup.receipts.ClientZkReceiptOperations;
import asia.coolapp.chat.BuildConfig;
import asia.coolapp.chat.components.TypingStatusRepository;
import asia.coolapp.chat.components.TypingStatusSender;
import asia.coolapp.chat.crypto.ReentrantSessionLock;
import asia.coolapp.chat.crypto.storage.SignalBaseIdentityKeyStore;
import asia.coolapp.chat.crypto.storage.SignalIdentityKeyStore;
import asia.coolapp.chat.crypto.storage.SignalSenderKeyStore;
import asia.coolapp.chat.crypto.storage.SignalServiceAccountDataStoreImpl;
import asia.coolapp.chat.crypto.storage.SignalServiceDataStoreImpl;
import asia.coolapp.chat.crypto.storage.TextSecurePreKeyStore;
import asia.coolapp.chat.crypto.storage.TextSecureSessionStore;
import asia.coolapp.chat.database.DatabaseObserver;
import asia.coolapp.chat.database.JobDatabase;
import asia.coolapp.chat.database.PendingRetryReceiptCache;
import asia.coolapp.chat.jobmanager.JobManager;
import asia.coolapp.chat.jobmanager.JobMigrator;
import asia.coolapp.chat.jobmanager.impl.FactoryJobPredicate;
import asia.coolapp.chat.jobmanager.impl.JsonDataSerializer;
import asia.coolapp.chat.jobs.CreateSignedPreKeyJob;
import asia.coolapp.chat.jobs.FastJobStorage;
import asia.coolapp.chat.jobs.GroupCallUpdateSendJob;
import asia.coolapp.chat.jobs.JobManagerFactories;
import asia.coolapp.chat.jobs.MarkerJob;
import asia.coolapp.chat.jobs.PushDecryptMessageJob;
import asia.coolapp.chat.jobs.PushGroupSendJob;
import asia.coolapp.chat.jobs.PushMediaSendJob;
import asia.coolapp.chat.jobs.PushProcessMessageJob;
import asia.coolapp.chat.jobs.PushTextSendJob;
import asia.coolapp.chat.jobs.ReactionSendJob;
import asia.coolapp.chat.jobs.TypingSendJob;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.megaphone.MegaphoneRepository;
import asia.coolapp.chat.messages.BackgroundMessageRetriever;
import asia.coolapp.chat.messages.IncomingMessageObserver;
import asia.coolapp.chat.messages.IncomingMessageProcessor;
import asia.coolapp.chat.net.SignalWebSocketHealthMonitor;
import asia.coolapp.chat.notifications.MessageNotifier;
import asia.coolapp.chat.notifications.OptimizedMessageNotifier;
import asia.coolapp.chat.payments.MobileCoinConfig;
import asia.coolapp.chat.payments.Payments;
import asia.coolapp.chat.push.SecurityEventListener;
import asia.coolapp.chat.push.SignalServiceNetworkAccess;
import asia.coolapp.chat.recipients.LiveRecipientCache;
import asia.coolapp.chat.revealable.ViewOnceMessageManager;
import asia.coolapp.chat.service.ExpiringMessageManager;
import asia.coolapp.chat.service.ExpiringStoriesManager;
import asia.coolapp.chat.service.PendingRetryReceiptManager;
import asia.coolapp.chat.service.TrimThreadsByDateManager;
import asia.coolapp.chat.service.webrtc.SignalCallManager;
import asia.coolapp.chat.shakereport.ShakeToReport;
import asia.coolapp.chat.util.AlarmSleepTimer;
import asia.coolapp.chat.util.AppForegroundObserver;
import asia.coolapp.chat.util.ByteUnit;
import asia.coolapp.chat.util.EarlyMessageCache;
import asia.coolapp.chat.util.FeatureFlags;
import asia.coolapp.chat.util.FrameRateTracker;
import asia.coolapp.chat.util.TextSecurePreferences;
import asia.coolapp.chat.video.exo.GiphyMp4Cache;
import asia.coolapp.chat.video.exo.SimpleExoPlayerPool;
import asia.coolapp.chat.webrtc.audio.AudioManagerCompat;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceDataStore;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.SignalWebSocket;
import org.whispersystems.signalservice.api.groupsv2.ClientZkOperations;
import org.whispersystems.signalservice.api.groupsv2.GroupsV2Operations;
import org.whispersystems.signalservice.api.push.ACI;
import org.whispersystems.signalservice.api.push.PNI;
import org.whispersystems.signalservice.api.services.DonationsService;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.util.SleepTimer;
import org.whispersystems.signalservice.api.util.UptimeSleepTimer;
import org.whispersystems.signalservice.api.websocket.WebSocketFactory;
import org.whispersystems.signalservice.internal.websocket.WebSocketConnection;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link ApplicationDependencies.Provider} that provides real app dependencies.
 */
public class ApplicationDependencyProvider implements ApplicationDependencies.Provider {

  private final Application context;

  public ApplicationDependencyProvider(@NonNull Application context) {
    this.context = context;
  }

  private @NonNull ClientZkOperations provideClientZkOperations() {
    return ClientZkOperations.create(provideSignalServiceNetworkAccess().getConfiguration());
  }

  @Override
  public @NonNull GroupsV2Operations provideGroupsV2Operations() {
    return new GroupsV2Operations(provideClientZkOperations(), FeatureFlags.groupLimits().getHardLimit());
  }

  @Override
  public @NonNull SignalServiceAccountManager provideSignalServiceAccountManager() {
    return new SignalServiceAccountManager(provideSignalServiceNetworkAccess().getConfiguration(),
                                           new DynamicCredentialsProvider(),
                                           BuildConfig.SIGNAL_AGENT,
                                           provideGroupsV2Operations(),
                                           FeatureFlags.okHttpAutomaticRetry());
  }

  @Override
  public @NonNull SignalServiceMessageSender provideSignalServiceMessageSender(@NonNull SignalWebSocket signalWebSocket, @NonNull SignalServiceDataStore protocolStore) {
      return new SignalServiceMessageSender(provideSignalServiceNetworkAccess().getConfiguration(),
                                            new DynamicCredentialsProvider(),
                                            protocolStore,
                                            ReentrantSessionLock.INSTANCE,
                                            BuildConfig.SIGNAL_AGENT,
                                            signalWebSocket,
                                            Optional.of(new SecurityEventListener(context)),
                                            provideClientZkOperations().getProfileOperations(),
                                            SignalExecutors.newCachedBoundedExecutor("signal-messages", 1, 16, 30),
                                            ByteUnit.KILOBYTES.toBytes(256),
                                            FeatureFlags.okHttpAutomaticRetry());
  }

  @Override
  public @NonNull SignalServiceMessageReceiver provideSignalServiceMessageReceiver() {
    return new SignalServiceMessageReceiver(provideSignalServiceNetworkAccess().getConfiguration(),
                                            new DynamicCredentialsProvider(),
                                            BuildConfig.SIGNAL_AGENT,
                                            provideClientZkOperations().getProfileOperations(),
                                            FeatureFlags.okHttpAutomaticRetry());
  }

  @Override
  public @NonNull SignalServiceNetworkAccess provideSignalServiceNetworkAccess() {
    return new SignalServiceNetworkAccess(context);
  }

  @Override
  public @NonNull IncomingMessageProcessor provideIncomingMessageProcessor() {
    return new IncomingMessageProcessor(context);
  }

  @Override
  public @NonNull BackgroundMessageRetriever provideBackgroundMessageRetriever() {
    return new BackgroundMessageRetriever();
  }

  @Override
  public @NonNull LiveRecipientCache provideRecipientCache() {
    return new LiveRecipientCache(context);
  }

  @Override
  public @NonNull JobManager provideJobManager() {
    JobManager.Configuration config = new JobManager.Configuration.Builder()
                                                                  .setDataSerializer(new JsonDataSerializer())
                                                                  .setJobFactories(JobManagerFactories.getJobFactories(context))
                                                                  .setConstraintFactories(JobManagerFactories.getConstraintFactories(context))
                                                                  .setConstraintObservers(JobManagerFactories.getConstraintObservers(context))
                                                                  .setJobStorage(new FastJobStorage(JobDatabase.getInstance(context)))
                                                                  .setJobMigrator(new JobMigrator(TextSecurePreferences.getJobManagerVersion(context), JobManager.CURRENT_VERSION, JobManagerFactories.getJobMigrations(context)))
                                                                  .addReservedJobRunner(new FactoryJobPredicate(PushDecryptMessageJob.KEY, PushProcessMessageJob.KEY, MarkerJob.KEY))
                                                                  .addReservedJobRunner(new FactoryJobPredicate(PushTextSendJob.KEY, PushMediaSendJob.KEY, PushGroupSendJob.KEY, ReactionSendJob.KEY, TypingSendJob.KEY, GroupCallUpdateSendJob.KEY))
                                                                  .build();
    return new JobManager(context, config);
  }

  @Override
  public @NonNull FrameRateTracker provideFrameRateTracker() {
    return new FrameRateTracker(context);
  }

  public @NonNull MegaphoneRepository provideMegaphoneRepository() {
    return new MegaphoneRepository(context);
  }

  @Override
  public @NonNull EarlyMessageCache provideEarlyMessageCache() {
    return new EarlyMessageCache();
  }

  @Override
  public @NonNull MessageNotifier provideMessageNotifier() {
    return new OptimizedMessageNotifier(context);
  }

  @Override
  public @NonNull IncomingMessageObserver provideIncomingMessageObserver() {
    return new IncomingMessageObserver(context);
  }

  @Override
  public @NonNull TrimThreadsByDateManager provideTrimThreadsByDateManager() {
    return new TrimThreadsByDateManager(context);
  }

  @Override
  public @NonNull ViewOnceMessageManager provideViewOnceMessageManager() {
    return new ViewOnceMessageManager(context);
  }

  @Override
  public @NonNull ExpiringStoriesManager provideExpiringStoriesManager() {
    return new ExpiringStoriesManager(context);
  }

  @Override
  public @NonNull ExpiringMessageManager provideExpiringMessageManager() {
    return new ExpiringMessageManager(context);
  }

  @Override
  public @NonNull TypingStatusRepository provideTypingStatusRepository() {
    return new TypingStatusRepository();
  }

  @Override
  public @NonNull TypingStatusSender provideTypingStatusSender() {
    return new TypingStatusSender();
  }

  @Override
  public @NonNull DatabaseObserver provideDatabaseObserver() {
    return new DatabaseObserver(context);
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public @NonNull Payments providePayments(@NonNull SignalServiceAccountManager signalServiceAccountManager) {
    MobileCoinConfig network;

    if      (BuildConfig.MOBILE_COIN_ENVIRONMENT.equals("mainnet")) network = MobileCoinConfig.getMainNet(signalServiceAccountManager);
    else if (BuildConfig.MOBILE_COIN_ENVIRONMENT.equals("testnet")) network = MobileCoinConfig.getTestNet(signalServiceAccountManager);
    else throw new AssertionError("Unknown network " + BuildConfig.MOBILE_COIN_ENVIRONMENT);

    return new Payments(network);
  }

  @Override
  public @NonNull ShakeToReport provideShakeToReport() {
    return new ShakeToReport(context);
  }

  @Override
  public @NonNull AppForegroundObserver provideAppForegroundObserver() {
    return new AppForegroundObserver();
  }

  @Override
  public @NonNull SignalCallManager provideSignalCallManager() {
    return new SignalCallManager(context);
  }

  @Override
  public @NonNull PendingRetryReceiptManager providePendingRetryReceiptManager() {
    return new PendingRetryReceiptManager(context);
  }

  @Override
  public @NonNull PendingRetryReceiptCache providePendingRetryReceiptCache() {
    return new PendingRetryReceiptCache(context);
  }

  @Override
  public @NonNull SignalWebSocket provideSignalWebSocket() {
    SleepTimer                   sleepTimer      = SignalStore.account().isFcmEnabled() ? new UptimeSleepTimer() : new AlarmSleepTimer(context);
    SignalWebSocketHealthMonitor healthMonitor   = new SignalWebSocketHealthMonitor(context, sleepTimer);
    SignalWebSocket              signalWebSocket = new SignalWebSocket(provideWebSocketFactory(healthMonitor));

    healthMonitor.monitor(signalWebSocket);

    return signalWebSocket;
  }

  @Override
  public @NonNull SignalServiceDataStoreImpl provideProtocolStore() {
    ACI localAci = SignalStore.account().getAci();
    PNI localPni = SignalStore.account().getPni();

    if (localAci == null) {
      throw new IllegalStateException("No ACI set!");
    }

    if (localPni == null) {
      throw new IllegalStateException("No PNI set!");
    }

    boolean needsPreKeyJob = false;

    if (!SignalStore.account().hasAciIdentityKey()) {
      SignalStore.account().generateAciIdentityKeyIfNecessary();
      needsPreKeyJob = true;
    }

    if (!SignalStore.account().hasPniIdentityKey()) {
      SignalStore.account().generatePniIdentityKeyIfNecessary();
      needsPreKeyJob = true;
    }

    if (needsPreKeyJob) {
      CreateSignedPreKeyJob.enqueueIfNeeded();
    }

    SignalBaseIdentityKeyStore baseIdentityStore = new SignalBaseIdentityKeyStore(context);

    SignalServiceAccountDataStoreImpl aciStore = new SignalServiceAccountDataStoreImpl(context,
                                                                                       new TextSecurePreKeyStore(localAci),
                                                                                       new SignalIdentityKeyStore(baseIdentityStore, () -> SignalStore.account().getAciIdentityKey()),
                                                                                       new TextSecureSessionStore(localAci),
                                                                                       new SignalSenderKeyStore(context));

    SignalServiceAccountDataStoreImpl pniStore = new SignalServiceAccountDataStoreImpl(context,
                                                                                       new TextSecurePreKeyStore(localPni),
                                                                                       new SignalIdentityKeyStore(baseIdentityStore, () -> SignalStore.account().getPniIdentityKey()),
                                                                                       new TextSecureSessionStore(localPni),
                                                                                       new SignalSenderKeyStore(context));
    return new SignalServiceDataStoreImpl(context, aciStore, pniStore);
  }

  @Override
  public @NonNull GiphyMp4Cache provideGiphyMp4Cache() {
    return new GiphyMp4Cache(ByteUnit.MEGABYTES.toBytes(16));
  }

  @Override
  public @NonNull SimpleExoPlayerPool provideExoPlayerPool() {
    return new SimpleExoPlayerPool(context);
  }

  @Override
  public @NonNull AudioManagerCompat provideAndroidCallAudioManager() {
    return AudioManagerCompat.create(context);
  }

  @Override
  public @NonNull DonationsService provideDonationsService() {
    return new DonationsService(provideSignalServiceNetworkAccess().getConfiguration(),
                                new DynamicCredentialsProvider(),
                                BuildConfig.SIGNAL_AGENT,
                                provideGroupsV2Operations(),
                                FeatureFlags.okHttpAutomaticRetry());
  }

  @Override
  public @NonNull DeadlockDetector provideDeadlockDetector() {
    HandlerThread handlerThread = new HandlerThread("signal-DeadlockDetector");
    handlerThread.start();
    return new DeadlockDetector(new Handler(handlerThread.getLooper()), TimeUnit.SECONDS.toMillis(5));
  }

  @Override
  public @NonNull ClientZkReceiptOperations provideClientZkReceiptOperations() {
    return provideClientZkOperations().getReceiptOperations();
  }

  private @NonNull WebSocketFactory provideWebSocketFactory(@NonNull SignalWebSocketHealthMonitor healthMonitor) {
    return new WebSocketFactory() {
      @Override
      public WebSocketConnection createWebSocket() {
        return new WebSocketConnection("normal",
                                       provideSignalServiceNetworkAccess().getConfiguration(),
                                       Optional.of(new DynamicCredentialsProvider()),
                                       BuildConfig.SIGNAL_AGENT,
                                       healthMonitor);
      }

      @Override
      public WebSocketConnection createUnidentifiedWebSocket() {
        return new WebSocketConnection("unidentified",
                                       provideSignalServiceNetworkAccess().getConfiguration(),
                                       Optional.empty(),
                                       BuildConfig.SIGNAL_AGENT,
                                       healthMonitor);
      }
    };
  }

  private static class DynamicCredentialsProvider implements CredentialsProvider {

    @Override
    public ACI getAci() {
      return SignalStore.account().getAci();
    }

    @Override
    public PNI getPni() {
      return SignalStore.account().getPni();
    }

    @Override
    public String getE164() {
      return SignalStore.account().getE164();
    }

    @Override
    public String getPassword() {
      return SignalStore.account().getServicePassword();
    }

    @Override
    public int getDeviceId() {
      return SignalStore.account().getDeviceId();
    }
  }
}
