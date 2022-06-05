package asia.coolapp.chat.database;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.R;
import asia.coolapp.chat.components.emoji.EmojiStrings;
import asia.coolapp.chat.contactshare.Contact;
import asia.coolapp.chat.contactshare.ContactUtil;
import asia.coolapp.chat.database.model.MessageRecord;
import asia.coolapp.chat.database.model.MmsMessageRecord;
import asia.coolapp.chat.mms.GifSlide;
import asia.coolapp.chat.mms.Slide;
import asia.coolapp.chat.mms.StickerSlide;
import asia.coolapp.chat.util.MessageRecordUtil;
import asia.coolapp.chat.util.Util;

import java.util.Objects;

public final class ThreadBodyUtil {

  private static final String TAG = Log.tag(ThreadBodyUtil.class);

  private ThreadBodyUtil() {
  }

  public static @NonNull String getFormattedBodyFor(@NonNull Context context, @NonNull MessageRecord record) {
    if (record.isMms()) {
      return getFormattedBodyForMms(context, (MmsMessageRecord) record);
    }

    return record.getBody();
  }

  private static @NonNull String getFormattedBodyForMms(@NonNull Context context, @NonNull MmsMessageRecord record) {
    if (record.getSharedContacts().size() > 0) {
      Contact contact = record.getSharedContacts().get(0);

      return ContactUtil.getStringSummary(context, contact).toString();
    } else if (record.getSlideDeck().getDocumentSlide() != null) {
      return format(context, record, EmojiStrings.FILE, R.string.ThreadRecord_file);
    } else if (record.getSlideDeck().getAudioSlide() != null) {
      return format(context, record, EmojiStrings.AUDIO, R.string.ThreadRecord_voice_message);
    } else if (MessageRecordUtil.hasSticker(record)) {
      String emoji = getStickerEmoji(record);
      return format(context, record, emoji, R.string.ThreadRecord_sticker);
    }

    boolean hasImage = false;
    boolean hasVideo = false;
    boolean hasGif   = false;

    for (Slide slide : record.getSlideDeck().getSlides()) {
      hasVideo |= slide.hasVideo();
      hasImage |= slide.hasImage();
      hasGif   |= slide instanceof GifSlide || slide.isVideoGif();
    }

    if (hasGif) {
      return format(context, record, EmojiStrings.GIF, R.string.ThreadRecord_gif);
    } else if (hasVideo) {
      return format(context, record, EmojiStrings.VIDEO, R.string.ThreadRecord_video);
    } else if (hasImage) {
      return format(context, record, EmojiStrings.PHOTO, R.string.ThreadRecord_photo);
    } else if (TextUtils.isEmpty(record.getBody())) {
      return context.getString(R.string.ThreadRecord_media_message);
    } else {
      return getBody(context, record);
    }
  }
  
  private static @NonNull String format(@NonNull Context context, @NonNull MessageRecord record, @NonNull String emoji, @StringRes int defaultStringRes) {
    return String.format("%s %s", emoji, getBodyOrDefault(context, record, defaultStringRes));
  }

  private static @NonNull String getBodyOrDefault(@NonNull Context context, @NonNull MessageRecord record, @StringRes int defaultStringRes) {
    return TextUtils.isEmpty(record.getBody()) ? context.getString(defaultStringRes) : getBody(context, record);
  }

  private static @NonNull String getBody(@NonNull Context context, @NonNull MessageRecord record) {
    return MentionUtil.updateBodyWithDisplayNames(context, record, record.getBody()).toString();
  }

  private static @NonNull String getStickerEmoji(@NonNull MessageRecord record) {
    StickerSlide slide = Objects.requireNonNull(((MmsMessageRecord) record).getSlideDeck().getStickerSlide());

    return Util.isEmpty(slide.getEmoji()) ? EmojiStrings.STICKER
                                          : slide.getEmoji();
  }
}
