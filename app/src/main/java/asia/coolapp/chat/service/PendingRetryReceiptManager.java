package asia.coolapp.chat.service;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.database.MessageDatabase;
import asia.coolapp.chat.database.PendingRetryReceiptCache;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.model.PendingRetryReceiptModel;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.util.FeatureFlags;


/**
 * Manages the time-based creation of error messages for retries that are pending for messages we couldn't decrypt.
 */
public final class PendingRetryReceiptManager extends TimedEventManager<PendingRetryReceiptModel> {

  private static final String TAG = Log.tag(PendingRetryReceiptManager.class);

  private final PendingRetryReceiptCache pendingCache;
  private final MessageDatabase          messageDatabase;

  public PendingRetryReceiptManager(@NonNull Application application) {
    super(application, "PendingRetryReceiptManager");

    this.pendingCache    = ApplicationDependencies.getPendingRetryReceiptCache();
    this.messageDatabase = SignalDatabase.sms();

    scheduleIfNecessary();
  }

  @WorkerThread
  @Override
  protected @Nullable PendingRetryReceiptModel getNextClosestEvent() {
    PendingRetryReceiptModel model = pendingCache.getOldest();

    if (model != null) {
      Log.i(TAG, "Next closest expiration is in " + getDelayForEvent(model) + " ms for timestamp " + model.getSentTimestamp() + ".");
    } else {
      Log.d(TAG, "No pending receipts to schedule.");
    }

    return model;
  }

  @WorkerThread
  @Override
  protected void executeEvent(@NonNull PendingRetryReceiptModel event) {
    Log.w(TAG, "It's been " + (System.currentTimeMillis() - event.getReceivedTimestamp()) + " ms since this retry receipt was received. Showing an error.");
    messageDatabase.insertBadDecryptMessage(event.getAuthor(), event.getAuthorDevice(), event.getSentTimestamp(), event.getReceivedTimestamp(), event.getThreadId());
    pendingCache.delete(event);
  }

  @WorkerThread
  @Override
  protected long getDelayForEvent(@NonNull PendingRetryReceiptModel event) {
    long expiresAt = event.getReceivedTimestamp() + FeatureFlags.retryReceiptLifespan();
    long timeLeft  = expiresAt - System.currentTimeMillis();

    return Math.max(0, timeLeft);
  }

  @AnyThread
  @Override
  protected void scheduleAlarm(@NonNull Application application, long delay) {
    setAlarm(application, delay, PendingRetryReceiptAlarm.class);
  }

  public static class PendingRetryReceiptAlarm extends BroadcastReceiver {

    private static final String TAG = Log.tag(PendingRetryReceiptAlarm.class);

    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d(TAG, "onReceive()");
      ApplicationDependencies.getPendingRetryReceiptManager().scheduleIfNecessary();
    }
  }
}
