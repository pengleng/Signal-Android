package asia.coolapp.chat.payments.preferences;

import androidx.annotation.NonNull;

import asia.coolapp.chat.R;
import asia.coolapp.chat.components.settings.SettingHeader;
import asia.coolapp.chat.payments.preferences.model.InProgress;
import asia.coolapp.chat.payments.preferences.model.InfoCard;
import asia.coolapp.chat.payments.preferences.model.IntroducingPayments;
import asia.coolapp.chat.payments.preferences.model.NoRecentActivity;
import asia.coolapp.chat.payments.preferences.model.PaymentItem;
import asia.coolapp.chat.payments.preferences.model.SeeAll;
import asia.coolapp.chat.payments.preferences.viewholder.InProgressViewHolder;
import asia.coolapp.chat.payments.preferences.viewholder.InfoCardViewHolder;
import asia.coolapp.chat.payments.preferences.viewholder.IntroducingPaymentViewHolder;
import asia.coolapp.chat.payments.preferences.viewholder.NoRecentActivityViewHolder;
import asia.coolapp.chat.payments.preferences.viewholder.PaymentItemViewHolder;
import asia.coolapp.chat.payments.preferences.viewholder.SeeAllViewHolder;
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter;

public class PaymentsHomeAdapter extends MappingAdapter {

  public PaymentsHomeAdapter(@NonNull Callbacks callbacks) {
    registerFactory(IntroducingPayments.class, p -> new IntroducingPaymentViewHolder(p, callbacks), R.layout.payments_home_introducing_payments_item);
    registerFactory(NoRecentActivity.class, NoRecentActivityViewHolder::new, R.layout.payments_home_no_recent_activity_item);
    registerFactory(InProgress.class, InProgressViewHolder::new, R.layout.payments_home_in_progress);
    registerFactory(PaymentItem.class, p -> new PaymentItemViewHolder(p, callbacks), R.layout.payments_home_payment_item);
    registerFactory(SettingHeader.Item.class, SettingHeader.ViewHolder::new, R.layout.base_settings_header_item);
    registerFactory(SeeAll.class, p -> new SeeAllViewHolder(p, callbacks), R.layout.payments_home_see_all_item);
    registerFactory(InfoCard.class, p -> new InfoCardViewHolder(p, callbacks), R.layout.payment_info_card);
  }

  public interface Callbacks {
    default void onActivatePayments() {}
    default void onRestorePaymentsAccount() {}
    default void onSeeAll(@NonNull PaymentType paymentType) {}
    default void onPaymentItem(@NonNull PaymentItem model) {}
    default void onInfoCardDismissed() {}
    default void onViewRecoveryPhrase() {}
    default void onUpdatePin() {}
  }
}
