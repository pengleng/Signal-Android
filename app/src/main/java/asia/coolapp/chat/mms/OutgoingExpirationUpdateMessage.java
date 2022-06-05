package asia.coolapp.chat.mms;

import asia.coolapp.chat.database.ThreadDatabase;
import asia.coolapp.chat.database.model.StoryType;
import asia.coolapp.chat.recipients.Recipient;

import java.util.Collections;
import java.util.LinkedList;

public class OutgoingExpirationUpdateMessage extends OutgoingSecureMediaMessage {

  public OutgoingExpirationUpdateMessage(Recipient recipient, long sentTimeMillis, long expiresIn) {
    super(recipient,
          "",
          new LinkedList<>(),
          sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION,
          expiresIn,
          false,
          StoryType.NONE,
          null,
          false,
          null,
          Collections.emptyList(),
          Collections.emptyList(),
          Collections.emptyList());
  }

  @Override
  public boolean isExpirationUpdate() {
    return true;
  }

}
