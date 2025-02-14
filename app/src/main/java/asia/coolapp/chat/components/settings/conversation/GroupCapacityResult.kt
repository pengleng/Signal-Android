package asia.coolapp.chat.components.settings.conversation

import asia.coolapp.chat.ContactSelectionListFragment
import asia.coolapp.chat.groups.SelectionLimits
import asia.coolapp.chat.recipients.RecipientId

class GroupCapacityResult(
  private val selfId: RecipientId,
  private val members: List<RecipientId>,
  private val selectionLimits: SelectionLimits,
  val isAnnouncementGroup: Boolean
) {
  fun getMembers(): List<RecipientId?> {
    return members
  }

  fun getSelectionLimit(): Int {
    if (!selectionLimits.hasHardLimit()) {
      return ContactSelectionListFragment.NO_LIMIT
    }
    val containsSelf = members.indexOf(selfId) != -1
    return selectionLimits.hardLimit - if (containsSelf) 1 else 0
  }

  fun getSelectionWarning(): Int {
    if (!selectionLimits.hasRecommendedLimit()) {
      return ContactSelectionListFragment.NO_LIMIT
    }

    val containsSelf = members.indexOf(selfId) != -1
    return selectionLimits.recommendedLimit - if (containsSelf) 1 else 0
  }

  fun getRemainingCapacity(): Int {
    return selectionLimits.hardLimit - members.size
  }

  fun getMembersWithoutSelf(): List<RecipientId> {
    val recipientIds = ArrayList<RecipientId>(members.size)
    for (recipientId in members) {
      if (recipientId != selfId) {
        recipientIds.add(recipientId)
      }
    }
    return recipientIds
  }
}
