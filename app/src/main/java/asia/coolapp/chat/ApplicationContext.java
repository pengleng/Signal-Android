/*
 * Copyright (C) 2013 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package asia.coolapp.chat;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.security.ProviderInstaller;

import org.conscrypt.Conscrypt;
import org.greenrobot.eventbus.EventBus;
import org.signal.aesgcmprovider.AesGcmProvider;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.AndroidLogger;
import org.signal.core.util.logging.Log;
import org.signal.core.util.tracing.Tracer;
import org.signal.glide.SignalGlideCodecs;
import org.signal.libsignal.protocol.logging.SignalProtocolLoggerProvider;
import org.signal.ringrtc.CallManager;
import asia.coolapp.chat.avatar.AvatarPickerStorage;
import asia.coolapp.chat.crypto.AttachmentSecretProvider;
import asia.coolapp.chat.crypto.DatabaseSecretProvider;
import asia.coolapp.chat.database.LogDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.SqlCipherLibraryLoader;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.dependencies.ApplicationDependencyProvider;
import asia.coolapp.chat.emoji.EmojiSource;
import asia.coolapp.chat.emoji.JumboEmoji;
import asia.coolapp.chat.gcm.FcmJobService;
import asia.coolapp.chat.jobs.CheckServiceReachabilityJob;
import asia.coolapp.chat.jobs.CreateSignedPreKeyJob;
import asia.coolapp.chat.jobs.DownloadLatestEmojiDataJob;
import asia.coolapp.chat.jobs.EmojiSearchIndexDownloadJob;
import asia.coolapp.chat.jobs.FcmRefreshJob;
import asia.coolapp.chat.jobs.FontDownloaderJob;
import asia.coolapp.chat.jobs.GroupV2UpdateSelfProfileKeyJob;
import asia.coolapp.chat.jobs.MultiDeviceContactUpdateJob;
import asia.coolapp.chat.jobs.ProfileUploadJob;
import asia.coolapp.chat.jobs.PushNotificationReceiveJob;
import asia.coolapp.chat.jobs.RefreshPreKeysJob;
import asia.coolapp.chat.jobs.RetrieveProfileJob;
import asia.coolapp.chat.jobs.RetrieveReleaseChannelJob;
import asia.coolapp.chat.jobs.SubscriptionKeepAliveJob;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.logging.CustomSignalProtocolLogger;
import asia.coolapp.chat.logging.PersistentLogger;
import asia.coolapp.chat.messageprocessingalarm.MessageProcessReceiver;
import asia.coolapp.chat.migrations.ApplicationMigrations;
import asia.coolapp.chat.mms.SignalGlideComponents;
import asia.coolapp.chat.mms.SignalGlideModule;
import asia.coolapp.chat.notifications.NotificationChannels;
import asia.coolapp.chat.providers.BlobProvider;
import asia.coolapp.chat.ratelimit.RateLimitUtil;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.registration.RegistrationUtil;
import asia.coolapp.chat.ringrtc.RingRtcLogger;
import asia.coolapp.chat.service.DirectoryRefreshListener;
import asia.coolapp.chat.service.KeyCachingService;
import asia.coolapp.chat.service.LocalBackupListener;
import asia.coolapp.chat.service.RotateSenderCertificateListener;
import asia.coolapp.chat.service.RotateSignedPreKeyListener;
import asia.coolapp.chat.service.UpdateApkRefreshListener;
import asia.coolapp.chat.service.webrtc.AndroidTelecomUtil;
import asia.coolapp.chat.storage.StorageSyncHelper;
import asia.coolapp.chat.util.AppForegroundObserver;
import asia.coolapp.chat.util.AppStartup;
import asia.coolapp.chat.util.DynamicTheme;
import asia.coolapp.chat.util.FeatureFlags;
import asia.coolapp.chat.util.SignalLocalMetrics;
import asia.coolapp.chat.util.SignalUncaughtExceptionHandler;
import asia.coolapp.chat.util.TextSecurePreferences;
import asia.coolapp.chat.util.Util;
import asia.coolapp.chat.util.VersionTracker;
import asia.coolapp.chat.util.dynamiclanguage.DynamicLanguageContextWrapper;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import rxdogtag2.RxDogTag;

/**
 * Will be called once when the TextSecure process is created.
 *
 * We're using this as an insertion point to patch up the Android PRNG disaster,
 * to initialize the job manager, and to check for GCM registration freshness.
 *
 * @author Moxie Marlinspike
 */
