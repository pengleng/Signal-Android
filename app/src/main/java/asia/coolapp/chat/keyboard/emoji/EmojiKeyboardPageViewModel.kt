package asia.coolapp.chat.keyboard.emoji

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import asia.coolapp.chat.R
import asia.coolapp.chat.components.emoji.EmojiPageModel
import asia.coolapp.chat.components.emoji.EmojiPageViewGridAdapter.EmojiHeader
import asia.coolapp.chat.components.emoji.RecentEmojiPageModel
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.emoji.EmojiCategory
import asia.coolapp.chat.keyboard.emoji.EmojiKeyboardPageCategoryMappingModel.EmojiCategoryMappingModel
import asia.coolapp.chat.util.DefaultValueLiveData
import asia.coolapp.chat.util.TextSecurePreferences
import asia.coolapp.chat.util.adapter.mapping.MappingModelList
import asia.coolapp.chat.util.livedata.LiveDataUtil

class EmojiKeyboardPageViewModel(private val repository: EmojiKeyboardPageRepository) : ViewModel() {

  private val internalSelectedKey = DefaultValueLiveData<String>(getStartingTab())

  val selectedKey: LiveData<String>
    get() = internalSelectedKey

  val allEmojiModels: MutableLiveData<List<EmojiPageModel>> = MutableLiveData()
  val pages: LiveData<MappingModelList>
  val categories: LiveData<MappingModelList>

  init {
    pages = LiveDataUtil.mapAsync(allEmojiModels) { models ->
      val list = MappingModelList()
      models.forEach { pageModel ->
        if (RecentEmojiPageModel.KEY != pageModel.key) {
          val category = EmojiCategory.forKey(pageModel.key)
          list += EmojiHeader(pageModel.key, category.getCategoryLabel())
          list += pageModel.toMappingModels()
        } else if (pageModel.displayEmoji.isNotEmpty()) {
          list += EmojiHeader(pageModel.key, R.string.ReactWithAnyEmojiBottomSheetDialogFragment__recently_used)
          list += pageModel.toMappingModels()
        }
      }

      list
    }

    categories = LiveDataUtil.combineLatest(allEmojiModels, internalSelectedKey) { models, selectedKey ->
      val list = MappingModelList()
      list += models.map { m ->
        if (RecentEmojiPageModel.KEY == m.key) {
          EmojiKeyboardPageCategoryMappingModel.RecentsMappingModel(m.key == selectedKey)
        } else {
          val category = EmojiCategory.forKey(m.key)
          EmojiCategoryMappingModel(category, category.key == selectedKey)
        }
      }
      list
    }
  }

  fun onKeySelected(key: String) {
    internalSelectedKey.value = key
  }

  fun refreshRecentEmoji() {
    repository.getEmoji(allEmojiModels::postValue)
  }

  companion object {
    fun getStartingTab(): String {
      return if (RecentEmojiPageModel.hasRecents(ApplicationDependencies.getApplication(), TextSecurePreferences.RECENT_STORAGE_KEY)) {
        RecentEmojiPageModel.KEY
      } else {
        EmojiCategory.PEOPLE.key
      }
    }
  }

  class Factory(context: Context) : ViewModelProvider.Factory {

    private val repository = EmojiKeyboardPageRepository(context)

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(EmojiKeyboardPageViewModel(repository)))
    }
  }
}
