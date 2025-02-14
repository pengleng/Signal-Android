package asia.coolapp.chat.search

import asia.coolapp.chat.database.model.ThreadRecord
import asia.coolapp.chat.recipients.Recipient

/**
 * Represents an all-encompassing search result that can contain various result for different
 * subcategories.
 */
data class SearchResult(
  val query: String,
  val contacts: List<Recipient>,
  val conversations: List<ThreadRecord>,
  val messages: List<MessageResult>
) {
  fun size(): Int {
    return contacts.size + conversations.size + messages.size
  }

  val isEmpty: Boolean
    get() = size() == 0

  fun merge(result: ContactSearchResult): SearchResult {
    return this.copy(contacts = result.results, query = result.query)
  }

  fun merge(result: ThreadSearchResult): SearchResult {
    return this.copy(conversations = result.results, query = result.query)
  }

  fun merge(result: MessageSearchResult): SearchResult {
    return this.copy(messages = result.results, query = result.query)
  }

  companion object {
    @JvmField
    val EMPTY = SearchResult("", emptyList(), emptyList(), emptyList())
  }
}
