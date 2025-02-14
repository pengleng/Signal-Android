package asia.coolapp.chat.database.model;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import asia.coolapp.chat.contactshare.Contact;
import asia.coolapp.chat.database.documents.IdentityKeyMismatch;
import asia.coolapp.chat.database.documents.NetworkFailure;
import asia.coolapp.chat.linkpreview.LinkPreview;
import asia.coolapp.chat.mms.Slide;
import asia.coolapp.chat.mms.SlideDeck;
import asia.coolapp.chat.recipients.Recipient;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class MmsMessageRecord extends MessageRecord {

  private final @NonNull  SlideDeck         slideDeck;
  private final @Nullable Quote             quote;
  private final @NonNull  List<Contact>     contacts     = new LinkedList<>();
  private final @NonNull  List<LinkPreview> linkPreviews = new LinkedList<>();
  private final @NonNull  StoryType         storyType;
  private final @Nullable ParentStoryId     parentStoryId;

  private final boolean viewOnce;

  MmsMessageRecord(long id, String body, Recipient conversationRecipient,
                   Recipient individualRecipient, int recipientDeviceId, long dateSent,
                   long dateReceived, long dateServer, long threadId, int deliveryStatus, int deliveryReceiptCount,
                   long type, Set<IdentityKeyMismatch> mismatches,
                   Set<NetworkFailure> networkFailures, int subscriptionId, long expiresIn,
                   long expireStarted, boolean viewOnce,
                   @NonNull SlideDeck slideDeck, int readReceiptCount,
                   @Nullable Quote quote, @NonNull List<Contact> contacts,
                   @NonNull List<LinkPreview> linkPreviews, boolean unidentified,
                   @NonNull List<ReactionRecord> reactions, boolean remoteDelete, long notifiedTimestamp,
                   int viewedReceiptCount, long receiptTimestamp, @NonNull StoryType storyType,
                   @Nullable ParentStoryId parentStoryId)
  {
    super(id, body, conversationRecipient, individualRecipient, recipientDeviceId,
          dateSent, dateReceived, dateServer, threadId, deliveryStatus, deliveryReceiptCount,
          type, mismatches, networkFailures, subscriptionId, expiresIn, expireStarted, readReceiptCount,
          unidentified, reactions, remoteDelete, notifiedTimestamp, viewedReceiptCount, receiptTimestamp);

    this.slideDeck     = slideDeck;
    this.quote         = quote;
    this.viewOnce      = viewOnce;
    this.storyType     = storyType;
    this.parentStoryId = parentStoryId;

    this.contacts.addAll(contacts);
    this.linkPreviews.addAll(linkPreviews);
  }

  @Override
  public boolean isMms() {
    return true;
  }

  @NonNull
  public SlideDeck getSlideDeck() {
    return slideDeck;
  }

  @Override
  public boolean isMediaPending() {
    for (Slide slide : getSlideDeck().getSlides()) {
      if (slide.isInProgress() || slide.isPendingDownload()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isViewOnce() {
    return viewOnce;
  }

  public @NonNull StoryType getStoryType() {
    return storyType;
  }

  public @Nullable ParentStoryId getParentStoryId() {
    return parentStoryId;
  }

  public boolean containsMediaSlide() {
    return slideDeck.containsMediaSlide();
  }

  public @Nullable Quote getQuote() {
    return quote;
  }

  public @NonNull List<Contact> getSharedContacts() {
    return contacts;
  }

  public @NonNull List<LinkPreview> getLinkPreviews() {
    return linkPreviews;
  }
}
