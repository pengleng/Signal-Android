package asia.coolapp.chat.payments.preferences.model;

import androidx.annotation.NonNull;

import asia.coolapp.chat.util.adapter.mapping.MappingModel;

public class NoRecentActivity implements MappingModel<NoRecentActivity> {
  @Override
  public boolean areItemsTheSame(@NonNull NoRecentActivity newItem) {
    return true;
  }

  @Override
  public boolean areContentsTheSame(@NonNull NoRecentActivity newItem) {
    return true;
  }
}
