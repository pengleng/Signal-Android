package asia.coolapp.chat.service.webrtc

import org.signal.ringrtc.CallManager
import asia.coolapp.chat.groups.GroupId
import asia.coolapp.chat.recipients.RecipientId
import java.util.UUID

data class GroupCallRingCheckInfo(
  val recipientId: RecipientId,
  val groupId: GroupId.V2,
  val ringId: Long,
  val ringerUuid: UUID,
  val ringUpdate: CallManager.RingUpdate
)
