package asia.coolapp.chat.conversation.colors

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.annimon.stream.Stream
import org.signal.core.util.MapUtil
import asia.coolapp.chat.conversation.colors.ChatColorsPalette.Names.all
import asia.coolapp.chat.groups.GroupId
import asia.coolapp.chat.groups.LiveGroup
import asia.coolapp.chat.groups.ui.GroupMemberEntry.FullMember
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.util.DefaultValueLiveData
import java.util.Optional

object NameColors {

  fun createSessionMembersCache(): MutableMap<GroupId, Set<Recipient>> {
    return mutableMapOf()
  }

  fun getNameColorsMapLiveData(
    recipientId: LiveData<RecipientId>,
    sessionMemberCache: MutableMap<GroupId, Set<Recipient>>
  ): LiveData<Map<RecipientId, NameColor>> {
    val recipient = Transformations.switchMap(recipientId) { r: RecipientId? -> Recipient.live(r!!).liveData }
    val group = Transformations.map(recipient) { obj: Recipient -> obj.groupId }
    val groupMembers = Transformations.switchMap(group) { g: Optional<GroupId> ->
      g.map { groupId: GroupId -> this.getSessionGroupRecipients(groupId, sessionMemberCache) }
        .orElseGet { DefaultValueLiveData(emptySet()) }
    }
    return Transformations.map(groupMembers) { members: Set<Recipient>? ->
      val sorted = Stream.of(members)
        .filter { member: Recipient? -> member != Recipient.self() }
        .sortBy { obj: Recipient -> obj.requireStringId() }
        .toList()
      val names = all
      val colors: MutableMap<RecipientId, NameColor> = HashMap()
      for (i in sorted.indices) {
        colors[sorted[i].id] = names[i % names.size]
      }
      colors
    }
  }

  private fun getSessionGroupRecipients(groupId: GroupId, sessionMemberCache: MutableMap<GroupId, Set<Recipient>>): LiveData<Set<Recipient>> {
    val fullMembers = Transformations.map(
      LiveGroup(groupId).fullMembers
    ) { members: List<FullMember>? ->
      Stream.of(members)
        .map { it.member }
        .toList()
    }
    return Transformations.map(fullMembers) { currentMembership: List<Recipient>? ->
      val cachedMembers: MutableSet<Recipient> = MapUtil.getOrDefault(sessionMemberCache, groupId, HashSet()).toMutableSet()
      cachedMembers.addAll(currentMembership!!)
      sessionMemberCache[groupId] = cachedMembers
      cachedMembers
    }
  }
}
