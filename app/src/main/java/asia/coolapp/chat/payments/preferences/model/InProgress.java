package asia.coolapp.chat.payments.preferences.model;

import androidx.annotation.NonNull;

import asia.coolapp.chat.util.adapter.mapping.MappingModel;

public class InProgress implements MappingModel<InProgress> {
  @Override
  public boolean areItemsTheSame(@NonNull InProgress newItem) {
    return true;
  }

  @Override
  public boolean areContentsTheSame(@NonNull InProgress newItem) {
    return true;
  }
}