public class ApplicationContext extends MultiDexApplication implements AppForegroundObserver.Listener {

  private static final String TAG = Log.tag(ApplicationContext.class);

  private PersistentLogger persistentLogger;

  public static ApplicationContext getInstance(Context context) {
    return (ApplicationContext)context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    Tracer.getInstance().start("Application#onCreate()");
    AppStartup.getInstance().onApplicationCreate();
    SignalLocalMetrics.ColdStart.start();

    long startTime = System.currentTimeMillis();

    if (FeatureFlags.internalUser()) {
      Tracer.getInstance().setMaxBufferSize(35_000);
    }

    super.onCreate();

    AppStartup.getInstance().addBlocking("security-provider", this::initializeSecurityProvider)
                            .addBlocking("sqlcipher-init", () -> {
                              SqlCipherLibraryLoader.load();
                              SignalDatabase.init(this,
                                                  DatabaseSecretProvider.getOrCreateDatabaseSecret(this),
                                                  AttachmentSecretProvider.getInstance(this).getOrCreateAttachmentSecret());
                            })
                            .addBlocking("logging", () -> {
                              initializeLogging();
                              Log.i(TAG, "onCreate()");
                            })
                            .addBlocking("crash-handling", this::initializeCrashHandling)
                            .addBlocking("rx-init", this::initializeRx)
                            .addBlocking("event-bus", () -> EventBus.builder().logNoSubscriberMessages(false).installDefaultEventBus())
                            .addBlocking("app-dependencies", this::initializeAppDependencies)
                            .addBlocking("notification-channels", () -> NotificationChannels.create(this))
                            .addBlocking("first-launch", this::initializeFirstEverAppLaunch)
                            .addBlocking("app-migrations", this::initializeApplicationMigrations)
                            .addBlocking("ring-rtc", this::initializeRingRtc)
                            .addBlocking("mark-registration", () -> RegistrationUtil.maybeMarkRegistrationComplete(this))
                            .addBlocking("lifecycle-observer", () -> ApplicationDependencies.getAppForegroundObserver().addListener(this))
                            .addBlocking("message-retriever", this::initializeMessageRetrieval)
                            .addBlocking("dynamic-theme", () -> DynamicTheme.setDefaultDayNightMode(this))
                            .addBlocking("vector-compat", () -> {
                              if (Build.VERSION.SDK_INT < 21) {
                                AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
                              }
                            })
                            .addBlocking("proxy-init", () -> {
                              if (SignalStore.proxy().isProxyEnabled()) {
                                Log.w(TAG, "Proxy detected. Enabling Conscrypt.setUseEngineSocketByDefault()");
                                Conscrypt.setUseEngineSocketByDefault(true);
                              }
                            })
                            .addBlocking("blob-provider", this::initializeBlobProvider)
                            .addBlocking("feature-flags", FeatureFlags::init)
                            .addBlocking("glide", () -> SignalGlideModule.setRegisterGlideComponents(new SignalGlideComponents()))
                            .addNonBlocking(this::cleanAvatarStorage)
                            .addNonBlocking(this::initializeRevealableMessageManager)
                            .addNonBlocking(this::initializePendingRetryReceiptManager)
                            .addNonBlocking(this::initializeFcmCheck)
                            .addNonBlocking(CreateSignedPreKeyJob::enqueueIfNeeded)
                            .addNonBlocking(this::initializePeriodicTasks)
                            .addNonBlocking(this::initializeCircumvention)
                            .addNonBlocking(this::initializePendingMessages)
                            .addNonBlocking(this::initializeCleanup)
                            .addNonBlocking(this::initializeGlideCodecs)
                            .addNonBlocking(RefreshPreKeysJob::scheduleIfNecessary)
                            .addNonBlocking(StorageSyncHelper::scheduleRoutineSync)
                            .addNonBlocking(() -> ApplicationDependencies.getJobManager().beginJobLoop())
                            .addNonBlocking(EmojiSource::refresh)
                            .addNonBlocking(() -> ApplicationDependencies.getGiphyMp4Cache().onAppStart(this))
                            .addNonBlocking(this::ensureProfileUploaded)
                            .addNonBlocking(() -> ApplicationDependencies.getExpireStoriesManager().scheduleIfNecessary())
                            .addPostRender(() -> RateLimitUtil.retryAllRateLimitedMessages(this))
                            .addPostRender(this::initializeExpiringMessageManager)
                            .addPostRender(() -> SignalStore.settings().setDefaultSms(Util.isDefaultSmsProvider(this)))
                            .addPostRender(() -> DownloadLatestEmojiDataJob.scheduleIfNecessary(this))
                            .addPostRender(EmojiSearchIndexDownloadJob::scheduleIfNecessary)
                            .addPostRender(() -> SignalDatabase.messageLog().trimOldMessages(System.currentTimeMillis(), FeatureFlags.retryRespondMaxAge()))
                            .addPostRender(() -> JumboEmoji.updateCurrentVersion(this))
                            .addPostRender(RetrieveReleaseChannelJob::enqueue)
                            .addPostRender(() -> AndroidTelecomUtil.registerPhoneAccount())
                            .addPostRender(() -> ApplicationDependencies.getJobManager().add(new FontDownloaderJob()))
                            .addPostRender(CheckServiceReachabilityJob::enqueueIfNecessary)
                            .addPostRender(GroupV2UpdateSelfProfileKeyJob::enqueueForGroupsIfNecessary)
                            .execute();

    Log.d(TAG, "onCreate() took " + (System.currentTimeMillis() - startTime) + " ms");
    SignalLocalMetrics.ColdStart.onApplicationCreateFinished();
    Tracer.getInstance().end("Application#onCreate()");
  }

