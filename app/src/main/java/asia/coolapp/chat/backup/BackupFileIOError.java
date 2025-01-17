package asia.coolapp.chat.backup;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import asia.coolapp.chat.R;
import asia.coolapp.chat.components.settings.app.AppSettingsActivity;
import asia.coolapp.chat.notifications.NotificationCancellationHelper;
import asia.coolapp.chat.notifications.NotificationChannels;

import java.io.IOException;

public enum BackupFileIOError {
  ACCESS_ERROR(R.string.LocalBackupJobApi29_backup_failed, R.string.LocalBackupJobApi29_your_backup_directory_has_been_deleted_or_moved),
  FILE_TOO_LARGE(R.string.LocalBackupJobApi29_backup_failed, R.string.LocalBackupJobApi29_your_backup_file_is_too_large),
  NOT_ENOUGH_SPACE(R.string.LocalBackupJobApi29_backup_failed, R.string.LocalBackupJobApi29_there_is_not_enough_space),
  UNKNOWN(R.string.LocalBackupJobApi29_backup_failed, R.string.LocalBackupJobApi29_tap_to_manage_backups);

  private static final short BACKUP_FAILED_ID = 31321;

  private final @StringRes int titleId;
  private final @StringRes int messageId;

  BackupFileIOError(@StringRes int titleId, @StringRes int messageId) {
    this.titleId     = titleId;
    this.messageId   = messageId;
  }

  public static void clearNotification(@NonNull Context context) {
    NotificationCancellationHelper.cancelLegacy(context, BACKUP_FAILED_ID);
  }

  public void postNotification(@NonNull Context context) {
    PendingIntent pendingIntent           = PendingIntent.getActivity(context, -1, AppSettingsActivity.backups(context), 0);
    Notification backupFailedNotification = new NotificationCompat.Builder(context, NotificationChannels.FAILURES)
                                                                  .setSmallIcon(R.drawable.ic_signal_backup)
                                                                  .setContentTitle(context.getString(titleId))
                                                                  .setContentText(context.getString(messageId))
                                                                  .setContentIntent(pendingIntent)
                                                                  .build();

    NotificationManagerCompat.from(context)
                             .notify(BACKUP_FAILED_ID, backupFailedNotification);
  }

  public static void postNotificationForException(@NonNull Context context, @NonNull IOException e, int runAttempt) {
    BackupFileIOError error = getFromException(e);

    if (error != null) {
      error.postNotification(context);
    }

    if (error == null && runAttempt > 0) {
      UNKNOWN.postNotification(context);
    }
  }

  private static @Nullable BackupFileIOError getFromException(@NonNull IOException e) {
    if (e.getMessage() != null) {
           if (e.getMessage().contains("EFBIG"))  return FILE_TOO_LARGE;
      else if (e.getMessage().contains("ENOSPC")) return NOT_ENOUGH_SPACE;
    }

    return null;
  }
}
