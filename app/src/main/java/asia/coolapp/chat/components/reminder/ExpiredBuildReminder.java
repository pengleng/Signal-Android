package asia.coolapp.chat.components.reminder;

import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.R;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.util.PlayStoreUtil;

import java.util.List;

/**
 * Showed when a build has fully expired (either via the compile-time constant, or remote
 * deprecation).
 */
public class ExpiredBuildReminder extends Reminder {

  public ExpiredBuildReminder(final Context context) {
    super(null, context.getString(R.string.ExpiredBuildReminder_this_version_of_signal_has_expired));

    setOkListener(v -> PlayStoreUtil.openPlayStoreOrOurApkDownloadPage(context));
    addAction(new Action(context.getString(R.string.ExpiredBuildReminder_update_now), R.id.reminder_action_update_now));
  }

  @Override
  public boolean isDismissable() {
    return false;
  }

  @Override
  public List<Action> getActions() {
    return super.getActions();
  }

  @Override
  public @NonNull Importance getImportance() {
    return Importance.TERMINAL;
  }

  public static boolean isEligible() {
    return SignalStore.misc().isClientDeprecated();
  }
}
