package asia.coolapp.chat.stories.viewer.reply.direct

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.util.livedata.Store

class StoryDirectReplyViewModel(
  private val storyId: Long,
  private val groupDirectReplyRecipientId: RecipientId?,
  private val repository: StoryDirectReplyRepository
) : ViewModel() {

  private val store = Store(StoryDirectReplyState())
  private val disposables = CompositeDisposable()

  val state: LiveData<StoryDirectReplyState> = store.stateLiveData

  init {
    if (groupDirectReplyRecipientId != null) {
      store.update(Recipient.live(groupDirectReplyRecipientId).liveDataResolved) { recipient, state ->
        state.copy(recipient = recipient)
      }
    }

    disposables += repository.getStoryPost(storyId).subscribe { record ->
      store.update { it.copy(storyRecord = record) }
    }
  }

  fun sendReply(charSequence: CharSequence): Completable {
    return repository.send(storyId, groupDirectReplyRecipientId, charSequence, false)
  }

  fun sendReaction(emoji: CharSequence): Completable {
    return repository.send(storyId, groupDirectReplyRecipientId, emoji, true)
  }

  override fun onCleared() {
    super.onCleared()
    disposables.clear()
  }

  class Factory(
    private val storyId: Long,
    private val groupDirectReplyRecipientId: RecipientId?,
    private val repository: StoryDirectReplyRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return modelClass.cast(
        StoryDirectReplyViewModel(storyId, groupDirectReplyRecipientId, repository)
      ) as T
    }
  }
}
