package asia.coolapp.chat.keyboard

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.signal.core.util.ThreadUtil
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.stickers.StickerSearchRepository
import asia.coolapp.chat.util.DefaultValueLiveData

class KeyboardPagerViewModel : ViewModel() {

  private val page: DefaultValueLiveData<KeyboardPage>
  private val pages: DefaultValueLiveData<Set<KeyboardPage>>

  init {
    val startingPages: MutableSet<KeyboardPage> = KeyboardPage.values().toMutableSet()
    if (SignalStore.settings().isPreferSystemEmoji) {
      startingPages.remove(KeyboardPage.EMOJI)
    }
    pages = DefaultValueLiveData(startingPages)
    page = DefaultValueLiveData(startingPages.first())

    StickerSearchRepository(ApplicationDependencies.getApplication()).getStickerFeatureAvailability { available ->
      if (!available) {
        val updatedPages = pages.value.toMutableSet().apply { remove(KeyboardPage.STICKER) }
        pages.postValue(updatedPages)
        if (page.value == KeyboardPage.STICKER) {
          switchToPage(KeyboardPage.GIF)
          switchToPage(KeyboardPage.EMOJI)
        }
      }
    }
  }

  fun page(): LiveData<KeyboardPage> = page
  fun pages(): LiveData<Set<KeyboardPage>> = pages

  @MainThread
  fun setOnlyPage(page: KeyboardPage) {
    pages.value = setOf(page)
    switchToPage(page)
  }

  fun switchToPage(page: KeyboardPage) {
    if (this.pages.value.contains(page) && this.page.value != page) {
      if (ThreadUtil.isMainThread()) {
        this.page.value = page
      } else {
        this.page.postValue(page)
      }
    }
  }
}
