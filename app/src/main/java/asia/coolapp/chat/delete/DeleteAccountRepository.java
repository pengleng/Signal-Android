package asia.coolapp.chat.delete;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.annimon.stream.Stream;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.database.GroupDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.groups.GroupManager;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.pin.KbsEnclaves;
import asia.coolapp.chat.subscription.Subscriber;
import asia.coolapp.chat.util.ServiceUtil;
import org.whispersystems.signalservice.api.util.PhoneNumberFormatter;
import org.whispersystems.signalservice.internal.EmptyResponse;
import org.whispersystems.signalservice.internal.ServiceResponse;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedResponseException;

import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;

class DeleteAccountRepository {
  private static final String TAG = Log.tag(DeleteAccountRepository.class);

  @NonNull List<Country> getAllCountries() {
    return Stream.of(PhoneNumberUtil.getInstance().getSupportedRegions())
                 .map(DeleteAccountRepository::getCountryForRegion)
                 .sorted(new RegionComparator())
                 .toList();
  }

  @NonNull String getRegionDisplayName(@NonNull String region) {
    return PhoneNumberFormatter.getRegionDisplayName(region).orElse("");
  }

  int getRegionCountryCode(@NonNull String region) {
    return PhoneNumberUtil.getInstance().getCountryCodeForRegion(region);
  }

  void deleteAccount(@NonNull Consumer<DeleteAccountEvent> onDeleteAccountEvent) {
    SignalExecutors.BOUNDED.execute(() -> {
      if (SignalStore.donationsValues().getSubscriber() != null) {
        Log.i(TAG, "deleteAccount: attempting to cancel subscription");
        onDeleteAccountEvent.accept(DeleteAccountEvent.CancelingSubscription.INSTANCE);

        Subscriber                     subscriber                 = SignalStore.donationsValues().requireSubscriber();
        ServiceResponse<EmptyResponse> cancelSubscriptionResponse = ApplicationDependencies.getDonationsService()
                                                                                           .cancelSubscription(subscriber.getSubscriberId())
                                                                                           .blockingGet();

        if (cancelSubscriptionResponse.getExecutionError().isPresent()) {
          Log.w(TAG, "deleteAccount: failed attempt to cancel subscription");
          onDeleteAccountEvent.accept(DeleteAccountEvent.CancelSubscriptionFailed.INSTANCE);
          return;
        }

        switch (cancelSubscriptionResponse.getStatus()) {
          case 404:
            Log.i(TAG, "deleteAccount: subscription does not exist. Continuing deletion...");
            break;
          case 200:
            Log.i(TAG, "deleteAccount: successfully cancelled subscription. Continuing deletion...");
            break;
          default:
            Log.w(TAG, "deleteAccount: an unexpected error occurred. " + cancelSubscriptionResponse.getStatus());
            onDeleteAccountEvent.accept(DeleteAccountEvent.CancelSubscriptionFailed.INSTANCE);
            return;
        }
      }

      Log.i(TAG, "deleteAccount: attempting to leave groups...");

      int groupsLeft = 0;
      try (GroupDatabase.Reader groups = SignalDatabase.groups().getGroups()) {
        GroupDatabase.GroupRecord groupRecord = groups.getNext();
        onDeleteAccountEvent.accept(new DeleteAccountEvent.LeaveGroupsProgress(groups.getCount(), 0));
        Log.i(TAG, "deleteAccount: found " + groups.getCount() + " groups to leave.");

        while (groupRecord != null) {
          if (groupRecord.getId().isPush() && groupRecord.isActive()) {
            GroupManager.leaveGroup(ApplicationDependencies.getApplication(), groupRecord.getId().requirePush());
            onDeleteAccountEvent.accept(new DeleteAccountEvent.LeaveGroupsProgress(groups.getCount(), ++groupsLeft));
          }

          groupRecord = groups.getNext();
        }

        onDeleteAccountEvent.accept(DeleteAccountEvent.LeaveGroupsFinished.INSTANCE);
      } catch (Exception e) {
        Log.w(TAG, "deleteAccount: failed to leave groups", e);
        onDeleteAccountEvent.accept(DeleteAccountEvent.LeaveGroupsFailed.INSTANCE);
        return;
      }

      Log.i(TAG, "deleteAccount: successfully left all groups.");
      Log.i(TAG, "deleteAccount: attempting to remove pin...");

      try {
        ApplicationDependencies.getKeyBackupService(KbsEnclaves.current()).newPinChangeSession().removePin();
      } catch (UnauthenticatedResponseException | IOException e) {
        Log.w(TAG, "deleteAccount: failed to remove PIN", e);
        onDeleteAccountEvent.accept(DeleteAccountEvent.PinDeletionFailed.INSTANCE);
        return;
      }

      Log.i(TAG, "deleteAccount: successfully removed pin.");
      Log.i(TAG, "deleteAccount: attempting to delete account from server...");

      try {
        ApplicationDependencies.getSignalServiceAccountManager().deleteAccount();
      } catch (IOException e) {
        Log.w(TAG, "deleteAccount: failed to delete account from signal service", e);
        onDeleteAccountEvent.accept(DeleteAccountEvent.ServerDeletionFailed.INSTANCE);
        return;
      }

      Log.i(TAG, "deleteAccount: successfully removed account from server");
      Log.i(TAG, "deleteAccount: attempting to delete user data and close process...");

      if (!ServiceUtil.getActivityManager(ApplicationDependencies.getApplication()).clearApplicationUserData()) {
        Log.w(TAG, "deleteAccount: failed to delete user data");
        onDeleteAccountEvent.accept(DeleteAccountEvent.LocalDataDeletionFailed.INSTANCE);
      }
    });
  }

  private static @NonNull Country getCountryForRegion(@NonNull String region) {
    return new Country(PhoneNumberFormatter.getRegionDisplayName(region).orElse(""),
                       PhoneNumberUtil.getInstance().getCountryCodeForRegion(region),
                       region);
  }

  private static class RegionComparator implements Comparator<Country> {

    private final Collator collator;

    RegionComparator() {
      collator = Collator.getInstance();
      collator.setStrength(Collator.PRIMARY);
    }

    @Override
    public int compare(Country lhs, Country rhs) {
      return collator.compare(lhs.getDisplayName(), rhs.getDisplayName());
    }
  }
}
