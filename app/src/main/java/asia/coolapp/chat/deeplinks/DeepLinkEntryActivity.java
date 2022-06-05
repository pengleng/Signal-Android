package asia.coolapp.chat.deeplinks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import asia.coolapp.chat.MainActivity;
import asia.coolapp.chat.PassphraseRequiredActivity;

public class DeepLinkEntryActivity extends PassphraseRequiredActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    Intent intent = MainActivity.clearTop(this);
    Uri    data   = getIntent().getData();
    intent.setData(data);
    startActivity(intent);
  }
}
