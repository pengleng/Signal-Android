package asia.coolapp.chat.groups.ui;

import androidx.annotation.NonNull;

import asia.coolapp.chat.recipients.Recipient;

public interface RecipientClickListener {
  void onClick(@NonNull Recipient recipient);
}
