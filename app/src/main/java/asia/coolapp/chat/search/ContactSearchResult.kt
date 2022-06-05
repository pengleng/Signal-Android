package asia.coolapp.chat.search

import asia.coolapp.chat.recipients.Recipient

data class ContactSearchResult(val results: List<Recipient>, val query: String)
