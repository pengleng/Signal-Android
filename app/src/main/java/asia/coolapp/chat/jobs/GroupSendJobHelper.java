package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.RecipientAccessList;
import org.whispersystems.signalservice.api.messages.SendMessageResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class GroupSendJobHelper {

  private static final String TAG = Log.tag(GroupSendJobHelper.class);

  private GroupSendJobHelper() {
  }

  static @NonNull SendResult getCompletedSends(@NonNull List<Recipient> possibleRecipients, @NonNull Collection<SendMessageResult> results) {
    RecipientAccessList accessList  = new RecipientAccessList(possibleRecipients);
    List<Recipient>     completions = new ArrayList<>(results.size());
    List<RecipientId>   skipped     = new ArrayList<>();

    for (SendMessageResult sendMessageResult : results) {
      Recipient recipient = accessList.requireByAddress(sendMessageResult.getAddress());

      if (sendMessageResult.getIdentityFailure() != null) {
        Log.w(TAG, "Identity failure for " + recipient.getId());
      }

      if (sendMessageResult.isUnregisteredFailure()) {
        Log.w(TAG, "Unregistered failure for " + recipient.getId());
        skipped.add(recipient.getId());
      }

      if (sendMessageResult.getProofRequiredFailure() != null) {
        Log.w(TAG, "Proof required failure for " + recipient.getId());
        skipped.add(recipient.getId());
      }

      if (sendMessageResult.getSuccess()               != null ||
          sendMessageResult.getIdentityFailure()       != null ||
          sendMessageResult.getProofRequiredFailure()  != null ||
          sendMessageResult.isUnregisteredFailure())
      {
        completions.add(recipient);
      }
    }

    return new SendResult(completions, skipped);
  }

  public static class SendResult {
    public final List<Recipient>   completed;
    public final List<RecipientId> skipped;

    public SendResult(@NonNull List<Recipient> completed, @NonNull List<RecipientId> skipped) {
      this.completed = completed;
      this.skipped   = skipped;
    }
  }
}
