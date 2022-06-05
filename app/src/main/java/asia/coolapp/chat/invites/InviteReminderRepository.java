package asia.coolapp.chat.invites;

import android.content.Context;

import asia.coolapp.chat.database.MmsSmsDatabase;
import asia.coolapp.chat.database.RecipientDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.recipients.Recipient;

public final class InviteReminderRepository implements InviteReminderModel.Repository {

  private final Context context;

  public InviteReminderRepository(Context context) {
    this.context = context;
  }

  @Override
  public void setHasSeenFirstInviteReminder(Recipient recipient) {
    RecipientDatabase recipientDatabase = SignalDatabase.recipients();
    recipientDatabase.setSeenFirstInviteReminder(recipient.getId());
  }

  @Override
  public void setHasSeenSecondInviteReminder(Recipient recipient) {
    RecipientDatabase recipientDatabase = SignalDatabase.recipients();
    recipientDatabase.setSeenSecondInviteReminder(recipient.getId());
  }

  @Override
  public int getPercentOfInsecureMessages(int insecureCount) {
    MmsSmsDatabase mmsSmsDatabase = SignalDatabase.mmsSms();
    int            insecure       = mmsSmsDatabase.getInsecureMessageCountForInsights();
    int            secure         = mmsSmsDatabase.getSecureMessageCountForInsights();

    if (insecure + secure == 0) return 0;
    return Math.round(100f * (insecureCount / (float) (insecure + secure)));
  }
}
