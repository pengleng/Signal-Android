package asia.coolapp.chat.stories.settings.custom

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import asia.coolapp.chat.database.model.DistributionListId
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.util.livedata.Store

class PrivateStorySettingsViewModel(private val distributionListId: DistributionListId, private val repository: PrivateStorySettingsRepository) : ViewModel() {

  private val store = Store(PrivateStorySettingsState())
  private val disposables = CompositeDisposable()

  val state: LiveData<PrivateStorySettingsState> = store.stateLiveData

  override fun onCleared() {
    disposables.clear()
  }

  fun refresh() {
    disposables.clear()
    disposables += repository.getRecord(distributionListId)
      .subscribe { record ->
        store.update { it.copy(privateStory = record) }
      }
    disposables += repository.getRepliesAndReactionsEnabled(distributionListId)
      .subscribe { repliesAndReactionsEnabled ->
        store.update { it.copy(areRepliesAndReactionsEnabled = repliesAndReactionsEnabled) }
      }
  }

  fun getName(): String {
    return store.state.privateStory?.name ?: ""
  }

  fun remove(recipient: Recipient) {
    disposables += repository.removeMember(distributionListId, recipient.id)
      .subscribe {
        refresh()
      }
  }

  fun setRepliesAndReactionsEnabled(repliesAndReactionsEnabled: Boolean) {
    disposables += repository.setRepliesAndReactionsEnabled(distributionListId, repliesAndReactionsEnabled)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { refresh() }
  }

  fun delete(): Completable {
    return repository.delete(distributionListId).observeOn(AndroidSchedulers.mainThread())
  }

  class Factory(private val privateStoryItemData: DistributionListId, private val repository: PrivateStorySettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return modelClass.cast(PrivateStorySettingsViewModel(privateStoryItemData, repository)) as T
    }
  }
}
