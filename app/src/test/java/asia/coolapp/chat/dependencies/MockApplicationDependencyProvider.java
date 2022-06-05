package asia.coolapp.chat.dependencies;

import androidx.annotation.NonNull;

import org.signal.core.util.concurrent.DeadlockDetector;
import org.signal.libsignal.zkgroup.receipts.ClientZkReceiptOperations;
import asia.coolapp.chat.components.TypingStatusRepository;
import asia.coolapp.chat.components.TypingStatusSender;
import asia.coolapp.chat.crypto.storage.SignalServiceDataStoreImpl;
import asia.coolapp.chat.database.DatabaseObserver;
import asia.coolapp.chat.database.PendingRetryReceiptCache;
import asia.coolapp.chat.jobmanager.JobManager;
import asia.coolapp.chat.megaphone.MegaphoneRepository;
import asia.coolapp.chat.messages.BackgroundMessageRetriever;
import asia.coolapp.chat.messages.IncomingMessageObserver;
import asia.coolapp.chat.messages.IncomingMessageProcessor;
import asia.coolapp.chat.notifications.MessageNotifier;
import asia.coolapp.chat.payments.Payments;
import asia.coolapp.chat.push.SignalServiceNetworkAccess;
import asia.coolapp.chat.recipients.LiveRecipientCache;
import asia.coolapp.chat.revealable.ViewOnceMessageManager;
import asia.coolapp.chat.service.ExpiringMessageManager;
import asia.coolapp.chat.service.ExpiringStoriesManager;
import asia.coolapp.chat.service.PendingRetryReceiptManager;
import asia.coolapp.chat.service.TrimThreadsByDateManager;
import asia.coolapp.chat.service.webrtc.SignalCallManager;
import asia.coolapp.chat.shakereport.ShakeToReport;
import asia.coolapp.chat.util.AppForegroundObserver;
import asia.coolapp.chat.util.EarlyMessageCache;
import asia.coolapp.chat.util.FrameRateTracker;
import asia.coolapp.chat.video.exo.GiphyMp4Cache;
import asia.coolapp.chat.video.exo.SimpleExoPlayerPool;
import asia.coolapp.chat.webrtc.audio.AudioManagerCompat;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceDataStore;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.SignalWebSocket;
import org.whispersystems.signalservice.api.groupsv2.GroupsV2Operations;
import org.whispersystems.signalservice.api.services.DonationsService;

import static org.mockito.Mockito.mock;

public class MockApplicationDependencyProvider implements ApplicationDependencies.Provider {
  @Override
  public @NonNull GroupsV2Operations provideGroupsV2Operations() {
    return null;
  }

  @Override
  public @NonNull SignalServiceAccountManager provideSignalServiceAccountManager() {
    return null;
  }

  @Override
  public @NonNull SignalServiceMessageSender provideSignalServiceMessageSender(@NonNull SignalWebSocket signalWebSocket, @NonNull SignalServiceDataStore protocolStore) {
    return null;
  }

  @Override
  public @NonNull SignalServiceMessageReceiver provideSignalServiceMessageReceiver() {
    return null;
  }

  @Override
  public @NonNull SignalServiceNetworkAccess provideSignalServiceNetworkAccess() {
    return null;
  }

  @Override
  public @NonNull IncomingMessageProcessor provideIncomingMessageProcessor() {
    return null;
  }

  @Override
  public @NonNull BackgroundMessageRetriever provideBackgroundMessageRetriever() {
    return null;
  }

  @Override
  public @NonNull LiveRecipientCache provideRecipientCache() {
    return null;
  }

  @Override
  public @NonNull JobManager provideJobManager() {
    return mock(JobManager.class);
  }

  @Override
  public @NonNull FrameRateTracker provideFrameRateTracker() {
    return null;
  }

  @Override
  public @NonNull MegaphoneRepository provideMegaphoneRepository() {
    return null;
  }

  @Override
  public @NonNull EarlyMessageCache provideEarlyMessageCache() {
    return null;
  }

  @Override
  public @NonNull MessageNotifier provideMessageNotifier() {
    return null;
  }

  @Override
  public @NonNull IncomingMessageObserver provideIncomingMessageObserver() {
    return null;
  }

  @Override
  public @NonNull TrimThreadsByDateManager provideTrimThreadsByDateManager() {
    return null;
  }

  @Override
  public @NonNull ViewOnceMessageManager provideViewOnceMessageManager() {
    return null;
  }

  @Override
  public @NonNull ExpiringStoriesManager provideExpiringStoriesManager() {
    return null;
  }

  @Override
  public @NonNull ExpiringMessageManager provideExpiringMessageManager() {
    return null;
  }

  @Override
  public @NonNull TypingStatusRepository provideTypingStatusRepository() {
    return null;
  }

  @Override
  public @NonNull TypingStatusSender provideTypingStatusSender() {
    return null;
  }

  @Override
  public @NonNull DatabaseObserver provideDatabaseObserver() {
    return mock(DatabaseObserver.class);
  }

  @Override
  public @NonNull Payments providePayments(@NonNull SignalServiceAccountManager signalServiceAccountManager) {
    return null;
  }

  @Override
  public @NonNull ShakeToReport provideShakeToReport() {
    return null;
  }

  @Override
  public @NonNull AppForegroundObserver provideAppForegroundObserver() {
    return mock(AppForegroundObserver.class);
  }

  @Override
  public @NonNull SignalCallManager provideSignalCallManager() {
    return null;
  }

  @Override
  public @NonNull PendingRetryReceiptManager providePendingRetryReceiptManager() {
    return null;
  }

  @Override
  public @NonNull PendingRetryReceiptCache providePendingRetryReceiptCache() {
    return null;
  }

  @Override
  public @NonNull SignalWebSocket provideSignalWebSocket() {
    return null;
  }

  @Override
  public @NonNull SignalServiceDataStoreImpl provideProtocolStore() {
    return null;
  }

  @Override
  public @NonNull GiphyMp4Cache provideGiphyMp4Cache() {
    return null;
  }

  @Override
  public @NonNull SimpleExoPlayerPool provideExoPlayerPool() {
    return null;
  }

  @Override
  public @NonNull AudioManagerCompat provideAndroidCallAudioManager() {
    return null;
  }

  @Override
  public @NonNull DonationsService provideDonationsService() {
    return null;
  }

  @Override
  public @NonNull DeadlockDetector provideDeadlockDetector() {
    return null;
  }

  @Override
  public @NonNull ClientZkReceiptOperations provideClientZkReceiptOperations() {
    return null;
  }
}
