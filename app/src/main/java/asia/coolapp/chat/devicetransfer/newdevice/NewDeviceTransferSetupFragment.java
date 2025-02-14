package asia.coolapp.chat.devicetransfer.newdevice;

import android.app.PendingIntent;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.navigation.fragment.NavHostFragment;

import org.signal.devicetransfer.DeviceToDeviceTransferService;
import org.signal.devicetransfer.DeviceToDeviceTransferService.TransferNotificationData;
import asia.coolapp.chat.MainActivity;
import asia.coolapp.chat.R;
import asia.coolapp.chat.devicetransfer.DeviceTransferSetupFragment;
import asia.coolapp.chat.devicetransfer.SetupStep;
import asia.coolapp.chat.notifications.NotificationChannels;
import asia.coolapp.chat.notifications.NotificationIds;
import asia.coolapp.chat.util.navigation.SafeNavigation;

/**
 * Most responsibility is in {@link DeviceTransferSetupFragment} and delegates here
 * for strings and behavior relevant to setting up device transfer for the new device.
 *
 * Also responsible for setting up {@link DeviceToDeviceTransferService}.
 */
public final class NewDeviceTransferSetupFragment extends DeviceTransferSetupFragment {

  @Override
  protected void navigateAwayFromTransfer() {
    SafeNavigation.safeNavigate(NavHostFragment.findNavController(this), R.id.action_deviceTransferSetup_to_transferOrRestore);
  }

  @Override
  protected void navigateToTransferConnected() {
    SafeNavigation.safeNavigate(NavHostFragment.findNavController(this), R.id.action_new_device_transfer);
  }

  @Override
  protected @StringRes int getErrorTextForStep(@NonNull SetupStep step) {
    switch (step) {
      case PERMISSIONS_DENIED:
        return R.string.NewDeviceTransferSetup__signal_needs_the_location_permission_to_discover_and_connect_with_your_old_device;
      case LOCATION_DISABLED:
        return R.string.NewDeviceTransferSetup__signal_needs_location_services_enabled_to_discover_and_connect_with_your_old_device;
      case WIFI_DISABLED:
        return R.string.NewDeviceTransferSetup__signal_needs_wifi_on_to_discover_and_connect_with_your_old_device;
      case WIFI_DIRECT_UNAVAILABLE:
        return R.string.NewDeviceTransferSetup__sorry_it_appears_your_device_does_not_support_wifi_direct;
      case ERROR:
        return R.string.NewDeviceTransferSetup__an_unexpected_error_occurred_while_attempting_to_connect_to_your_old_device;
    }
    throw new AssertionError("No error text for step: " + step);
  }

  @Override
  protected @StringRes int getErrorResolveButtonTextForStep(@NonNull SetupStep step) {
    if (step == SetupStep.WIFI_DIRECT_UNAVAILABLE) {
      return R.string.NewDeviceTransferSetup__restore_a_backup;
    }
    throw new AssertionError("No error resolve button text for step: " + step);
  }

  @Override
  protected @StringRes int getStatusTextForStep(@NonNull SetupStep step, boolean takingTooLongInStep) {
    switch (step) {
      case SETTING_UP:
        return takingTooLongInStep ? R.string.NewDeviceTransferSetup__take_a_moment_should_be_ready_soon
                                   : R.string.NewDeviceTransferSetup__preparing_to_connect_to_old_android_device;
      case WAITING:
        return R.string.NewDeviceTransferSetup__waiting_for_old_device_to_connect;
      case ERROR:
        return R.string.NewDeviceTransferSetup__an_unexpected_error_occurred_while_attempting_to_connect_to_your_old_device;
      case TROUBLESHOOTING:
        return R.string.DeviceTransferSetup__unable_to_discover_old_device;
    }
    throw new AssertionError("No status text for step: " + step);
  }

  @Override
  protected void navigateWhenWifiDirectUnavailable() {
    SafeNavigation.safeNavigate(NavHostFragment.findNavController(this), R.id.action_deviceTransferSetup_to_transferOrRestore);
  }

  @Override
  protected void startTransfer() {
    PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, MainActivity.clearTop(requireContext()), 0);

    TransferNotificationData notificationData = new TransferNotificationData(NotificationIds.DEVICE_TRANSFER, NotificationChannels.BACKUPS, R.drawable.ic_signal_backup);
    DeviceToDeviceTransferService.startServer(requireContext(), new NewDeviceServerTask(), notificationData, pendingIntent);
  }
}
