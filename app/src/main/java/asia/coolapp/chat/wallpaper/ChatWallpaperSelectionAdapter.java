package asia.coolapp.chat.wallpaper;

import androidx.annotation.Nullable;

import asia.coolapp.chat.R;
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter;

class ChatWallpaperSelectionAdapter extends MappingAdapter {
  ChatWallpaperSelectionAdapter(@Nullable ChatWallpaperViewHolder.EventListener eventListener) {
    registerFactory(ChatWallpaperSelectionMappingModel.class, ChatWallpaperViewHolder.createFactory(R.layout.chat_wallpaper_selection_fragment_adapter_item, eventListener, null));
  }
}
