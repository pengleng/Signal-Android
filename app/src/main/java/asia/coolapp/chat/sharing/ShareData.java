package asia.coolapp.chat.sharing;

import android.net.Uri;

import androidx.annotation.NonNull;

import asia.coolapp.chat.mediasend.Media;
import asia.coolapp.chat.util.MediaUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ShareData {

  private final Optional<Uri>    uri;
  private final Optional<String> mimeType;
  private final Optional<ArrayList<Media>> media;
  private final boolean                    external;
  private final boolean                    isMmsOrSmsSupported;

  static ShareData forIntentData(@NonNull Uri uri, @NonNull String mimeType, boolean external, boolean isMmsOrSmsSupported) {
    return new ShareData(Optional.of(uri), Optional.of(mimeType), Optional.empty(), external, isMmsOrSmsSupported);
  }

  static ShareData forPrimitiveTypes() {
    return new ShareData(Optional.empty(), Optional.empty(), Optional.empty(), true, true);
  }

  static ShareData forMedia(@NonNull List<Media> media, boolean isMmsOrSmsSupported) {
    return new ShareData(Optional.empty(), Optional.empty(), Optional.of(new ArrayList<>(media)), true, isMmsOrSmsSupported);
  }

  private ShareData(Optional<Uri> uri, Optional<String> mimeType, Optional<ArrayList<Media>> media, boolean external, boolean isMmsOrSmsSupported) {
    this.uri                 = uri;
    this.mimeType            = mimeType;
    this.media               = media;
    this.external            = external;
    this.isMmsOrSmsSupported = isMmsOrSmsSupported;
  }

  boolean isForIntent() {
    return uri.isPresent();
  }

  boolean isForPrimitive() {
    return !uri.isPresent() && !media.isPresent();
  }

  boolean isForMedia() {
    return media.isPresent();
  }

  public @NonNull Uri getUri() {
    return uri.get();
  }

  public @NonNull String getMimeType() {
    return mimeType.get();
  }

  public @NonNull ArrayList<Media> getMedia() {
    return media.get();
  }

  public boolean isExternal() {
    return external;
  }

  public boolean isMmsOrSmsSupported() {
    return isMmsOrSmsSupported;
  }

  public boolean isStoriesSupported() {
    if (isForIntent()) {
      return MediaUtil.isStorySupportedType(getMimeType());
    } else if (isForMedia()) {
      return getMedia().stream().allMatch(media -> MediaUtil.isStorySupportedType(media.getMimeType()));
    } else {
      return false;
    }
  }
}
