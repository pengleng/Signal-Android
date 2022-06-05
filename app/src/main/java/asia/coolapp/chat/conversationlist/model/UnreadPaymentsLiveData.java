package asia.coolapp.chat.conversationlist.model;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import org.signal.core.util.concurrent.SignalExecutors;
import asia.coolapp.chat.database.DatabaseObserver;
import asia.coolapp.chat.database.PaymentDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.util.concurrent.SerialMonoLifoExecutor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * LiveData encapsulating the logic to watch the Payments database for changes to payments and supply
 * a list of unread payments to listeners. If there are no unread payments, Optional.empty() will be passed
 * through instead.
 */
public final class UnreadPaymentsLiveData extends LiveData<Optional<UnreadPayments>> {

  private final PaymentDatabase           paymentDatabase;
  private final DatabaseObserver.Observer observer;
  private final Executor                  executor;

  public UnreadPaymentsLiveData() {
    this.paymentDatabase = SignalDatabase.payments();
    this.observer        = this::refreshUnreadPayments;
    this.executor        = new SerialMonoLifoExecutor(SignalExecutors.BOUNDED);
  }

  @Override
  protected void onActive() {
    refreshUnreadPayments();
    ApplicationDependencies.getDatabaseObserver().registerAllPaymentsObserver(observer);
  }

  @Override
  protected void onInactive() {
    ApplicationDependencies.getDatabaseObserver().unregisterObserver(observer);
  }

  private void refreshUnreadPayments() {
    executor.execute(() -> postValue(Optional.ofNullable(getUnreadPayments())));
  }

  @WorkerThread
  private @Nullable UnreadPayments getUnreadPayments() {
    List<PaymentDatabase.PaymentTransaction> unseenPayments  = paymentDatabase.getUnseenPayments();
    int                                      unseenCount     = unseenPayments.size();

    switch (unseenCount) {
      case 0:
        return null;
      case 1:
        PaymentDatabase.PaymentTransaction transaction = unseenPayments.get(0);
        Recipient                          recipient   = transaction.getPayee().hasRecipientId()
                                                         ? Recipient.resolved(transaction.getPayee().requireRecipientId())
                                                         : null;

        return UnreadPayments.forSingle(recipient, transaction.getUuid(), transaction.getAmount());
      default:
        return UnreadPayments.forMultiple(unseenCount);
    }
  }
}
