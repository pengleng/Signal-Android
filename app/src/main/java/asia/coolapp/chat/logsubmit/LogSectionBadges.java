package asia.coolapp.chat.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.recipients.Recipient;

final class LogSectionBadges implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "BADGES";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    if (!SignalStore.account().isRegistered()) {
      return "Unregistered";
    }

    if (SignalStore.account().getE164() == null || SignalStore.account().getAci() == null) {
      return "Self not yet available!";
    }

    return new StringBuilder().append("Badge Count                  : ").append(Recipient.self().getBadges().size()).append("\n")
                              .append("ExpiredBadge                 : ").append(SignalStore.donationsValues().getExpiredBadge() != null).append("\n")
                              .append("LastKeepAliveLaunchTime      : ").append(SignalStore.donationsValues().getLastKeepAliveLaunchTime()).append("\n")
                              .append("LastEndOfPeriod              : ").append(SignalStore.donationsValues().getLastEndOfPeriod()).append("\n")
                              .append("IsUserManuallyCancelled      : ").append(SignalStore.donationsValues().isUserManuallyCancelled()).append("\n")
                              .append("DisplayBadgesOnProfile       : ").append(SignalStore.donationsValues().getDisplayBadgesOnProfile()).append("\n")
                              .append("SubscriptionRedemptionFailed : ").append(SignalStore.donationsValues().getSubscriptionRedemptionFailed()).append("\n")
                              .append("ShouldCancelBeforeNextAttempt: ").append(SignalStore.donationsValues().getShouldCancelSubscriptionBeforeNextSubscribeAttempt()).append("\n");
  }
}
