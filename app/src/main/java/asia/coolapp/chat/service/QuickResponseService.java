package asia.coolapp.chat.service;

import android.app.IntentService;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.R;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.sms.MessageSender;
import asia.coolapp.chat.sms.OutgoingTextMessage;
import asia.coolapp.chat.util.Rfc5724Uri;

import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

public class QuickResponseService extends IntentService {

  private static final String TAG = Log.tag(QuickResponseService.class);

  public QuickResponseService() {
    super("QuickResponseService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (!TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(intent.getAction())) {
      Log.w(TAG, "Received unknown intent: " + intent.getAction());
      return;
    }

    if (KeyCachingService.isLocked(this)) {
      Log.w(TAG, "Got quick response request when locked...");
      Toast.makeText(this, R.string.QuickResponseService_quick_response_unavailable_when_Signal_is_locked, Toast.LENGTH_LONG).show();
      return;
    }

    try {
      Rfc5724Uri uri        = new Rfc5724Uri(intent.getDataString());
      String     content    = intent.getStringExtra(Intent.EXTRA_TEXT);
      String     number     = uri.getPath();

      if (number.contains("%")){
        number = URLDecoder.decode(number);
      }

      Recipient recipient      = Recipient.external(this, number);
      int       subscriptionId = recipient.getDefaultSubscriptionId().orElse(-1);
      long      expiresIn      = TimeUnit.SECONDS.toMillis(recipient.getExpiresInSeconds());

      if (!TextUtils.isEmpty(content)) {
        MessageSender.send(this, new OutgoingTextMessage(recipient, content, expiresIn, subscriptionId), -1, false, null, null);
      }
    } catch (URISyntaxException e) {
      Toast.makeText(this, R.string.QuickResponseService_problem_sending_message, Toast.LENGTH_LONG).show();
      Log.w(TAG, e);
    }
  }
}
