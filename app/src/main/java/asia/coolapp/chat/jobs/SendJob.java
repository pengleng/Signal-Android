package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.BuildConfig;
import asia.coolapp.chat.TextSecureExpiredException;
import asia.coolapp.chat.attachments.Attachment;
import asia.coolapp.chat.attachments.DatabaseAttachment;
import asia.coolapp.chat.contactshare.Contact;
import asia.coolapp.chat.database.AttachmentDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.mms.OutgoingMediaMessage;
import asia.coolapp.chat.util.Util;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SendJob extends BaseJob {

  @SuppressWarnings("unused")
  private final static String TAG = Log.tag(SendJob.class);

  public SendJob(Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public final void onRun() throws Exception {
    if (SignalStore.misc().isClientDeprecated()) {
      throw new TextSecureExpiredException(String.format("TextSecure expired (build %d, now %d)",
                                                         BuildConfig.BUILD_TIMESTAMP,
                                                         System.currentTimeMillis()));
    }

    Log.i(TAG, "Starting message send attempt");
    onSend();
    Log.i(TAG, "Message send completed");
  }

  protected abstract void onSend() throws Exception;

  protected static void markAttachmentsUploaded(long messageId, @NonNull OutgoingMediaMessage message) {
    List<Attachment> attachments = new LinkedList<>();

    attachments.addAll(message.getAttachments());
    attachments.addAll(Stream.of(message.getLinkPreviews()).map(lp -> lp.getThumbnail().orElse(null)).withoutNulls().toList());
    attachments.addAll(Stream.of(message.getSharedContacts()).map(Contact::getAvatarAttachment).withoutNulls().toList());

    if (message.getOutgoingQuote() != null) {
      attachments.addAll(message.getOutgoingQuote().getAttachments());
    }

    AttachmentDatabase database = SignalDatabase.attachments();

    for (Attachment attachment : attachments) {
      database.markAttachmentUploaded(messageId, attachment);
    }
  }

  protected String buildAttachmentString(@NonNull List<Attachment> attachments) {
    List<String> strings = attachments.stream().map(attachment -> {
      if (attachment instanceof DatabaseAttachment) {
        return ((DatabaseAttachment) attachment).getAttachmentId().toString();
      } else if (attachment.getUri() != null) {
        return attachment.getUri().toString();
      } else {
        return attachment.toString();
      }
    }).collect(Collectors.toList());

    return Util.join(strings, ", ");
  }
}
