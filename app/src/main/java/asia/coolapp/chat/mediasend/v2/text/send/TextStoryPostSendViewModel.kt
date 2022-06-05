package asia.coolapp.chat.mediasend.v2.text.send

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import asia.coolapp.chat.contacts.paged.ContactSearchKey
import asia.coolapp.chat.database.model.IdentityRecord
import asia.coolapp.chat.linkpreview.LinkPreview
import asia.coolapp.chat.mediasend.v2.text.TextStoryPostCreationState
import asia.coolapp.chat.util.livedata.Store

class TextStoryPostSendViewModel(private val repository: TextStoryPostSendRepository) : ViewModel() {

  private val store = Store(TextStoryPostSendState.INIT)
  private val untrustedIdentitySubject = PublishSubject.create<List<IdentityRecord>>()
  private val disposables = CompositeDisposable()

  val state: LiveData<TextStoryPostSendState> = store.stateLiveData
  val untrustedIdentities: Observable<List<IdentityRecord>> = untrustedIdentitySubject

  override fun onCleared() {
    disposables.clear()
  }

  fun onSending() {
    store.update {
      TextStoryPostSendState.SENDING
    }
  }

  fun onSendCancelled() {
    store.update {
      TextStoryPostSendState.INIT
    }
  }

  fun onSend(contactSearchKeys: Set<ContactSearchKey>, textStoryPostCreationState: TextStoryPostCreationState, linkPreview: LinkPreview?) {
    store.update {
      TextStoryPostSendState.SENDING
    }

    disposables += repository.send(contactSearchKeys, textStoryPostCreationState, linkPreview).subscribeBy(
      onSuccess = {
        when (it) {
          is TextStoryPostSendResult.Success -> {
            store.update { TextStoryPostSendState.SENT }
          }
          is TextStoryPostSendResult.UntrustedRecordsError -> {
            untrustedIdentitySubject.onNext(it.untrustedRecords)
            store.update { TextStoryPostSendState.INIT }
          }
        }
      },
      onError = {
        // TODO [stories] -- Error of some sort.
        store.update { TextStoryPostSendState.INIT }
      }
    )
  }

  class Factory(private val repository: TextStoryPostSendRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return modelClass.cast(TextStoryPostSendViewModel(repository)) as T
    }
  }
}
