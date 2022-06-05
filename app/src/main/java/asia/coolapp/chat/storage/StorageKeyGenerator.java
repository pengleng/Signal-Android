package asia.coolapp.chat.storage;

import androidx.annotation.NonNull;

/**
 * Generates a key for use with the storage service.
 */
interface StorageKeyGenerator {
  @NonNull
  byte[] generate();
}
