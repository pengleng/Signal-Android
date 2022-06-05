package asia.coolapp.chat.payments.backup;

import androidx.annotation.NonNull;

import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.payments.Mnemonic;

public final class PaymentsRecoveryRepository {
  public @NonNull Mnemonic getMnemonic() {
    return SignalStore.paymentsValues().getPaymentsMnemonic();
  }
}
