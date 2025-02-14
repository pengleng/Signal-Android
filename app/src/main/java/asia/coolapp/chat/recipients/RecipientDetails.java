package asia.coolapp.chat.recipients;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.libsignal.zkgroup.profiles.ProfileKeyCredential;
import asia.coolapp.chat.badges.models.Badge;
import asia.coolapp.chat.conversation.colors.AvatarColor;
import asia.coolapp.chat.conversation.colors.ChatColors;
import asia.coolapp.chat.database.RecipientDatabase.InsightsBannerTier;
import asia.coolapp.chat.database.RecipientDatabase.MentionSetting;
import asia.coolapp.chat.database.RecipientDatabase.RegisteredState;
import asia.coolapp.chat.database.RecipientDatabase.UnidentifiedAccessMode;
import asia.coolapp.chat.database.RecipientDatabase.VibrateState;
import asia.coolapp.chat.database.model.DistributionListId;
import asia.coolapp.chat.database.model.RecipientRecord;
import asia.coolapp.chat.groups.GroupId;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.profiles.ProfileName;
import asia.coolapp.chat.util.TextSecurePreferences;
import asia.coolapp.chat.util.Util;
import asia.coolapp.chat.wallpaper.ChatWallpaper;
import org.whispersystems.signalservice.api.push.PNI;
import org.whispersystems.signalservice.api.push.ServiceId;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class RecipientDetails {

  final ServiceId                  serviceId;
  final PNI                        pni;
  final String                     username;
  final String                     e164;
  final String                     email;
  final GroupId                    groupId;
  final DistributionListId         distributionListId;
  final String                     groupName;
  final String                     systemContactName;
  final String                     customLabel;
  final Uri                        systemContactPhoto;
  final Uri                        contactUri;
  final Optional<Long>             groupAvatarId;
  final Uri                        messageRingtone;
  final Uri                        callRingtone;
  final long                       mutedUntil;
  final VibrateState               messageVibrateState;
  final VibrateState               callVibrateState;
  final boolean                    blocked;
  final int                        expireMessages;
  final List<Recipient>            participants;
  final ProfileName                profileName;
  final Optional<Integer>          defaultSubscriptionId;
  final RegisteredState            registered;
  final byte[]                     profileKey;
  final ProfileKeyCredential       profileKeyCredential;
  final String                     profileAvatar;
  final boolean                    hasProfileImage;
  final boolean                    profileSharing;
  final long                       lastProfileFetch;
  final boolean                    systemContact;
  final boolean                    isSelf;
  final String                     notificationChannel;
  final UnidentifiedAccessMode     unidentifiedAccessMode;
  final boolean                    forceSmsSelection;
  final Recipient.Capability       groupsV1MigrationCapability;
  final Recipient.Capability       senderKeyCapability;
  final Recipient.Capability       announcementGroupCapability;
  final Recipient.Capability       changeNumberCapability;
  final Recipient.Capability       storiesCapability;
  final InsightsBannerTier         insightsBannerTier;
  final byte[]                     storageId;
  final MentionSetting             mentionSetting;
  final ChatWallpaper              wallpaper;
  final ChatColors                 chatColors;
  final AvatarColor                avatarColor;
  final String                     about;
  final String                     aboutEmoji;
  final ProfileName                systemProfileName;
  final Optional<Recipient.Extras> extras;
  final boolean                    hasGroupsInCommon;
  final List<Badge>                badges;
  final boolean                    isReleaseChannel;

  public RecipientDetails(@Nullable String groupName,
                          @Nullable String systemContactName,
                          @NonNull Optional<Long> groupAvatarId,
                          boolean systemContact,
                          boolean isSelf,
                          @NonNull RegisteredState registeredState,
                          @NonNull RecipientRecord record,
                          @Nullable List<Recipient> participants,
                          boolean isReleaseChannel)
  {
    this.groupAvatarId               = groupAvatarId;
    this.systemContactPhoto          = Util.uri(record.getSystemContactPhotoUri());
    this.customLabel                 = record.getSystemPhoneLabel();
    this.contactUri                  = Util.uri(record.getSystemContactUri());
    this.serviceId                   = record.getServiceId();
    this.pni                         = record.getPni();
    this.username                    = record.getUsername();
    this.e164                        = record.getE164();
    this.email                       = record.getEmail();
    this.groupId                     = record.getGroupId();
    this.distributionListId          = record.getDistributionListId();
    this.messageRingtone             = record.getMessageRingtone();
    this.callRingtone                = record.getCallRingtone();
    this.mutedUntil                  = record.getMuteUntil();
    this.messageVibrateState         = record.getMessageVibrateState();
    this.callVibrateState            = record.getCallVibrateState();
    this.blocked                     = record.isBlocked();
    this.expireMessages              = record.getExpireMessages();
    this.participants                = participants == null ? new LinkedList<>() : participants;
    this.profileName                 = record.getProfileName();
    this.defaultSubscriptionId       = record.getDefaultSubscriptionId();
    this.registered                  = registeredState;
    this.profileKey                  = record.getProfileKey();
    this.profileKeyCredential        = record.getProfileKeyCredential();
    this.profileAvatar               = record.getProfileAvatar();
    this.hasProfileImage             = record.hasProfileImage();
    this.profileSharing              = record.isProfileSharing();
    this.lastProfileFetch            = record.getLastProfileFetch();
    this.systemContact               = systemContact;
    this.isSelf                      = isSelf;
    this.notificationChannel         = record.getNotificationChannel();
    this.unidentifiedAccessMode      = record.getUnidentifiedAccessMode();
    this.forceSmsSelection           = record.isForceSmsSelection();
    this.groupsV1MigrationCapability = record.getGroupsV1MigrationCapability();
    this.senderKeyCapability         = record.getSenderKeyCapability();
    this.announcementGroupCapability = record.getAnnouncementGroupCapability();
    this.changeNumberCapability      = record.getChangeNumberCapability();
    this.storiesCapability           = record.getStoriesCapability();
    this.insightsBannerTier          = record.getInsightsBannerTier();
    this.storageId                   = record.getStorageId();
    this.mentionSetting              = record.getMentionSetting();
    this.wallpaper                   = record.getWallpaper();
    this.chatColors                  = record.getChatColors();
    this.avatarColor                 = record.getAvatarColor();
    this.about                       = record.getAbout();
    this.aboutEmoji                  = record.getAboutEmoji();
    this.systemProfileName           = record.getSystemProfileName();
    this.groupName                   = groupName;
    this.systemContactName           = systemContactName;
    this.extras                      = Optional.ofNullable(record.getExtras());
    this.hasGroupsInCommon           = record.hasGroupsInCommon();
    this.badges                      = record.getBadges();
    this.isReleaseChannel            = isReleaseChannel;
  }

  private RecipientDetails() {
    this.groupAvatarId               = null;
    this.systemContactPhoto          = null;
    this.customLabel                 = null;
    this.contactUri                  = null;
    this.serviceId                   = null;
    this.pni                         = null;
    this.username                    = null;
    this.e164                        = null;
    this.email                       = null;
    this.groupId                     = null;
    this.distributionListId          = null;
    this.messageRingtone             = null;
    this.callRingtone                = null;
    this.mutedUntil                  = 0;
    this.messageVibrateState         = VibrateState.DEFAULT;
    this.callVibrateState            = VibrateState.DEFAULT;
    this.blocked                     = false;
    this.expireMessages              = 0;
    this.participants                = new LinkedList<>();
    this.profileName                 = ProfileName.EMPTY;
    this.insightsBannerTier          = InsightsBannerTier.TIER_TWO;
    this.defaultSubscriptionId       = Optional.empty();
    this.registered                  = RegisteredState.UNKNOWN;
    this.profileKey                  = null;
    this.profileKeyCredential        = null;
    this.profileAvatar               = null;
    this.hasProfileImage             = false;
    this.profileSharing              = false;
    this.lastProfileFetch            = 0;
    this.systemContact               = true;
    this.isSelf                      = false;
    this.notificationChannel         = null;
    this.unidentifiedAccessMode      = UnidentifiedAccessMode.UNKNOWN;
    this.forceSmsSelection           = false;
    this.groupName                   = null;
    this.groupsV1MigrationCapability = Recipient.Capability.UNKNOWN;
    this.senderKeyCapability         = Recipient.Capability.UNKNOWN;
    this.announcementGroupCapability = Recipient.Capability.UNKNOWN;
    this.changeNumberCapability      = Recipient.Capability.UNKNOWN;
    this.storiesCapability           = Recipient.Capability.UNKNOWN;
    this.storageId                   = null;
    this.mentionSetting              = MentionSetting.ALWAYS_NOTIFY;
    this.wallpaper                   = null;
    this.chatColors                  = null;
    this.avatarColor                 = AvatarColor.UNKNOWN;
    this.about                       = null;
    this.aboutEmoji                  = null;
    this.systemProfileName           = ProfileName.EMPTY;
    this.systemContactName           = null;
    this.extras                      = Optional.empty();
    this.hasGroupsInCommon           = false;
    this.badges                      = Collections.emptyList();
    this.isReleaseChannel            = false;
  }

  public static @NonNull RecipientDetails forIndividual(@NonNull Context context, @NonNull RecipientRecord settings) {
    boolean systemContact    = !settings.getSystemProfileName().isEmpty();
    boolean isSelf           = (settings.getE164() != null && settings.getE164().equals(SignalStore.account().getE164())) ||
                               (settings.getServiceId() != null && settings.getServiceId().equals(SignalStore.account().getAci()));
    boolean isReleaseChannel = settings.getId().equals(SignalStore.releaseChannelValues().getReleaseChannelRecipientId());

    RegisteredState registeredState = settings.getRegistered();

    if (isSelf) {
      if (SignalStore.account().isRegistered() && !TextSecurePreferences.isUnauthorizedRecieved(context)) {
        registeredState = RegisteredState.REGISTERED;
      } else {
        registeredState = RegisteredState.NOT_REGISTERED;
      }
    }

    return new RecipientDetails(null, settings.getSystemDisplayName(), Optional.empty(), systemContact, isSelf, registeredState, settings, null, isReleaseChannel);
  }

  public static @NonNull RecipientDetails forDistributionList(String title, @Nullable List<Recipient> members, @NonNull RecipientRecord record) {
    return new RecipientDetails(title, null, Optional.empty(), false, false, record.getRegistered(), record, members, false);
  }

  public static @NonNull RecipientDetails forUnknown() {
    return new RecipientDetails();
  }
}
