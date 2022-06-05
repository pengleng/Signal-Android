package asia.coolapp.chat.mms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import asia.coolapp.chat.attachments.Attachment;
import asia.coolapp.chat.contactshare.Contact;
import asia.coolapp.chat.database.ThreadDatabase;
import asia.coolapp.chat.database.model.Mention;
import asia.coolapp.chat.database.model.StoryType;
import asia.coolapp.chat.database.model.databaseprotos.DecryptedGroupV2Context;
import asia.coolapp.chat.linkpreview.LinkPreview;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.sms.GroupV2UpdateMessageUtil;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

import java.util.Collections;
import java.util.List;

public final class OutgoingGroupUpdateMessage extends OutgoingSecureMediaMessage {

  private final MessageGroupContext messageGroupContext;

  public OutgoingGroupUpdateMessage(@NonNull Recipient recipient,
                                    @NonNull MessageGroupContext groupContext,
                                    @NonNull List<Attachment> avatar,
                                    long sentTimeMillis,
                                    long expiresIn,
                                    boolean viewOnce,
                                    @Nullable QuoteModel quote,
                                    @NonNull List<Contact> contacts,
                                    @NonNull List<LinkPreview> previews,
                                    @NonNull List<Mention> mentions)
  {
    super(recipient,
          groupContext.getEncodedGroupContext(),
          avatar,
          sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION,
          expiresIn,
          viewOnce,
          StoryType.NONE,
          null,
          false,
          quote,
          contacts,
          previews,
          mentions);

    this.messageGroupContext = groupContext;
  }

  public OutgoingGroupUpdateMessage(@NonNull Recipient recipient,
                                    @NonNull GroupContext group,
                                    @Nullable final Attachment avatar,
                                    long sentTimeMillis,
                                    long expireIn,
                                    boolean viewOnce,
                                    @Nullable QuoteModel quote,
                                    @NonNull List<Contact> contacts,
                                    @NonNull List<LinkPreview> previews,
                                    @NonNull List<Mention> mentions)
  {
    this(recipient, new MessageGroupContext(group), getAttachments(avatar), sentTimeMillis, expireIn, viewOnce, quote, contacts, previews, mentions);
  }

  public OutgoingGroupUpdateMessage(@NonNull Recipient recipient,
                                    @NonNull DecryptedGroupV2Context group,
                                    @Nullable final Attachment avatar,
                                    long sentTimeMillis,
                                    long expireIn,
                                    boolean viewOnce,
                                    @Nullable QuoteModel quote,
                                    @NonNull List<Contact> contacts,
                                    @NonNull List<LinkPreview> previews,
                                    @NonNull List<Mention> mentions)
  {
    this(recipient, new MessageGroupContext(group), getAttachments(avatar), sentTimeMillis, expireIn, viewOnce, quote, contacts, previews, mentions);
  }

  @Override
  public boolean isGroup() {
    return true;
  }

  public boolean isV2Group() {
    return GroupV2UpdateMessageUtil.isGroupV2(messageGroupContext);
  }

  public boolean isJustAGroupLeave() {
    return GroupV2UpdateMessageUtil.isJustAGroupLeave(messageGroupContext);
  }

  public @NonNull MessageGroupContext.GroupV1Properties requireGroupV1Properties() {
    return messageGroupContext.requireGroupV1Properties();
  }

  public @NonNull MessageGroupContext.GroupV2Properties requireGroupV2Properties() {
    return messageGroupContext.requireGroupV2Properties();
  }

  private static List<Attachment> getAttachments(@Nullable Attachment avatar) {
    return avatar == null ? Collections.emptyList() : Collections.singletonList(avatar);
  }
}