  @Override
  public void onForeground() {
    long startTime = System.currentTimeMillis();
    Log.i(TAG, "App is now visible.");

    ApplicationDependencies.getFrameRateTracker().start();
    ApplicationDependencies.getMegaphoneRepository().onAppForegrounded();
    ApplicationDependencies.getDeadlockDetector().start();
    SubscriptionKeepAliveJob.launchSubscriberIdKeepAliveJobIfNecessary();

    SignalExecutors.BOUNDED.execute(() -> {
      FeatureFlags.refreshIfNecessary();
      ApplicationDependencies.getRecipientCache().warmUp();
      RetrieveProfileJob.enqueueRoutineFetchIfNecessary(this);
      executePendingContactSync();
      KeyCachingService.onAppForegrounded(this);
      ApplicationDependencies.getShakeToReport().enable();
      checkBuildExpiration();
    });

    Log.d(TAG, "onStart() took " + (System.currentTimeMillis() - startTime) + " ms");
  }

  @Override
  public void onBackground() {
    Log.i(TAG, "App is no longer visible.");
    KeyCachingService.onAppBackgrounded(this);
    ApplicationDependencies.getMessageNotifier().clearVisibleThread();
    ApplicationDependencies.getFrameRateTracker().stop();
    ApplicationDependencies.getShakeToReport().disable();
    ApplicationDependencies.getDeadlockDetector().stop();
  }

  public PersistentLogger getPersistentLogger() {
    return persistentLogger;
  }

  public void checkBuildExpiration() {
    if (Util.getTimeUntilBuildExpiry() <= 0 && !SignalStore.misc().isClientDeprecated()) {
      Log.w(TAG, "Build expired!");
      SignalStore.misc().markClientDeprecated();
    }
  }

