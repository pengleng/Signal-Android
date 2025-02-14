package asia.coolapp.chat.components.settings.app.subscription.receipts.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign
import asia.coolapp.chat.database.model.DonationReceiptRecord
import asia.coolapp.chat.util.InternetConnectionObserver
import asia.coolapp.chat.util.livedata.Store

class DonationReceiptDetailViewModel(id: Long, private val repository: DonationReceiptDetailRepository) : ViewModel() {

  private val store = Store(DonationReceiptDetailState())
  private val disposables = CompositeDisposable()
  private var networkDisposable: Disposable
  private val cachedRecord: Single<DonationReceiptRecord> = repository.getDonationReceiptRecord(id).cache()

  val state: LiveData<DonationReceiptDetailState> = store.stateLiveData

  init {
    networkDisposable = InternetConnectionObserver
      .observe()
      .distinctUntilChanged()
      .subscribe { isConnected ->
        if (isConnected) {
          retry()
        }
      }

    refresh()
  }

  private fun retry() {
    if (store.state.subscriptionName == null) {
      refresh()
    }
  }

  private fun refresh() {
    disposables.clear()

    disposables += cachedRecord.subscribe { record ->
      store.update { it.copy(donationReceiptRecord = record) }
    }

    disposables += cachedRecord.flatMap {
      if (it.subscriptionLevel > 0) {
        repository.getSubscriptionLevelName(it.subscriptionLevel)
      } else {
        Single.just("")
      }
    }.subscribe { name ->
      store.update { it.copy(subscriptionName = name) }
    }
  }

  override fun onCleared() {
    disposables.clear()
    networkDisposable.dispose()
  }

  class Factory(private val id: Long, private val repository: DonationReceiptDetailRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return modelClass.cast(DonationReceiptDetailViewModel(id, repository)) as T
    }
  }
}
