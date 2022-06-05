package asia.coolapp.chat.conversation.ui.mentions;

import androidx.annotation.NonNull;

import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.util.viewholders.RecipientMappingModel;

public final class MentionViewState extends RecipientMappingModel<MentionViewState> {

  private final Recipient recipient;

  public MentionViewState(@NonNull Recipient recipient) {
    this.recipient = recipient;
  }

  @Override
  public @NonNull Recipient getRecipient() {
    return recipient;
  }
}