  private void initializeSecurityProvider() {
    try {
      Class.forName("org.signal.aesgcmprovider.AesGcmCipher");
    } catch (ClassNotFoundException e) {
      Log.e(TAG, "Failed to find AesGcmCipher class");
      throw new ProviderInitializationException();
    }

    int aesPosition = Security.insertProviderAt(new AesGcmProvider(), 1);
    Log.i(TAG, "Installed AesGcmProvider: " + aesPosition);

    if (aesPosition < 0) {
      Log.e(TAG, "Failed to install AesGcmProvider()");
      throw new ProviderInitializationException();
    }

    int conscryptPosition = Security.insertProviderAt(Conscrypt.newProvider(), 2);
    Log.i(TAG, "Installed Conscrypt provider: " + conscryptPosition);

    if (conscryptPosition < 0) {
      Log.w(TAG, "Did not install Conscrypt provider. May already be present.");
    }
  }

  private void initializeLogging() {
    persistentLogger = new PersistentLogger(this);
    org.signal.core.util.logging.Log.initialize(FeatureFlags::internalUser, new AndroidLogger(), persistentLogger);

    SignalProtocolLoggerProvider.setProvider(new CustomSignalProtocolLogger());

    SignalExecutors.UNBOUNDED.execute(() -> {
      Log.blockUntilAllWritesFinished();
      LogDatabase.getInstance(this).trimToSize();
    });
  }

  private void initializeCrashHandling() {
    final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(new SignalUncaughtExceptionHandler(originalHandler));
  }

  private void initializeRx() {
    RxDogTag.install();
    RxJavaPlugins.setInitIoSchedulerHandler(schedulerSupplier -> Schedulers.from(SignalExecutors.BOUNDED_IO, true, false));
    RxJavaPlugins.setInitComputationSchedulerHandler(schedulerSupplier -> Schedulers.from(SignalExecutors.BOUNDED, true, false));
    RxJavaPlugins.setErrorHandler(e -> {
      boolean wasWrapped = false;
      while ((e instanceof UndeliverableException || e instanceof AssertionError || e instanceof OnErrorNotImplementedException) && e.getCause() != null) {
        wasWrapped = true;
        e = e.getCause();
      }

      if (wasWrapped && (e instanceof SocketException || e instanceof SocketTimeoutException || e instanceof InterruptedException)) {
        return;
      }

      Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
      if (uncaughtExceptionHandler == null) {
        uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
      }

      uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
    });
  }

  private void initializeApplicationMigrations() {
    ApplicationMigrations.onApplicationCreate(this, ApplicationDependencies.getJobManager());
  }

  public void initializeMessageRetrieval() {
    ApplicationDependencies.getIncomingMessageObserver();
  }

  private void initializeAppDependencies() {
    ApplicationDependencies.init(this, new ApplicationDependencyProvider(this));
  }

  private void initializeFirstEverAppLaunch() {
    if (TextSecurePreferences.getFirstInstallVersion(this) == -1) {
      if (!SignalDatabase.databaseFileExists(this) || VersionTracker.getDaysSinceFirstInstalled(this) < 365) {
        Log.i(TAG, "First ever app launch!");
        AppInitialization.onFirstEverAppLaunch(this);
      }

      Log.i(TAG, "Setting first install version to " + BuildConfig.CANONICAL_VERSION_CODE);
      TextSecurePreferences.setFirstInstallVersion(this, BuildConfig.CANONICAL_VERSION_CODE);
    } else if (!TextSecurePreferences.isPasswordDisabled(this) && VersionTracker.getDaysSinceFirstInstalled(this) < 90) {
      Log.i(TAG, "Detected a new install that doesn't have passphrases disabled -- assuming bad initialization.");
      AppInitialization.onRepairFirstEverAppLaunch(this);
    } else if (!TextSecurePreferences.isPasswordDisabled(this) && VersionTracker.getDaysSinceFirstInstalled(this) < 912) {
      Log.i(TAG, "Detected a not-recent install that doesn't have passphrases disabled -- disabling now.");
      TextSecurePreferences.setPasswordDisabled(this, true);
    }
  }

  private void initializeFcmCheck() {
    if (SignalStore.account().isRegistered()) {
      long nextSetTime = SignalStore.account().getFcmTokenLastSetTime() + TimeUnit.HOURS.toMillis(6);

      if (SignalStore.account().getFcmToken() == null || nextSetTime <= System.currentTimeMillis()) {
        ApplicationDependencies.getJobManager().add(new FcmRefreshJob());
      }
    }
  }

