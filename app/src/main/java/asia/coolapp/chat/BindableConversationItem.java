package asia.coolapp.chat;

import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import asia.coolapp.chat.components.voice.VoiceNotePlaybackState;
import asia.coolapp.chat.contactshare.Contact;
import asia.coolapp.chat.conversation.ConversationMessage;
import asia.coolapp.chat.conversation.colors.Colorizable;
import asia.coolapp.chat.conversation.colors.Colorizer;
import asia.coolapp.chat.conversation.mutiselect.MultiselectPart;
import asia.coolapp.chat.conversation.mutiselect.Multiselectable;
import asia.coolapp.chat.database.model.InMemoryMessageRecord;
import asia.coolapp.chat.database.model.MessageRecord;
import asia.coolapp.chat.database.model.MmsMessageRecord;
import asia.coolapp.chat.giph.mp4.GiphyMp4Playable;
import asia.coolapp.chat.groups.GroupId;
import asia.coolapp.chat.groups.GroupMigrationMembershipChange;
import asia.coolapp.chat.linkpreview.LinkPreview;
import asia.coolapp.chat.mms.GlideRequests;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.stickers.StickerLocator;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public interface BindableConversationItem extends Unbindable, GiphyMp4Playable, Colorizable, Multiselectable {
  void bind(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull ConversationMessage messageRecord,
            @NonNull Optional<MessageRecord> previousMessageRecord,
            @NonNull Optional<MessageRecord> nextMessageRecord,
            @NonNull GlideRequests glideRequests,
            @NonNull Locale locale,
            @NonNull Set<MultiselectPart> batchSelected,
            @NonNull Recipient recipients,
            @Nullable String searchQuery,
            boolean pulseMention,
            boolean hasWallpaper,
            boolean isMessageRequestAccepted,
            boolean canPlayInline,
            @NonNull Colorizer colorizer);

  @NonNull ConversationMessage getConversationMessage();

  void setEventListener(@Nullable EventListener listener);

  default void updateTimestamps() {
    // Intentionally Blank.
  }

  default void updateContactNameColor() {
    // Intentionally Blank.
  }

  interface EventListener {
    void onQuoteClicked(MmsMessageRecord messageRecord);
    void onLinkPreviewClicked(@NonNull LinkPreview linkPreview);
    void onMoreTextClicked(@NonNull RecipientId conversationRecipientId, long messageId, boolean isMms);
    void onStickerClicked(@NonNull StickerLocator stickerLocator);
    void onViewOnceMessageClicked(@NonNull MmsMessageRecord messageRecord);
    void onSharedContactDetailsClicked(@NonNull Contact contact, @NonNull View avatarTransitionView);
    void onAddToContactsClicked(@NonNull Contact contact);
    void onMessageSharedContactClicked(@NonNull List<Recipient> choices);
    void onInviteSharedContactClicked(@NonNull List<Recipient> choices);
    void onReactionClicked(@NonNull MultiselectPart multiselectPart, long messageId, boolean isMms);
    void onGroupMemberClicked(@NonNull RecipientId recipientId, @NonNull GroupId groupId);
    void onMessageWithErrorClicked(@NonNull MessageRecord messageRecord);
    void onMessageWithRecaptchaNeededClicked(@NonNull MessageRecord messageRecord);
    void onIncomingIdentityMismatchClicked(@NonNull RecipientId recipientId);
    void onRegisterVoiceNoteCallbacks(@NonNull Observer<VoiceNotePlaybackState> onPlaybackStartObserver);
    void onUnregisterVoiceNoteCallbacks(@NonNull Observer<VoiceNotePlaybackState> onPlaybackStartObserver);
    void onVoiceNotePause(@NonNull Uri uri);
    void onVoiceNotePlay(@NonNull Uri uri, long messageId, double position);
    void onVoiceNoteSeekTo(@NonNull Uri uri, double position);
    void onVoiceNotePlaybackSpeedChanged(@NonNull Uri uri, float speed);
    void onGroupMigrationLearnMoreClicked(@NonNull GroupMigrationMembershipChange membershipChange);
    void onChatSessionRefreshLearnMoreClicked();
    void onBadDecryptLearnMoreClicked(@NonNull RecipientId author);
    void onSafetyNumberLearnMoreClicked(@NonNull Recipient recipient);
    void onJoinGroupCallClicked();
    void onInviteFriendsToGroupClicked(@NonNull GroupId.V2 groupId);
    void onEnableCallNotificationsClicked();
    void onPlayInlineContent(ConversationMessage conversationMessage);
    void onInMemoryMessageClicked(@NonNull InMemoryMessageRecord messageRecord);
    void onViewGroupDescriptionChange(@Nullable GroupId groupId, @NonNull String description, boolean isMessageRequestAccepted);
    void onChangeNumberUpdateContact(@NonNull Recipient recipient);
    void onCallToAction(@NonNull String action);
    void onDonateClicked();
    void onBlockJoinRequest(@NonNull Recipient recipient);

    /** @return true if handled, false if you want to let the normal url handling continue */
    boolean onUrlClicked(@NonNull String url);
  }
}
