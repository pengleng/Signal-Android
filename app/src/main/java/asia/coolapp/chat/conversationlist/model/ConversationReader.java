package asia.coolapp.chat.conversationlist.model;

import android.database.Cursor;

import androidx.annotation.NonNull;

import asia.coolapp.chat.database.ThreadDatabase;
import asia.coolapp.chat.database.model.ThreadRecord;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.recipients.Recipient;
import org.signal.core.util.CursorUtil;

public class ConversationReader extends ThreadDatabase.StaticReader {

  public static final String[] HEADER_COLUMN    = {"header"};
  public static final String[] ARCHIVED_COLUMNS = {"header", "count"};
  public static final String[] PINNED_HEADER    = {Conversation.Type.PINNED_HEADER.toString()};
  public static final String[] UNPINNED_HEADER  = {Conversation.Type.UNPINNED_HEADER.toString()};

  private final Cursor cursor;

  public ConversationReader(@NonNull Cursor cursor) {
    super(cursor, ApplicationDependencies.getApplication());
    this.cursor = cursor;
  }

  public static String[] createArchivedFooterRow(int archivedCount) {
    return new String[]{Conversation.Type.ARCHIVED_FOOTER.toString(), String.valueOf(archivedCount)};
  }

  @Override
  public ThreadRecord getCurrent() {
    if (cursor.getColumnIndex(HEADER_COLUMN[0]) == -1) {
      return super.getCurrent();
    } else {
      return buildThreadRecordForHeader();
    }
  }

  private ThreadRecord buildThreadRecordForHeader() {
    Conversation.Type type  = Conversation.Type.valueOf(CursorUtil.requireString(cursor, HEADER_COLUMN[0]));
    int               count = 0;
    if (type == Conversation.Type.ARCHIVED_FOOTER) {
      count = CursorUtil.requireInt(cursor, ARCHIVED_COLUMNS[1]);
    }
    return new ThreadRecord.Builder(-(100 + type.ordinal()))
                           .setBody(type.toString())
                           .setDate(100)
                           .setRecipient(Recipient.UNKNOWN)
                           .setUnreadCount(count)
                           .build();
  }
}
