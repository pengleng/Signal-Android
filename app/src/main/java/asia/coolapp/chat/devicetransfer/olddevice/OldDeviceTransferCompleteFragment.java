package asia.coolapp.chat.devicetransfer.olddevice;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import asia.coolapp.chat.LoggingFragment;
import asia.coolapp.chat.R;

/**
 * Shown after the old device successfully completes sending a backup to the new device.
 */
public final class OldDeviceTransferCompleteFragment extends LoggingFragment {
  public OldDeviceTransferCompleteFragment() {
    super(R.layout.old_device_transfer_complete_fragment);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    view.findViewById(R.id.old_device_transfer_complete_fragment_close)
        .setOnClickListener(v -> close());
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        close();
      }
    });
  }

  private void close() {
    OldDeviceExitActivity.exit(requireActivity());
  }
}
