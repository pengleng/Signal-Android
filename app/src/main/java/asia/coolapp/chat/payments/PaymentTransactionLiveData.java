package asia.coolapp.chat.payments;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import org.signal.core.util.concurrent.SignalExecutors;
import asia.coolapp.chat.database.DatabaseObserver;
import asia.coolapp.chat.database.PaymentDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.util.concurrent.SerialMonoLifoExecutor;

import java.util.UUID;
import java.util.concurrent.Executor;

public final class PaymentTransactionLiveData extends LiveData<PaymentDatabase.PaymentTransaction> {

  private final UUID                      paymentId;
  private final PaymentDatabase           paymentDatabase;
  private final DatabaseObserver.Observer observer;
  private final Executor                  executor;

  public PaymentTransactionLiveData(@NonNull UUID paymentId) {
    this.paymentId       = paymentId;
    this.paymentDatabase = SignalDatabase.payments();
    this.observer        = this::getPaymentTransaction;
    this.executor        = new SerialMonoLifoExecutor(SignalExecutors.BOUNDED);
  }

  @Override
  protected void onActive() {
    getPaymentTransaction();
    ApplicationDependencies.getDatabaseObserver().registerPaymentObserver(paymentId, observer);
  }

  @Override
  protected void onInactive() {
    ApplicationDependencies.getDatabaseObserver().unregisterObserver(observer);
  }

  private void getPaymentTransaction() {
    executor.execute(() -> postValue(paymentDatabase.getPayment(paymentId)));
  }
}
