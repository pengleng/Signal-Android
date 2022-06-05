package asia.coolapp.chat.jobs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.DecryptionsDrainedConstraint;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.messages.GroupSendUtil;
import asia.coolapp.chat.net.NotPushRegisteredException;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.recipients.RecipientUtil;
import asia.coolapp.chat.transport.RetryLaterException;
import org.whispersystems.signalservice.api.crypto.ContentHint;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SendMessageResult;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProfileKeySendJob extends BaseJob {

  private static final String TAG            = Log.tag(ProfileKeySendJob.class);
  private static final String KEY_RECIPIENTS = "recipients";
  private static final String KEY_THREAD     = "thread";

  public static final String KEY = "ProfileKeySendJob";

  private final long              threadId;
  private final List<RecipientId> recipients;

  /**
   * Suitable for a 1:1 conversation or a GV1 group only.
   *
   * @param queueLimits True if you only want one of these to be run per person after decryptions
   *                    are drained, otherwise false.
   */
  @WorkerThread
  public static ProfileKeySendJob create(@NonNull Context context, long threadId, boolean queueLimits) {
    Recipient conversationRecipient = SignalDatabase.threads().getRecipientForThreadId(threadId);

    if (conversationRecipient == null) {
      throw new AssertionError("We have a thread but no recipient!");
    }

    if (conversationRecipient.isPushV2Group()) {
      throw new AssertionError("Do not send profile keys directly for GV2");
    }

    List<RecipientId> recipients = conversationRecipient.isGroup() ? Stream.of(RecipientUtil.getEligibleForSending(conversationRecipient.getParticipants())).map(Recipient::getId).toList()
                                                                   : Stream.of(conversationRecipient.getId()).toList();

    recipients.remove(Recipient.self().getId());

    if (queueLimits) {
      return new ProfileKeySendJob(new Parameters.Builder()
                                                 .setQueue("ProfileKeySendJob_" + conversationRecipient.getId().toQueueKey())
                                                 .setMaxInstancesForQueue(1)
                                                 .addConstraint(NetworkConstraint.KEY)
                                                 .addConstraint(DecryptionsDrainedConstraint.KEY)
                                                 .setLifespan(TimeUnit.DAYS.toMillis(1))
                                                 .setMaxAttempts(Parameters.UNLIMITED)
                                                 .build(), threadId, recipients);
    } else {
      return new ProfileKeySendJob(new Parameters.Builder()
                                                 .setQueue(conversationRecipient.getId().toQueueKey())
                                                 .addConstraint(NetworkConstraint.KEY)
                                                 .setLifespan(TimeUnit.DAYS.toMillis(1))
                                                 .setMaxAttempts(Parameters.UNLIMITED)
                                                 .build(), threadId, recipients);
    }
  }

  private ProfileKeySendJob(@NonNull Parameters parameters, long threadId, @NonNull List<RecipientId> recipients) {
    super(parameters);
    this.threadId   = threadId;
    this.recipients = recipients;
  }

  @Override
  protected void onRun() throws Exception {
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    Recipient conversationRecipient = SignalDatabase.threads().getRecipientForThreadId(threadId);

    if (conversationRecipient == null) {
      Log.w(TAG, "Thread no longer present");
      return;
    }

    List<Recipient> destinations = Stream.of(recipients).map(Recipient::resolved).toList();
    List<Recipient> completions  = deliver(destinations);

    for (Recipient completion : completions) {
      recipients.remove(completion.getId());
    }

    Log.i(TAG, "Completed now: " + completions.size() + ", Remaining: " + recipients.size());

    if (!recipients.isEmpty()) {
      Log.w(TAG, "Still need to send to " + recipients.size() + " recipients. Retrying.");
      throw new RetryLaterException();
    }
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    if (e instanceof NotPushRegisteredException) return false;
    return e instanceof IOException ||
           e instanceof RetryLaterException;
  }

  @Override
  public @NonNull Data serialize() {
    return new Data.Builder()
                   .putLong(KEY_THREAD, threadId)
                   .putString(KEY_RECIPIENTS, RecipientId.toSerializedList(recipients))
                   .build();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onFailure() {

  }

  private List<Recipient> deliver(@NonNull List<Recipient> destinations) throws IOException, UntrustedIdentityException {
    SignalServiceDataMessage.Builder dataMessage = SignalServiceDataMessage.newBuilder()
                                                                           .asProfileKeyUpdate(true)
                                                                           .withTimestamp(System.currentTimeMillis())
                                                                           .withProfileKey(Recipient.self().resolve().getProfileKey());

    List<SendMessageResult> results = GroupSendUtil.sendUnresendableDataMessage(context, null, destinations, false, ContentHint.IMPLICIT, dataMessage.build());

    return GroupSendJobHelper.getCompletedSends(destinations, results).completed;
  }

  public static class Factory implements Job.Factory<ProfileKeySendJob> {

    @Override
    public @NonNull ProfileKeySendJob create(@NonNull Parameters parameters, @NonNull Data data) {
      long              threadId   = data.getLong(KEY_THREAD);
      List<RecipientId> recipients = RecipientId.fromSerializedList(data.getString(KEY_RECIPIENTS));

      return new ProfileKeySendJob(parameters, threadId, recipients);
    }
  }
}