  private void initializeExpiringMessageManager() {
    ApplicationDependencies.getExpiringMessageManager().checkSchedule();
  }

  private void initializeRevealableMessageManager() {
    ApplicationDependencies.getViewOnceMessageManager().scheduleIfNecessary();
  }

  private void initializePendingRetryReceiptManager() {
    ApplicationDependencies.getPendingRetryReceiptManager().scheduleIfNecessary();
  }

  private void initializePeriodicTasks() {
    RotateSignedPreKeyListener.schedule(this);
    DirectoryRefreshListener.schedule(this);
    LocalBackupListener.schedule(this);
    RotateSenderCertificateListener.schedule(this);
    MessageProcessReceiver.startOrUpdateAlarm(this);

    if (BuildConfig.PLAY_STORE_DISABLED) {
      UpdateApkRefreshListener.schedule(this);
    }
  }

  private void initializeRingRtc() {
    try {
      CallManager.initialize(this, new RingRtcLogger());
    } catch (UnsatisfiedLinkError e) {
      throw new AssertionError("Unable to load ringrtc library", e);
    }
  }

  @WorkerThread
  private void initializeCircumvention() {
    if (ApplicationDependencies.getSignalServiceNetworkAccess().isCensored()) {
      try {
        ProviderInstaller.installIfNeeded(ApplicationContext.this);
      } catch (Throwable t) {
        Log.w(TAG, t);
      }
    }
  }

  private void ensureProfileUploaded() {
    if (SignalStore.account().isRegistered() && !SignalStore.registrationValues().hasUploadedProfile() && !Recipient.self().getProfileName().isEmpty()) {
      Log.w(TAG, "User has a profile, but has not uploaded one. Uploading now.");
      ApplicationDependencies.getJobManager().add(new ProfileUploadJob());
    }
  }

  private void executePendingContactSync() {
    if (TextSecurePreferences.needsFullContactSync(this)) {
      ApplicationDependencies.getJobManager().add(new MultiDeviceContactUpdateJob(true));
    }
  }

  private void initializePendingMessages() {
    if (TextSecurePreferences.getNeedsMessagePull(this)) {
      Log.i(TAG, "Scheduling a message fetch.");
      if (Build.VERSION.SDK_INT >= 26) {
        FcmJobService.schedule(this);
      } else {
        ApplicationDependencies.getJobManager().add(new PushNotificationReceiveJob());
      }
      TextSecurePreferences.setNeedsMessagePull(this, false);
    }
  }

  @WorkerThread
  private void initializeBlobProvider() {
    BlobProvider.getInstance().initialize(this);
  }

  @WorkerThread
  private void cleanAvatarStorage() {
    AvatarPickerStorage.cleanOrphans(this);
  }

  @WorkerThread
  private void initializeCleanup() {
    int deleted = SignalDatabase.attachments().deleteAbandonedPreuploadedAttachments();
    Log.i(TAG, "Deleted " + deleted + " abandoned attachments.");
  }

  private void initializeGlideCodecs() {
    SignalGlideCodecs.setLogProvider(new org.signal.glide.Log.Provider() {
      @Override
      public void v(@NonNull String tag, @NonNull String message) {
        Log.v(tag, message);
      }

      @Override
      public void d(@NonNull String tag, @NonNull String message) {
        Log.d(tag, message);
      }

      @Override
      public void i(@NonNull String tag, @NonNull String message) {
        Log.i(tag, message);
      }

      @Override
      public void w(@NonNull String tag, @NonNull String message) {
        Log.w(tag, message);
      }

      @Override
      public void e(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        Log.e(tag, message, throwable);
      }
    });
  }

  @Override
  protected void attachBaseContext(Context base) {
    DynamicLanguageContextWrapper.updateContext(base);
    super.attachBaseContext(base);
  }

  private static class ProviderInitializationException extends RuntimeException {
  }
}
