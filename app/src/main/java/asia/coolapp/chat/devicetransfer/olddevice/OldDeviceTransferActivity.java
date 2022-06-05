package asia.coolapp.chat.devicetransfer.olddevice;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import asia.coolapp.chat.PassphraseRequiredActivity;
import asia.coolapp.chat.R;
import asia.coolapp.chat.util.DynamicNoActionBarTheme;
import asia.coolapp.chat.util.DynamicTheme;

/**
 * Shell of an activity to hold the old device navigation graph. See the various
 * fragments in this package for actual implementation.
 */
public final class OldDeviceTransferActivity extends PassphraseRequiredActivity {

  private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState, boolean ready) {
    dynamicTheme.onCreate(this);

    setContentView(R.layout.old_device_transfer_activity);

    NavController controller = Navigation.findNavController(this, R.id.nav_host_fragment);
    controller.setGraph(R.navigation.old_device_transfer);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }
}
