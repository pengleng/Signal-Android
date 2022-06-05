package asia.coolapp.chat.components.settings;

import androidx.annotation.NonNull;

import asia.coolapp.chat.R;
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory;
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter;

/**
 * Reusable adapter for generic settings list.
 */
public class BaseSettingsAdapter extends MappingAdapter {

  public BaseSettingsAdapter() {
    registerFactory(SettingHeader.Item.class, SettingHeader.ViewHolder::new, R.layout.base_settings_header_item);
    registerFactory(SettingProgress.Item.class, SettingProgress.ViewHolder::new, R.layout.base_settings_progress_item);
  }

  public void configureSingleSelect(@NonNull SingleSelectSetting.SingleSelectSelectionChangedListener selectionChangedListener) {
    registerFactory(SingleSelectSetting.Item.class,
                    new LayoutFactory<>(v -> new SingleSelectSetting.ViewHolder(v, selectionChangedListener), R.layout.single_select_item));
  }

  public void configureCustomizableSingleSelect(@NonNull CustomizableSingleSelectSetting.CustomizableSingleSelectionListener selectionListener) {
    registerFactory(CustomizableSingleSelectSetting.Item.class,
                    new LayoutFactory<>(v -> new CustomizableSingleSelectSetting.ViewHolder(v, selectionListener), R.layout.customizable_single_select_item));
  }
}
