package asia.coolapp.chat.payments.preferences;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import asia.coolapp.chat.R;

/**
 * Dialog to display if chosen Recipient has not enabled payments.
 */
public final class RecipientHasNotEnabledPaymentsDialog {

  private RecipientHasNotEnabledPaymentsDialog() {
  }

  public static void show(@NonNull Context context) {
    show(context, null);
  }
  public static void show(@NonNull Context context, @Nullable Runnable onDismissed) {
    new AlertDialog.Builder(context).setTitle(R.string.ConfirmPaymentFragment__invalid_recipient)
                                    .setMessage(R.string.ConfirmPaymentFragment__this_person_has_not_activated_payments)
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                      dialog.dismiss();
                                      if (onDismissed != null) {
                                        onDismissed.run();
                                      }
                                    })
                                    .setCancelable(false)
                                    .show();
  }
}
