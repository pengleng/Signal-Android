package asia.coolapp.chat.badges.self.none

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.signal.core.util.logging.Log
import asia.coolapp.chat.components.settings.app.subscription.SubscriptionsRepository
import asia.coolapp.chat.util.livedata.Store

class BecomeASustainerViewModel(subscriptionsRepository: SubscriptionsRepository) : ViewModel() {

  private val store = Store(BecomeASustainerState())

  val state: LiveData<BecomeASustainerState> = store.stateLiveData

  private val disposables = CompositeDisposable()

  init {
    disposables += subscriptionsRepository.getSubscriptions().subscribeBy(
      onError = { Log.w(TAG, "Could not load subscriptions.") },
      onSuccess = { subscriptions ->
        store.update {
          it.copy(badge = subscriptions.firstOrNull()?.badge)
        }
      }
    )
  }

  override fun onCleared() {
    disposables.clear()
  }

  companion object {
    private val TAG = Log.tag(BecomeASustainerViewModel::class.java)
  }

  class Factory(private val subscriptionsRepository: SubscriptionsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return modelClass.cast(BecomeASustainerViewModel(subscriptionsRepository))!!
    }
  }
}
