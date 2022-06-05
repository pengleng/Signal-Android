package asia.coolapp.chat.notifications;

import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.database.RecipientDatabase;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.recipients.Recipient;

public enum ReplyMethod {

  GroupMessage,
  SecureMessage,
  UnsecuredSmsMessage;

  public static @NonNull ReplyMethod forRecipient(Context context, Recipient recipient) {
    if (recipient.isGroup()) {
      return ReplyMethod.GroupMessage;
    } else if (SignalStore.account().isRegistered() && recipient.getRegistered() == RecipientDatabase.RegisteredState.REGISTERED && !recipient.isForceSmsSelection()) {
      return ReplyMethod.SecureMessage;
    } else {
      return ReplyMethod.UnsecuredSmsMessage;
    }
  }
}
