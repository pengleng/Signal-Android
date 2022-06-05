package asia.coolapp.chat.util;

import android.content.Context;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import org.signal.libsignal.protocol.IdentityKey;
import asia.coolapp.chat.database.documents.IdentityKeyMismatch;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.verify.VerifyIdentityActivity;

public class VerifySpan extends ClickableSpan {

  private final Context     context;
  private final RecipientId recipientId;
  private final IdentityKey identityKey;

  public VerifySpan(@NonNull Context context, @NonNull IdentityKeyMismatch mismatch) {
    this.context     = context;
    this.recipientId = mismatch.getRecipientId(context);
    this.identityKey = mismatch.getIdentityKey();
  }

  public VerifySpan(@NonNull Context context, @NonNull RecipientId recipientId, @NonNull IdentityKey identityKey) {
    this.context     = context;
    this.recipientId = recipientId;
    this.identityKey = identityKey;
  }

  @Override
  public void onClick(@NonNull View widget) {
    context.startActivity(VerifyIdentityActivity.newIntent(context, recipientId, identityKey, false));
  }
}
