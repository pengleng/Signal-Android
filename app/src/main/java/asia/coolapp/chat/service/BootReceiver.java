package asia.coolapp.chat.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobs.PushNotificationReceiveJob;

public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    ApplicationDependencies.getJobManager().add(new PushNotificationReceiveJob());
  }
}
