package asia.coolapp.chat.conversation.colors.ui

import androidx.lifecycle.LiveData
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.conversation.colors.ChatColors
import asia.coolapp.chat.conversation.colors.ChatColorsPalette
import asia.coolapp.chat.database.ChatColorsDatabase
import asia.coolapp.chat.database.DatabaseObserver
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.util.concurrent.SerialMonoLifoExecutor
import java.util.concurrent.Executor

class ChatColorsOptionsLiveData : LiveData<List<ChatColors>>() {
  private val chatColorsDatabase: ChatColorsDatabase = SignalDatabase.chatColors
  private val observer: DatabaseObserver.Observer = DatabaseObserver.Observer { refreshChatColors() }
  private val executor: Executor = SerialMonoLifoExecutor(SignalExecutors.BOUNDED)

  override fun onActive() {
    refreshChatColors()
    ApplicationDependencies.getDatabaseObserver().registerChatColorsObserver(observer)
  }

  override fun onInactive() {
    ApplicationDependencies.getDatabaseObserver().unregisterObserver(observer)
  }

  private fun refreshChatColors() {
    executor.execute {
      val options = mutableListOf<ChatColors>().apply {
        addAll(ChatColorsPalette.Bubbles.all)
        addAll(chatColorsDatabase.getSavedChatColors())
      }

      postValue(options)
    }
  }
}
