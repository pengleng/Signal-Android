package asia.coolapp.chat.contacts.paged

import android.content.Context
import android.database.Cursor
import org.signal.core.util.CursorUtil
import asia.coolapp.chat.R
import asia.coolapp.chat.contacts.ContactRepository
import asia.coolapp.chat.database.DistributionListDatabase
import asia.coolapp.chat.database.GroupDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.ThreadDatabase
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId

/**
 * Database boundary interface which allows us to safely unit test the data source without
 * having to deal with database access.
 */
open class ContactSearchPagedDataSourceRepository(
  private val context: Context
) {

  private val contactRepository = ContactRepository(context, context.getString(R.string.note_to_self))

  open fun querySignalContacts(query: String?, includeSelf: Boolean): Cursor? {
    return contactRepository.querySignalContacts(query ?: "", includeSelf)
  }

  open fun queryNonSignalContacts(query: String?): Cursor? {
    return contactRepository.queryNonSignalContacts(query ?: "")
  }

  open fun queryNonGroupContacts(query: String?, includeSelf: Boolean): Cursor? {
    return contactRepository.queryNonGroupContacts(query ?: "", includeSelf)
  }

  open fun getGroupContacts(section: ContactSearchConfiguration.Section.Groups, query: String?): Cursor? {
    return SignalDatabase.groups.getGroupsFilteredByTitle(query ?: "", section.includeInactive, !section.includeV1, !section.includeMms).cursor
  }

  open fun getRecents(section: ContactSearchConfiguration.Section.Recents): Cursor? {
    return SignalDatabase.threads.getRecentConversationList(
      section.limit,
      section.includeInactiveGroups,
      section.groupsOnly,
      !section.includeGroupsV1,
      !section.includeSms
    )
  }

  open fun getStories(query: String?): Cursor? {
    return SignalDatabase.distributionLists.getAllListsForContactSelectionUiCursor(query, myStoryContainsQuery(query ?: ""))
  }

  open fun getRecipientFromDistributionListCursor(cursor: Cursor): Recipient {
    return Recipient.resolved(RecipientId.from(CursorUtil.requireLong(cursor, DistributionListDatabase.RECIPIENT_ID)))
  }

  open fun getRecipientFromThreadCursor(cursor: Cursor): Recipient {
    return Recipient.resolved(RecipientId.from(CursorUtil.requireLong(cursor, ThreadDatabase.RECIPIENT_ID)))
  }

  open fun getRecipientFromRecipientCursor(cursor: Cursor): Recipient {
    return Recipient.resolved(RecipientId.from(CursorUtil.requireLong(cursor, ContactRepository.ID_COLUMN)))
  }

  open fun getRecipientFromGroupCursor(cursor: Cursor): Recipient {
    return Recipient.resolved(RecipientId.from(CursorUtil.requireLong(cursor, GroupDatabase.RECIPIENT_ID)))
  }

  open fun getDistributionListMembershipCount(recipient: Recipient): Int {
    return SignalDatabase.distributionLists.getMemberCount(recipient.requireDistributionListId())
  }

  open fun getGroupStories(): Set<ContactSearchData.Story> {
    return SignalDatabase.groups.groupsToDisplayAsStories.map {
      val recipient = Recipient.resolved(SignalDatabase.recipients.getOrInsertFromGroupId(it))
      ContactSearchData.Story(recipient, recipient.participants.size)
    }.toSet()
  }

  open fun recipientNameContainsQuery(recipient: Recipient, query: String?): Boolean {
    return query.isNullOrBlank() || recipient.getDisplayName(context).contains(query)
  }

  open fun myStoryContainsQuery(query: String): Boolean {
    if (query.isEmpty()) {
      return true
    }

    val myStory = context.getString(R.string.Recipient_my_story)
    return myStory.contains(query)
  }
}
