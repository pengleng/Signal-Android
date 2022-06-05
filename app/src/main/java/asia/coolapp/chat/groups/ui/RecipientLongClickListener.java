package asia.coolapp.chat.groups.ui;

import androidx.annotation.NonNull;

import asia.coolapp.chat.recipients.Recipient;

public interface RecipientLongClickListener {
  boolean onLongClick(@NonNull Recipient recipient);
}
