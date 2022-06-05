package asia.coolapp.chat.payments.preferences;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import asia.coolapp.chat.PassphraseRequiredActivity;
import asia.coolapp.chat.R;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobs.PaymentLedgerUpdateJob;
import asia.coolapp.chat.util.DynamicNoActionBarTheme;
import asia.coolapp.chat.util.DynamicTheme;
import asia.coolapp.chat.util.navigation.SafeNavigation;

public class PaymentsActivity extends PassphraseRequiredActivity {

  public static final String EXTRA_PAYMENTS_STARTING_ACTION = "payments_starting_action";
  public static final String EXTRA_STARTING_ARGUMENTS       = "payments_starting_arguments";

  private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState, boolean ready) {
    dynamicTheme.onCreate(this);

    setContentView(R.layout.payments_activity);

    NavController controller = Navigation.findNavController(this, R.id.nav_host_fragment);
    controller.setGraph(R.navigation.payments_preferences);

    int startingAction = getIntent().getIntExtra(EXTRA_PAYMENTS_STARTING_ACTION, R.id.paymentsHome);
    if (startingAction != R.id.paymentsHome) {
      SafeNavigation.safeNavigate(controller, startingAction, getIntent().getBundleExtra(EXTRA_STARTING_ARGUMENTS));
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    dynamicTheme.onResume(this);

    ApplicationDependencies.getJobManager()
                           .add(PaymentLedgerUpdateJob.updateLedger());
  }
}
