package asia.coolapp.chat.database.model

import android.net.Uri
import org.signal.libsignal.zkgroup.groups.GroupMasterKey
import org.signal.libsignal.zkgroup.profiles.ProfileKeyCredential
import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.conversation.colors.AvatarColor
import asia.coolapp.chat.conversation.colors.ChatColors
import asia.coolapp.chat.database.IdentityDatabase.VerifiedStatus
import asia.coolapp.chat.database.RecipientDatabase
import asia.coolapp.chat.database.RecipientDatabase.InsightsBannerTier
import asia.coolapp.chat.database.RecipientDatabase.MentionSetting
import asia.coolapp.chat.database.RecipientDatabase.RegisteredState
import asia.coolapp.chat.database.RecipientDatabase.UnidentifiedAccessMode
import asia.coolapp.chat.database.RecipientDatabase.VibrateState
import asia.coolapp.chat.groups.GroupId
import asia.coolapp.chat.profiles.ProfileName
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.wallpaper.ChatWallpaper
import org.whispersystems.signalservice.api.push.PNI
import org.whispersystems.signalservice.api.push.ServiceId
import java.util.Optional

/**
 * Database model for [RecipientDatabase].
 */
data class RecipientRecord(
  val id: RecipientId,
  val serviceId: ServiceId?,
  val pni: PNI?,
  val username: String?,
  val e164: String?,
  val email: String?,
  val groupId: GroupId?,
  val distributionListId: DistributionListId?,
  val groupType: RecipientDatabase.GroupType,
  val isBlocked: Boolean,
  val muteUntil: Long,
  val messageVibrateState: VibrateState,
  val callVibrateState: VibrateState,
  val messageRingtone: Uri?,
  val callRingtone: Uri?,
  private val defaultSubscriptionId: Int,
  val expireMessages: Int,
  val registered: RegisteredState,
  val profileKey: ByteArray?,
  val profileKeyCredential: ProfileKeyCredential?,
  val systemProfileName: ProfileName,
  val systemDisplayName: String?,
  val systemContactPhotoUri: String?,
  val systemPhoneLabel: String?,
  val systemContactUri: String?,
  @get:JvmName("getProfileName")
  val signalProfileName: ProfileName,
  @get:JvmName("getProfileAvatar")
  val signalProfileAvatar: String?,
  @get:JvmName("hasProfileImage")
  val hasProfileImage: Boolean,
  @get:JvmName("isProfileSharing")
  val profileSharing: Boolean,
  val lastProfileFetch: Long,
  val notificationChannel: String?,
  val unidentifiedAccessMode: UnidentifiedAccessMode,
  @get:JvmName("isForceSmsSelection")
  val forceSmsSelection: Boolean,
  val rawCapabilities: Long,
  val groupsV1MigrationCapability: Recipient.Capability,
  val senderKeyCapability: Recipient.Capability,
  val announcementGroupCapability: Recipient.Capability,
  val changeNumberCapability: Recipient.Capability,
  val storiesCapability: Recipient.Capability,
  val insightsBannerTier: InsightsBannerTier,
  val storageId: ByteArray?,
  val mentionSetting: MentionSetting,
  val wallpaper: ChatWallpaper?,
  val chatColors: ChatColors?,
  val avatarColor: AvatarColor,
  val about: String?,
  val aboutEmoji: String?,
  val syncExtras: SyncExtras,
  val extras: Recipient.Extras?,
  @get:JvmName("hasGroupsInCommon")
  val hasGroupsInCommon: Boolean,
  val badges: List<Badge>
) {

  fun getDefaultSubscriptionId(): Optional<Int> {
    return if (defaultSubscriptionId != -1) Optional.of(defaultSubscriptionId) else Optional.empty()
  }

  /**
   * A bundle of data that's only necessary when syncing to storage service, not for a
   * [Recipient].
   */
  data class SyncExtras(
    val storageProto: ByteArray?,
    val groupMasterKey: GroupMasterKey?,
    val identityKey: ByteArray?,
    val identityStatus: VerifiedStatus,
    val isArchived: Boolean,
    val isForcedUnread: Boolean
  )
}
