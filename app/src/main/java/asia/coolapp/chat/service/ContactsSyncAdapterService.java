package asia.coolapp.chat.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import asia.coolapp.chat.contacts.ContactsSyncAdapter;

public class ContactsSyncAdapterService extends Service {

  private static ContactsSyncAdapter syncAdapter;

  @Override
  public synchronized void onCreate() {
    if (syncAdapter == null) {
      syncAdapter = new ContactsSyncAdapter(this, true);
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return syncAdapter.getSyncAdapterBinder();
  }
}
