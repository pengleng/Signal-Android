package asia.coolapp.chat.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import asia.coolapp.chat.jobs.EmojiSearchIndexDownloadJob;

public class LocaleChangedReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    NotificationChannels.create(context);
    EmojiSearchIndexDownloadJob.scheduleImmediately();
  }
}
