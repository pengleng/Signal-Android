package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.database.PaymentDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.BackoffUtil;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.payments.FailureReason;
import asia.coolapp.chat.payments.PaymentTransactionId;
import asia.coolapp.chat.payments.Payments;
import asia.coolapp.chat.payments.Wallet;
import asia.coolapp.chat.util.FeatureFlags;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class PaymentTransactionCheckJob extends BaseJob {

  private static final String TAG = Log.tag(PaymentTransactionCheckJob.class);

  public static final String KEY = "PaymentTransactionCheckJob";

  private static final String KEY_UUID = "uuid";

  private final UUID uuid;

  public PaymentTransactionCheckJob(@NonNull UUID uuid) {
    this(uuid, PaymentSendJob.QUEUE);
  }

  public PaymentTransactionCheckJob(@NonNull UUID uuid, @NonNull String queue) {
    this(new Parameters.Builder()
                       .setQueue(queue)
                       .addConstraint(NetworkConstraint.KEY)
                       .setMaxAttempts(Parameters.UNLIMITED)
                       .build(),
         uuid);
  }

  private PaymentTransactionCheckJob(@NonNull Parameters parameters, @NonNull UUID uuid) {
    super(parameters);

    this.uuid = uuid;
  }

  @Override
  protected void onRun() throws Exception {
    PaymentDatabase paymentDatabase = SignalDatabase.payments();

    PaymentDatabase.PaymentTransaction payment = paymentDatabase.getPayment(uuid);

    if (payment == null) {
      Log.w(TAG, "No payment found for UUID " + uuid);
      return;
    }

    Payments payments = ApplicationDependencies.getPayments();

    switch (payment.getDirection()) {
      case SENT: {
        Log.i(TAG, "Checking sent status of " + uuid);
        PaymentTransactionId           paymentTransactionId = new PaymentTransactionId.MobileCoin(Objects.requireNonNull(payment.getTransaction()), Objects.requireNonNull(payment.getReceipt()), payment.getFee().requireMobileCoin());
        Wallet.TransactionStatusResult status               = payments.getWallet().getSentTransactionStatus(paymentTransactionId);

        switch (status.getTransactionStatus()) {
          case COMPLETE:
            paymentDatabase.markPaymentSuccessful(uuid, status.getBlockIndex());
            Log.i(TAG, "Marked sent payment successful " + uuid);
            break;
          case FAILED:
            paymentDatabase.markPaymentFailed(uuid, FailureReason.UNKNOWN);
            Log.i(TAG, "Marked sent payment failed " + uuid);
            break;
          case IN_PROGRESS:
            Log.i(TAG, "Sent payment still in progress " + uuid);
            throw new IncompleteTransactionException();
          default:
            throw new AssertionError();
        }
        break;
      }
      case RECEIVED: {
        Log.i(TAG, "Checking received status of " + uuid);
        Wallet.ReceivedTransactionStatus transactionStatus = payments.getWallet().getReceivedTransactionStatus(Objects.requireNonNull(payment.getReceipt()));

        switch (transactionStatus.getStatus()) {
          case COMPLETE:
            paymentDatabase.markReceivedPaymentSuccessful(uuid, transactionStatus.getAmount(), transactionStatus.getBlockIndex());
            Log.i(TAG, "Marked received payment successful " + uuid);
            break;
          case FAILED:
            paymentDatabase.markPaymentFailed(uuid, FailureReason.UNKNOWN);
            Log.i(TAG, "Marked received payment failed " + uuid);
            break;
          case IN_PROGRESS:
            Log.i(TAG, "Received payment still in progress " + uuid);
            throw new IncompleteTransactionException();
          default:
            throw new AssertionError();
        }
        break;
      }
      default: {
        throw new AssertionError();
      }
    }
  }

  @Override
  public long getNextRunAttemptBackoff(int pastAttemptCount, @NonNull Exception exception) {
    if (exception instanceof NonSuccessfulResponseCodeException) {
      if (((NonSuccessfulResponseCodeException) exception).is5xx()) {
        return BackoffUtil.exponentialBackoff(pastAttemptCount, FeatureFlags.getServerErrorMaxBackoff());
      }
    }

    if (exception instanceof IncompleteTransactionException && pastAttemptCount < 20) {
      return 500;
    }

    return super.getNextRunAttemptBackoff(pastAttemptCount, exception);
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof IncompleteTransactionException ||
           e instanceof IOException;
  }

  @NonNull
  @Override
  public Data serialize() {
    return new Data.Builder()
                   .putString(KEY_UUID, uuid.toString())
                   .build();
  }

  @NonNull
  @Override
  public String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onFailure() {
  }

  private static final class IncompleteTransactionException extends Exception {
  }

  public static class Factory implements Job.Factory<PaymentTransactionCheckJob> {

    @Override
    public @NonNull PaymentTransactionCheckJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new PaymentTransactionCheckJob(parameters,
                                            UUID.fromString(data.getString(KEY_UUID)));
    }
  }
}
