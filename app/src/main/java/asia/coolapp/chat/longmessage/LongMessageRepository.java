package asia.coolapp.chat.longmessage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.signal.core.util.StreamUtil;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.conversation.ConversationMessage.ConversationMessageFactory;
import asia.coolapp.chat.database.MessageDatabase;
import asia.coolapp.chat.database.MmsDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.SmsDatabase;
import asia.coolapp.chat.database.model.MessageRecord;
import asia.coolapp.chat.database.model.MmsMessageRecord;
import asia.coolapp.chat.mms.PartAuthority;
import asia.coolapp.chat.mms.TextSlide;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

class LongMessageRepository {

  private final static String TAG = Log.tag(LongMessageRepository.class);

  private final MessageDatabase mmsDatabase;
  private final MessageDatabase smsDatabase;

  LongMessageRepository() {
    this.mmsDatabase = SignalDatabase.mms();
    this.smsDatabase = SignalDatabase.sms();
  }

  void getMessage(@NonNull Context context, long messageId, boolean isMms, @NonNull Callback<Optional<LongMessage>> callback) {
    SignalExecutors.BOUNDED.execute(() -> {
      if (isMms) {
        callback.onComplete(getMmsLongMessage(context, mmsDatabase, messageId));
      } else {
        callback.onComplete(getSmsLongMessage(context, smsDatabase, messageId));
      }
    });
  }

  @WorkerThread
  private Optional<LongMessage> getMmsLongMessage(@NonNull Context context, @NonNull MessageDatabase mmsDatabase, long messageId) {
    Optional<MmsMessageRecord> record = getMmsMessage(mmsDatabase, messageId);

    if (record.isPresent()) {
      TextSlide textSlide = record.get().getSlideDeck().getTextSlide();

      if (textSlide != null && textSlide.getUri() != null) {
        return Optional.of(new LongMessage(ConversationMessageFactory.createWithUnresolvedData(context, record.get(), readFullBody(context, textSlide.getUri()))));
      } else {
        return Optional.of(new LongMessage(ConversationMessageFactory.createWithUnresolvedData(context, record.get())));
      }
    } else {
      return Optional.empty();
    }
  }

  @WorkerThread
  private Optional<LongMessage> getSmsLongMessage(@NonNull Context context, @NonNull MessageDatabase smsDatabase, long messageId) {
    Optional<MessageRecord> record = getSmsMessage(smsDatabase, messageId);

    if (record.isPresent()) {
      return Optional.of(new LongMessage(ConversationMessageFactory.createWithUnresolvedData(context, record.get())));
    } else {
      return Optional.empty();
    }
  }


  @WorkerThread
  private Optional<MmsMessageRecord> getMmsMessage(@NonNull MessageDatabase mmsDatabase, long messageId) {
    try (Cursor cursor = mmsDatabase.getMessageCursor(messageId)) {
      return Optional.ofNullable((MmsMessageRecord) MmsDatabase.readerFor(cursor).getNext());
    }
  }

  @WorkerThread
  private Optional<MessageRecord> getSmsMessage(@NonNull MessageDatabase smsDatabase, long messageId) {
    try (Cursor cursor = smsDatabase.getMessageCursor(messageId)) {
      return Optional.ofNullable(SmsDatabase.readerFor(cursor).getNext());
    }
  }

  private @NonNull String readFullBody(@NonNull Context context, @NonNull Uri uri) {
    try (InputStream stream = PartAuthority.getAttachmentStream(context, uri)) {
      return StreamUtil.readFullyAsString(stream);
    } catch (IOException e) {
      Log.w(TAG, "Failed to read full text body.", e);
      return "";
    }
  }

  interface Callback<T> {
    void onComplete(T result);
  }
}
