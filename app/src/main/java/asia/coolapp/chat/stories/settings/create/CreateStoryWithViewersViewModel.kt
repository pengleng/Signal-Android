package asia.coolapp.chat.stories.settings.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.util.livedata.Store

class CreateStoryWithViewersViewModel(
  private val repository: CreateStoryWithViewersRepository
) : ViewModel() {

  private val store = Store(CreateStoryWithViewersState())
  private val disposables = CompositeDisposable()

  val state: LiveData<CreateStoryWithViewersState> = store.stateLiveData

  override fun onCleared() {
    disposables.clear()
  }

  fun setLabel(label: CharSequence) {
    store.update { it.copy(label = label) }
  }

  fun create(members: Set<RecipientId>) {
    store.update { it.copy(saveState = CreateStoryWithViewersState.SaveState.Saving) }

    val label = store.state.label
    if (label.isEmpty()) {
      store.update {
        it.copy(
          error = CreateStoryWithViewersState.NameError.NO_LABEL,
          saveState = CreateStoryWithViewersState.SaveState.Init
        )
      }
    }

    disposables += repository.createList(label, members).subscribeBy(
      onSuccess = { recipientId ->
        store.update {
          it.copy(saveState = CreateStoryWithViewersState.SaveState.Saved(recipientId))
        }
      },
      onError = {
        store.update {
          it.copy(
            saveState = CreateStoryWithViewersState.SaveState.Init,
            error = CreateStoryWithViewersState.NameError.DUPLICATE_LABEL
          )
        }
      }
    )
  }

  class Factory(
    private val repository: CreateStoryWithViewersRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return modelClass.cast(CreateStoryWithViewersViewModel(repository)) as T
    }
  }
}
