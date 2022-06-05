package asia.coolapp.chat.payments.preferences.addmoney;

import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.payments.MobileCoinPublicAddress;
import asia.coolapp.chat.util.AsynchronousCallback;

final class PaymentsAddMoneyRepository {

  @MainThread
  void getWalletAddress(@NonNull AsynchronousCallback.MainThread<AddressAndUri, Error> callback) {
    if (!SignalStore.paymentsValues().mobileCoinPaymentsEnabled()) {
      callback.onError(Error.PAYMENTS_NOT_ENABLED);
    }

    MobileCoinPublicAddress publicAddress        = ApplicationDependencies.getPayments().getWallet().getMobileCoinPublicAddress();
    String                  paymentAddressBase58 = publicAddress.getPaymentAddressBase58();
    Uri                     paymentAddressUri    = publicAddress.getPaymentAddressUri();

    callback.onComplete(new AddressAndUri(paymentAddressBase58, paymentAddressUri));
  }

  enum Error {
    PAYMENTS_NOT_ENABLED
  }

}
