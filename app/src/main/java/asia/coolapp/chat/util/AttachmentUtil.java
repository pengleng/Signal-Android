package asia.coolapp.chat.util;


import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.attachments.AttachmentId;
import asia.coolapp.chat.attachments.DatabaseAttachment;
import asia.coolapp.chat.database.NoSuchMessageException;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.model.MessageRecord;
import asia.coolapp.chat.jobmanager.impl.NotInCallConstraint;
import asia.coolapp.chat.recipients.Recipient;

import java.util.Collections;
import java.util.Set;

public class AttachmentUtil {

  private static final String TAG = Log.tag(AttachmentUtil.class);

  @WorkerThread
  public static boolean isAutoDownloadPermitted(@NonNull Context context, @Nullable DatabaseAttachment attachment) {
    if (attachment == null) {
      Log.w(TAG, "attachment was null, returning vacuous true");
      return true;
    }

    if (!isFromTrustedConversation(context, attachment)) {
      return false;
    }

    Set<String> allowedTypes = getAllowedAutoDownloadTypes(context);
    String      contentType  = attachment.getContentType();

    if (attachment.isVoiceNote()                                                       ||
        (MediaUtil.isAudio(attachment) && TextUtils.isEmpty(attachment.getFileName())) ||
        MediaUtil.isLongTextType(attachment.getContentType())                          ||
        attachment.isSticker())
    {
      return true;
    } else if (attachment.isVideoGif()) {
      return NotInCallConstraint.isNotInConnectedCall() && allowedTypes.contains("image");
    } else if (isNonDocumentType(contentType)) {
      return NotInCallConstraint.isNotInConnectedCall() && allowedTypes.contains(MediaUtil.getDiscreteMimeType(contentType));
    } else {
      return NotInCallConstraint.isNotInConnectedCall() && allowedTypes.contains("documents");
    }
  }

  /**
   * Deletes the specified attachment. If its the only attachment for its linked message, the entire
   * message is deleted.
   */
  @WorkerThread
  public static void deleteAttachment(@NonNull Context context,
                                      @NonNull DatabaseAttachment attachment)
  {
    AttachmentId attachmentId    = attachment.getAttachmentId();
    long         mmsId           = attachment.getMmsId();
    int          attachmentCount = SignalDatabase.attachments()
                                                 .getAttachmentsForMessage(mmsId)
                                                 .size();

    if (attachmentCount <= 1) {
      SignalDatabase.mms().deleteMessage(mmsId);
    } else {
      SignalDatabase.attachments().deleteAttachment(attachmentId);
    }
  }

  private static boolean isNonDocumentType(String contentType) {
    return
        MediaUtil.isImageType(contentType) ||
        MediaUtil.isVideoType(contentType) ||
        MediaUtil.isAudioType(contentType);
  }

  private static @NonNull Set<String> getAllowedAutoDownloadTypes(@NonNull Context context) {
    if      (NetworkUtil.isConnectedWifi(context))    return TextSecurePreferences.getWifiMediaDownloadAllowed(context);
    else if (NetworkUtil.isConnectedRoaming(context)) return TextSecurePreferences.getRoamingMediaDownloadAllowed(context);
    else if (NetworkUtil.isConnectedMobile(context))  return TextSecurePreferences.getMobileMediaDownloadAllowed(context);
    else                                              return Collections.emptySet();
  }

  @WorkerThread
  private static boolean isFromTrustedConversation(@NonNull Context context, @NonNull DatabaseAttachment attachment) {
    try {
      MessageRecord message = SignalDatabase.mms().getMessageRecord(attachment.getMmsId());

      Recipient individualRecipient = message.getRecipient();
      Recipient threadRecipient     = SignalDatabase.threads().getRecipientForThreadId(message.getThreadId());

      if (threadRecipient != null && threadRecipient.isGroup()) {
        return threadRecipient.isProfileSharing() || isTrustedIndividual(individualRecipient, message);
      } else {
        return isTrustedIndividual(individualRecipient, message);
      }
    } catch (NoSuchMessageException e) {
      Log.w(TAG, "Message could not be found! Assuming not a trusted contact.");
      return false;
    }
  }

  private static boolean isTrustedIndividual(@NonNull Recipient recipient, @NonNull MessageRecord message) {
    return recipient.isSystemContact()  ||
           recipient.isProfileSharing() ||
           message.isOutgoing()         ||
           recipient.isSelf()           ||
           recipient.isReleaseNotes();
    }
  }
