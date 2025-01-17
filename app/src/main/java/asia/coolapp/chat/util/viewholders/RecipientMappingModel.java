package asia.coolapp.chat.util.viewholders;

import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.adapter.mapping.MappingModel;

import java.util.Objects;

public abstract class RecipientMappingModel<T extends RecipientMappingModel<T>> implements MappingModel<T> {

  public abstract @NonNull Recipient getRecipient();

  public @NonNull String getName(@NonNull Context context) {
    return getRecipient().getDisplayName(context);
  }

  @Override
  public boolean areItemsTheSame(@NonNull T newItem) {
    return getRecipient().getId().equals(newItem.getRecipient().getId());
  }

  @Override
  public boolean areContentsTheSame(@NonNull T newItem) {
    Context context = ApplicationDependencies.getApplication();
    return getName(context).equals(newItem.getName(context)) && Objects.equals(getRecipient().getContactPhoto(), newItem.getRecipient().getContactPhoto());
  }

  public static class RecipientIdMappingModel extends RecipientMappingModel<RecipientIdMappingModel> {

    private final RecipientId recipientId;

    public RecipientIdMappingModel(@NonNull RecipientId recipientId) {
      this.recipientId = recipientId;
    }

    @Override
    public @NonNull Recipient getRecipient() {
      return Recipient.resolved(recipientId);
    }
  }
}
