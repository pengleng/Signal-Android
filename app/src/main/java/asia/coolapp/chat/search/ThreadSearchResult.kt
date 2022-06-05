package asia.coolapp.chat.search

import asia.coolapp.chat.database.model.ThreadRecord

data class ThreadSearchResult(val results: List<ThreadRecord>, val query: String)
