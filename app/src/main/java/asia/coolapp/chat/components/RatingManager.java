package asia.coolapp.chat.components;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.BuildConfig;
import asia.coolapp.chat.R;
import asia.coolapp.chat.util.PlayStoreUtil;
import asia.coolapp.chat.util.TextSecurePreferences;
import asia.coolapp.chat.util.VersionTracker;

import java.util.concurrent.TimeUnit;

public class RatingManager {

  private static final int DAYS_SINCE_INSTALL_THRESHOLD  = 7;
  private static final int DAYS_UNTIL_REPROMPT_THRESHOLD = 4;

  private static final String TAG = Log.tag(RatingManager.class);

  public static void showRatingDialogIfNecessary(Context context) {
    if (!TextSecurePreferences.isRatingEnabled(context) || BuildConfig.PLAY_STORE_DISABLED) return;

    long daysSinceInstall = VersionTracker.getDaysSinceFirstInstalled(context);
    long laterTimestamp   = TextSecurePreferences.getRatingLaterTimestamp(context);

    if (daysSinceInstall >= DAYS_SINCE_INSTALL_THRESHOLD &&
        System.currentTimeMillis() >= laterTimestamp)
    {
      showRatingDialog(context);
    }
  }

  private static void showRatingDialog(final Context context) {
    new AlertDialog.Builder(context)
        .setTitle(R.string.RatingManager_rate_this_app)
        .setMessage(R.string.RatingManager_if_you_enjoy_using_this_app_please_take_a_moment)
        .setPositiveButton(R.string.RatingManager_rate_now, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            TextSecurePreferences.setRatingEnabled(context, false);
            PlayStoreUtil.openPlayStoreOrOurApkDownloadPage(context);
         }
       })
       .setNegativeButton(R.string.RatingManager_no_thanks, new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
           TextSecurePreferences.setRatingEnabled(context, false);
         }
       })
       .setNeutralButton(R.string.RatingManager_later, new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
           long waitUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(DAYS_UNTIL_REPROMPT_THRESHOLD);
           TextSecurePreferences.setRatingLaterTimestamp(context, waitUntil);
         }
       })
       .show();
  }
}
