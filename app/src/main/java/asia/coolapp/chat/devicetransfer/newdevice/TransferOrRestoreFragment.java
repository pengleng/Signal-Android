package asia.coolapp.chat.devicetransfer.newdevice;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import asia.coolapp.chat.LoggingFragment;
import asia.coolapp.chat.R;
import asia.coolapp.chat.util.SpanUtil;
import asia.coolapp.chat.util.navigation.SafeNavigation;

/**
 * Simple jumping off menu to starts a device-to-device transfer or restore a backup.
 */
public final class TransferOrRestoreFragment extends LoggingFragment {

  public TransferOrRestoreFragment() {
    super(R.layout.fragment_transfer_restore);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    view.findViewById(R.id.transfer_or_restore_fragment_transfer)
        .setOnClickListener(v -> SafeNavigation.safeNavigate(Navigation.findNavController(v), R.id.action_new_device_transfer_instructions));

    View restoreBackup = view.findViewById(R.id.transfer_or_restore_fragment_restore);
    if (Build.VERSION.SDK_INT >= 21) {
      restoreBackup.setOnClickListener(v -> SafeNavigation.safeNavigate(Navigation.findNavController(v), R.id.action_choose_backup));
    } else {
      restoreBackup.setVisibility(View.GONE);
    }

    String description = getString(R.string.TransferOrRestoreFragment__transfer_your_account_and_messages_from_your_old_android_device);
    String toBold      = getString(R.string.TransferOrRestoreFragment__you_need_access_to_your_old_device);

    TextView transferDescriptionView = view.findViewById(R.id.transfer_or_restore_fragment_transfer_description);
    transferDescriptionView.setText(SpanUtil.boldSubstring(description, toBold));
  }
}
