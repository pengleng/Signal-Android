package asia.coolapp.chat;

import android.content.Context;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.insights.InsightsOptOut;
import asia.coolapp.chat.jobmanager.JobManager;
import asia.coolapp.chat.jobs.EmojiSearchIndexDownloadJob;
import asia.coolapp.chat.jobs.StickerPackDownloadJob;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.migrations.ApplicationMigrations;
import asia.coolapp.chat.stickers.BlessedPacks;
import asia.coolapp.chat.util.TextSecurePreferences;
import asia.coolapp.chat.util.Util;

/**
 * Rule of thumb: if there's something you want to do on the first app launch that involves
 * persisting state to the database, you'll almost certainly *also* want to do it post backup
 * restore, since a backup restore will wipe the current state of the database.
 */
public final class AppInitialization {

  private static final String TAG = Log.tag(AppInitialization.class);

  private AppInitialization() {}

  public static void onFirstEverAppLaunch(@NonNull Context context) {
    Log.i(TAG, "onFirstEverAppLaunch()");

    InsightsOptOut.userRequestedOptOut(context);
    TextSecurePreferences.setAppMigrationVersion(context, ApplicationMigrations.CURRENT_VERSION);
    TextSecurePreferences.setJobManagerVersion(context, JobManager.CURRENT_VERSION);
    TextSecurePreferences.setLastVersionCode(context, Util.getCanonicalVersionCode());
    TextSecurePreferences.setHasSeenStickerIntroTooltip(context, true);
    TextSecurePreferences.setPasswordDisabled(context, true);
    TextSecurePreferences.setReadReceiptsEnabled(context, true);
    TextSecurePreferences.setTypingIndicatorsEnabled(context, true);
    TextSecurePreferences.setHasSeenWelcomeScreen(context, false);
    ApplicationDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onFirstEverAppLaunch();
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.ZOZO.getPackId(), BlessedPacks.ZOZO.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.BANDIT.getPackId(), BlessedPacks.BANDIT.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.DAY_BY_DAY.getPackId(), BlessedPacks.DAY_BY_DAY.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_HANDS.getPackId(), BlessedPacks.SWOON_HANDS.getPackKey()));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_FACES.getPackId(), BlessedPacks.SWOON_FACES.getPackKey()));
  }

  public static void onPostBackupRestore(@NonNull Context context) {
    Log.i(TAG, "onPostBackupRestore()");

    ApplicationDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onPostBackupRestore();
    SignalStore.onFirstEverAppLaunch();
    SignalStore.onboarding().clearAll();
    TextSecurePreferences.onPostBackupRestore(context);
    TextSecurePreferences.setPasswordDisabled(context, true);
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.ZOZO.getPackId(), BlessedPacks.ZOZO.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.BANDIT.getPackId(), BlessedPacks.BANDIT.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.DAY_BY_DAY.getPackId(), BlessedPacks.DAY_BY_DAY.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_HANDS.getPackId(), BlessedPacks.SWOON_HANDS.getPackKey()));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_FACES.getPackId(), BlessedPacks.SWOON_FACES.getPackKey()));
    EmojiSearchIndexDownloadJob.scheduleImmediately();
  }

  /**
   * Temporary migration method that does the safest bits of {@link #onFirstEverAppLaunch(Context)}
   */
  public static void onRepairFirstEverAppLaunch(@NonNull Context context) {
    Log.w(TAG, "onRepairFirstEverAppLaunch()");

    InsightsOptOut.userRequestedOptOut(context);
    TextSecurePreferences.setAppMigrationVersion(context, ApplicationMigrations.CURRENT_VERSION);
    TextSecurePreferences.setJobManagerVersion(context, JobManager.CURRENT_VERSION);
    TextSecurePreferences.setLastVersionCode(context, Util.getCanonicalVersionCode());
    TextSecurePreferences.setHasSeenStickerIntroTooltip(context, true);
    TextSecurePreferences.setPasswordDisabled(context, true);
    ApplicationDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onFirstEverAppLaunch();
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.ZOZO.getPackId(), BlessedPacks.ZOZO.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.BANDIT.getPackId(), BlessedPacks.BANDIT.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.DAY_BY_DAY.getPackId(), BlessedPacks.DAY_BY_DAY.getPackKey(), false));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_HANDS.getPackId(), BlessedPacks.SWOON_HANDS.getPackKey()));
    ApplicationDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_FACES.getPackId(), BlessedPacks.SWOON_FACES.getPackKey()));
  }
}
