package asia.coolapp.chat.payments.preferences.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import asia.coolapp.chat.R;
import asia.coolapp.chat.components.AvatarImageView;
import asia.coolapp.chat.payments.preferences.PaymentsHomeAdapter;
import asia.coolapp.chat.payments.preferences.model.PaymentItem;
import asia.coolapp.chat.util.SpanUtil;
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder;
import asia.coolapp.chat.util.viewholders.RecipientMappingModel.RecipientIdMappingModel;
import asia.coolapp.chat.util.viewholders.RecipientViewHolder;

public class PaymentItemViewHolder extends MappingViewHolder<PaymentItem> {

  private final RecipientViewHolder<RecipientIdMappingModel> delegate;
  private final PaymentsHomeAdapter.Callbacks                callbacks;
  private final AvatarImageView                              avatar;
  private final TextView                                     name;
  private final TextView                                     date;
  private final TextView                                     amount;
  private final View                                         inProgress;
  private final View                                         unreadIndicator;

  public PaymentItemViewHolder(@NonNull View itemView, @NonNull PaymentsHomeAdapter.Callbacks callbacks) {
    super(itemView);
    this.delegate        = new RecipientViewHolder<>(itemView, null, true);
    this.callbacks       = callbacks;
    this.avatar          = findViewById(R.id.recipient_view_avatar);
    this.name            = findViewById(R.id.recipient_view_name);
    this.date            = findViewById(R.id.payments_home_payment_item_date);
    this.amount          = findViewById(R.id.payments_home_payment_item_amount);
    this.inProgress      = findViewById(R.id.payments_home_payment_item_avatar_progress_overlay);
    this.unreadIndicator = findViewById(R.id.unread_indicator);
  }

  @Override
  public void bind(@NonNull PaymentItem model) {
    if (model.hasRecipient() && !model.isDefrag()) {
      delegate.bind(model.getRecipientIdModel());
    } else {
      name.setText(model.getTransactionName(getContext()));
      avatar.disableQuickContact();
      avatar.setNonAvatarImageResource(model.getTransactionAvatar());
    }

    inProgress.setVisibility(model.isInProgress() ? View.VISIBLE : View.GONE);
    date.setText(model.getDate(context));
    amount.setText(SpanUtil.color(ContextCompat.getColor(context, model.getAmountColor()), model.getAmount(getContext())));

    itemView.setOnClickListener(v -> callbacks.onPaymentItem(model));

    unreadIndicator.setVisibility(model.isUnread() ? View.VISIBLE : View.GONE);
  }
}
