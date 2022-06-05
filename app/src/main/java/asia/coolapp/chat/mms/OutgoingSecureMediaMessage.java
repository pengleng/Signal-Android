package asia.coolapp.chat.mms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import asia.coolapp.chat.attachments.Attachment;
import asia.coolapp.chat.contactshare.Contact;
import asia.coolapp.chat.database.model.Mention;
import asia.coolapp.chat.database.model.ParentStoryId;
import asia.coolapp.chat.database.model.StoryType;
import asia.coolapp.chat.linkpreview.LinkPreview;
import asia.coolapp.chat.recipients.Recipient;

import java.util.Collections;
import java.util.List;

public class OutgoingSecureMediaMessage extends OutgoingMediaMessage {

  public OutgoingSecureMediaMessage(Recipient recipient,
                                    String body,
                                    List<Attachment> attachments,
                                    long sentTimeMillis,
                                    int distributionType,
                                    long expiresIn,
                                    boolean viewOnce,
                                    @NonNull StoryType storyType,
                                    @Nullable ParentStoryId parentStoryId,
                                    boolean isStoryReaction,
                                    @Nullable QuoteModel quote,
                                    @NonNull List<Contact> contacts,
                                    @NonNull List<LinkPreview> previews,
                                    @NonNull List<Mention> mentions)
  {
    super(recipient, body, attachments, sentTimeMillis, -1, expiresIn, viewOnce, distributionType, storyType, parentStoryId, isStoryReaction, quote, contacts, previews, mentions, Collections.emptySet(), Collections.emptySet());
  }

  public OutgoingSecureMediaMessage(OutgoingMediaMessage base) {
    super(base);
  }

  @Override
  public boolean isSecure() {
    return true;
  }

  @Override
  public @NonNull OutgoingMediaMessage withExpiry(long expiresIn) {
    return new OutgoingSecureMediaMessage(getRecipient(),
                                          getBody(),
                                          getAttachments(),
                                          getSentTimeMillis(),
                                          getDistributionType(),
                                          expiresIn,
                                          isViewOnce(),
                                          getStoryType(),
                                          getParentStoryId(),
                                          isStoryReaction(),
                                          getOutgoingQuote(),
                                          getSharedContacts(),
                                          getLinkPreviews(),
                                          getMentions());
  }

  public @NonNull OutgoingSecureMediaMessage withSentTimestamp(long sentTimestamp) {
    return new OutgoingSecureMediaMessage(getRecipient(),
                                          getBody(),
                                          getAttachments(),
                                          sentTimestamp,
                                          getDistributionType(),
                                          getExpiresIn(),
                                          isViewOnce(),
                                          getStoryType(),
                                          getParentStoryId(),
                                          isStoryReaction(),
                                          getOutgoingQuote(),
                                          getSharedContacts(),
                                          getLinkPreviews(),
                                          getMentions());
  }
}
