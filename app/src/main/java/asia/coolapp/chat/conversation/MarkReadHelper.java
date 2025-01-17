package asia.coolapp.chat.conversation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.database.MessageDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.ThreadDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.notifications.MarkReadReceiver;
import asia.coolapp.chat.util.Debouncer;
import asia.coolapp.chat.util.concurrent.SerialMonoLifoExecutor;

import java.util.List;
import java.util.concurrent.Executor;

class MarkReadHelper {
  private static final String TAG = Log.tag(MarkReadHelper.class);

  private static final long     DEBOUNCE_TIMEOUT = 100;
  private static final Executor EXECUTOR         = new SerialMonoLifoExecutor(SignalExecutors.BOUNDED);

  private final long           threadId;
  private final Context        context;
  private final LifecycleOwner lifecycleOwner;
  private final Debouncer      debouncer = new Debouncer(DEBOUNCE_TIMEOUT);
  private       long           latestTimestamp;

  MarkReadHelper(long threadId, @NonNull Context context, @NonNull LifecycleOwner lifecycleOwner) {
    this.threadId       = threadId;
    this.context        = context.getApplicationContext();
    this.lifecycleOwner = lifecycleOwner;
  }

  public void onViewsRevealed(long timestamp) {
    if (timestamp <= latestTimestamp || lifecycleOwner.getLifecycle().getCurrentState() != Lifecycle.State.RESUMED) {
      return;
    }

    latestTimestamp = timestamp;

    debouncer.publish(() -> {
      EXECUTOR.execute(() -> {
        ThreadDatabase                          threadDatabase = SignalDatabase.threads();
        List<MessageDatabase.MarkedMessageInfo> infos          = threadDatabase.setReadSince(threadId, false, timestamp);

        Log.d(TAG, "Marking " + infos.size() + " messages as read.");

        ApplicationDependencies.getMessageNotifier().updateNotification(context);
        MarkReadReceiver.process(context, infos);
      });
    });
  }
}
