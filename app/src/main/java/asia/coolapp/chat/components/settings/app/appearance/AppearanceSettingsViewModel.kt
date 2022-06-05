package asia.coolapp.chat.components.settings.app.appearance

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import asia.coolapp.chat.jobs.EmojiSearchIndexDownloadJob
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.util.livedata.Store

class AppearanceSettingsViewModel : ViewModel() {
  private val store: Store<AppearanceSettingsState>

  init {
    val initialState = AppearanceSettingsState(
      SignalStore.settings().theme,
      SignalStore.settings().messageFontSize,
      SignalStore.settings().language
    )

    store = Store(initialState)
  }

  val state: LiveData<AppearanceSettingsState> = store.stateLiveData

  fun setTheme(theme: String) {
    store.update { it.copy(theme = theme) }
    SignalStore.settings().theme = theme
  }

  fun setLanguage(language: String) {
    store.update { it.copy(language = language) }
    SignalStore.settings().language = language
    EmojiSearchIndexDownloadJob.scheduleImmediately()
  }

  fun setMessageFontSize(size: Int) {
    store.update { it.copy(messageFontSize = size) }
    SignalStore.settings().messageFontSize = size
  }
}
