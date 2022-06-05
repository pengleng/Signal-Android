package asia.coolapp.chat.database

import android.net.Uri
import org.signal.core.util.Bitmask
import org.signal.libsignal.zkgroup.profiles.ProfileKeyCredential
import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.conversation.colors.AvatarColor
import asia.coolapp.chat.conversation.colors.ChatColors
import asia.coolapp.chat.database.model.RecipientRecord
import asia.coolapp.chat.groups.GroupId
import asia.coolapp.chat.profiles.ProfileName
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientDetails
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.wallpaper.ChatWallpaper
import org.whispersystems.signalservice.api.push.ServiceId
import java.util.Optional
import java.util.UUID
import kotlin.random.Random

/**
 * Test utilities to create recipients in different states.
 */
object RecipientDatabaseTestUtils {

  fun createRecipient(
    resolved: Boolean = false,
    groupName: String? = null,
    groupAvatarId: Optional<Long> = Optional.empty(),
    systemContact: Boolean = false,
    isSelf: Boolean = false,
    participants: List<Recipient> = listOf(),
    recipientId: RecipientId = RecipientId.from(Random.nextLong()),
    serviceId: ServiceId? = ServiceId.from(UUID.randomUUID()),
    username: String? = null,
    e164: String? = null,
    email: String? = null,
    groupId: GroupId? = null,
    groupType: RecipientDatabase.GroupType = RecipientDatabase.GroupType.NONE,
    blocked: Boolean = false,
    muteUntil: Long = -1,
    messageVibrateState: RecipientDatabase.VibrateState = RecipientDatabase.VibrateState.DEFAULT,
    callVibrateState: RecipientDatabase.VibrateState = RecipientDatabase.VibrateState.DEFAULT,
    messageRingtone: Uri = Uri.EMPTY,
    callRingtone: Uri = Uri.EMPTY,
    defaultSubscriptionId: Int = 0,
    expireMessages: Int = 0,
    registered: RecipientDatabase.RegisteredState = RecipientDatabase.RegisteredState.REGISTERED,
    profileKey: ByteArray = Random.nextBytes(32),
    profileKeyCredential: ProfileKeyCredential? = null,
    systemProfileName: ProfileName = ProfileName.EMPTY,
    systemDisplayName: String? = null,
    systemContactPhoto: String? = null,
    systemPhoneLabel: String? = null,
    systemContactUri: String? = null,
    signalProfileName: ProfileName = ProfileName.EMPTY,
    signalProfileAvatar: String? = null,
    hasProfileImage: Boolean = false,
    profileSharing: Boolean = false,
    lastProfileFetch: Long = 0L,
    notificationChannel: String? = null,
    unidentifiedAccessMode: RecipientDatabase.UnidentifiedAccessMode = RecipientDatabase.UnidentifiedAccessMode.UNKNOWN,
    forceSmsSelection: Boolean = false,
    capabilities: Long = 0L,
    insightBannerTier: RecipientDatabase.InsightsBannerTier = RecipientDatabase.InsightsBannerTier.NO_TIER,
    storageId: ByteArray? = null,
    mentionSetting: RecipientDatabase.MentionSetting = RecipientDatabase.MentionSetting.ALWAYS_NOTIFY,
    wallpaper: ChatWallpaper? = null,
    chatColors: ChatColors? = null,
    avatarColor: AvatarColor = AvatarColor.A100,
    about: String? = null,
    aboutEmoji: String? = null,
    syncExtras: RecipientRecord.SyncExtras = RecipientRecord.SyncExtras(
      null,
      null,
      null,
      IdentityDatabase.VerifiedStatus.DEFAULT,
      false,
      false
    ),
    extras: Recipient.Extras? = null,
    hasGroupsInCommon: Boolean = false,
    badges: List<Badge> = emptyList(),
    isReleaseChannel: Boolean = false
  ): Recipient = Recipient(
    recipientId,
    RecipientDetails(
      groupName,
      systemDisplayName,
      groupAvatarId,
      systemContact,
      isSelf,
      registered,
      RecipientRecord(
        recipientId,
        serviceId,
        null,
        username,
        e164,
        email,
        groupId,
        null,
        groupType,
        blocked,
        muteUntil,
        messageVibrateState,
        callVibrateState,
        messageRingtone,
        callRingtone,
        defaultSubscriptionId,
        expireMessages,
        registered,
        profileKey,
        profileKeyCredential,
        systemProfileName,
        systemDisplayName,
        systemContactPhoto,
        systemPhoneLabel,
        systemContactUri,
        signalProfileName,
        signalProfileAvatar,
        hasProfileImage,
        profileSharing,
        lastProfileFetch,
        notificationChannel,
        unidentifiedAccessMode,
        forceSmsSelection,
        capabilities,
        Recipient.Capability.deserialize(Bitmask.read(capabilities, RecipientDatabase.Capabilities.GROUPS_V1_MIGRATION, RecipientDatabase.Capabilities.BIT_LENGTH).toInt()),
        Recipient.Capability.deserialize(Bitmask.read(capabilities, RecipientDatabase.Capabilities.SENDER_KEY, RecipientDatabase.Capabilities.BIT_LENGTH).toInt()),
        Recipient.Capability.deserialize(Bitmask.read(capabilities, RecipientDatabase.Capabilities.ANNOUNCEMENT_GROUPS, RecipientDatabase.Capabilities.BIT_LENGTH).toInt()),
        Recipient.Capability.deserialize(Bitmask.read(capabilities, RecipientDatabase.Capabilities.CHANGE_NUMBER, RecipientDatabase.Capabilities.BIT_LENGTH).toInt()),
        Recipient.Capability.deserialize(Bitmask.read(capabilities, RecipientDatabase.Capabilities.STORIES, RecipientDatabase.Capabilities.BIT_LENGTH).toInt()),
        insightBannerTier,
        storageId,
        mentionSetting,
        wallpaper,
        chatColors,
        avatarColor,
        about,
        aboutEmoji,
        syncExtras,
        extras,
        hasGroupsInCommon,
        badges
      ),
      participants,
      isReleaseChannel
    ),
    resolved
  )
}
