package asia.coolapp.chat.migrations;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.MainActivity;
import asia.coolapp.chat.NewConversationActivity;
import asia.coolapp.chat.R;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.ThreadDatabase;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.notifications.NotificationChannels;
import asia.coolapp.chat.notifications.NotificationIds;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.SetUtil;
import asia.coolapp.chat.util.TextSecurePreferences;

import java.util.List;
import java.util.Set;

/**
 * Show a user that contacts are newly available. Only for users that recently installed.
 */
public class UserNotificationMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(UserNotificationMigrationJob.class);

  public static final String KEY =  "UserNotificationMigration";

  UserNotificationMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private UserNotificationMigrationJob(Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  boolean isUiBlocking() {
    return false;
  }

  @Override
  void performMigration() {
    if (!SignalStore.account().isRegistered()   ||
        SignalStore.account().getE164() == null ||
        SignalStore.account().getAci() == null)
    {
      Log.w(TAG, "Not registered! Skipping.");
      return;
    }

    if (!SignalStore.settings().isNotifyWhenContactJoinsSignal()) {
      Log.w(TAG, "New contact notifications disabled! Skipping.");
      return;
    }

    if (TextSecurePreferences.getFirstInstallVersion(context) < 759) {
      Log.w(TAG, "Install is older than v5.0.8. Skipping.");
      return;
    }

    ThreadDatabase threadDatabase = SignalDatabase.threads();

    int threadCount = threadDatabase.getUnarchivedConversationListCount() +
                      threadDatabase.getArchivedConversationListCount();

    if (threadCount >= 3) {
      Log.w(TAG, "Already have 3 or more threads. Skipping.");
      return;
    }

    List<RecipientId> registered               = SignalDatabase.recipients().getRegistered();
    List<RecipientId> systemContacts           = SignalDatabase.recipients().getSystemContacts();
    Set<RecipientId>  registeredSystemContacts = SetUtil.intersection(registered, systemContacts);
    Set<RecipientId>  threadRecipients         = threadDatabase.getAllThreadRecipients();

    if (threadRecipients.containsAll(registeredSystemContacts)) {
      Log.w(TAG, "Threads already exist for all relevant contacts. Skipping.");
      return;
    }

    String message = context.getResources().getQuantityString(R.plurals.UserNotificationMigrationJob_d_contacts_are_on_signal,
                                                              registeredSystemContacts.size(),
                                                              registeredSystemContacts.size());

    Intent        mainActivityIntent    = new Intent(context, MainActivity.class);
    Intent        newConversationIntent = new Intent(context, NewConversationActivity.class);
    PendingIntent pendingIntent         = TaskStackBuilder.create(context)
                                                          .addNextIntent(mainActivityIntent)
                                                          .addNextIntent(newConversationIntent)
                                                          .getPendingIntent(0, 0);

    Notification notification = new NotificationCompat.Builder(context, NotificationChannels.getMessagesChannel(context))
                                                      .setSmallIcon(R.drawable.ic_notification)
                                                      .setContentText(message)
                                                      .setContentIntent(pendingIntent)
                                                      .build();

    try {
      NotificationManagerCompat.from(context)
                               .notify(NotificationIds.USER_NOTIFICATION_MIGRATION, notification);
    } catch (Throwable t) {
      Log.w(TAG, "Failed to notify!", t);
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static final class Factory implements Job.Factory<UserNotificationMigrationJob> {

    @Override
    public @NonNull UserNotificationMigrationJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new UserNotificationMigrationJob(parameters);
    }
  }
}
