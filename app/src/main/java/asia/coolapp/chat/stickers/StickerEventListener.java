package asia.coolapp.chat.stickers;

import androidx.annotation.NonNull;

import asia.coolapp.chat.database.model.StickerRecord;

public interface StickerEventListener {
  void onStickerSelected(@NonNull StickerRecord sticker);

  void onStickerManagementClicked();
}
