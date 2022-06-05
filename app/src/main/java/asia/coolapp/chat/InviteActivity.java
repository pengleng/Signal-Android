package asia.coolapp.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import asia.coolapp.chat.components.ContactFilterView;
import asia.coolapp.chat.components.ContactFilterView.OnFilterChangedListener;
import asia.coolapp.chat.contacts.ContactsCursorLoader.DisplayMode;
import asia.coolapp.chat.contacts.SelectedContact;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.groups.SelectionLimits;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.sms.MessageSender;
import asia.coolapp.chat.sms.OutgoingTextMessage;
import asia.coolapp.chat.util.DynamicNoActionBarInviteTheme;
import asia.coolapp.chat.util.DynamicTheme;
import asia.coolapp.chat.util.Util;
import asia.coolapp.chat.util.ViewUtil;
import asia.coolapp.chat.util.concurrent.ListenableFuture.Listener;
import asia.coolapp.chat.util.task.ProgressDialogAsyncTask;
import asia.coolapp.chat.util.text.AfterTextChanged;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class InviteActivity extends PassphraseRequiredActivity implements ContactSelectionListFragment.OnContactSelectedListener {

  private       ContactSelectionListFragment contactsFragment;
  private       EditText                     inviteText;
  private       ViewGroup                    smsSendFrame;
  private       Button                       smsSendButton;
  private       Animation                    slideInAnimation;
  private       Animation                    slideOutAnimation;
  private final DynamicTheme                 dynamicTheme = new DynamicNoActionBarInviteTheme();

  @Override
  protected void onPreCreate() {
    super.onPreCreate();
    dynamicTheme.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    getIntent().putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_SMS);
    getIntent().putExtra(ContactSelectionListFragment.SELECTION_LIMITS, SelectionLimits.NO_LIMITS);
    getIntent().putExtra(ContactSelectionListFragment.HIDE_COUNT, true);
    getIntent().putExtra(ContactSelectionListFragment.REFRESHABLE, false);

    setContentView(R.layout.invite_activity);

    initializeAppBar();
    initializeResources();
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }

  private void initializeAppBar() {
    final Toolbar primaryToolbar = findViewById(R.id.toolbar);
    setSupportActionBar(primaryToolbar);

    assert getSupportActionBar() != null;

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(R.string.AndroidManifest__invite_friends);
  }

  private void initializeResources() {
    slideInAnimation  = loadAnimation(R.anim.slide_from_bottom);
    slideOutAnimation = loadAnimation(R.anim.slide_to_bottom);

    View                 shareButton     = findViewById(R.id.share_button);
    TextView             shareText       = findViewById(R.id.share_text);
    View                 smsButton       = findViewById(R.id.sms_button);
    Button               smsCancelButton = findViewById(R.id.cancel_sms_button);
    ContactFilterView    contactFilter   = findViewById(R.id.contact_filter_edit_text);

    inviteText        = findViewById(R.id.invite_text);
    smsSendFrame      = findViewById(R.id.sms_send_frame);
    smsSendButton     = findViewById(R.id.send_sms_button);
    contactsFragment  = (ContactSelectionListFragment)getSupportFragmentManager().findFragmentById(R.id.contact_selection_list_fragment);

    inviteText.setText(getString(R.string.InviteActivity_lets_switch_to_signal, getString(R.string.install_url)));
    inviteText.addTextChangedListener(new AfterTextChanged(editable -> {
      boolean isEnabled = editable.length() > 0;
      smsButton.setEnabled(isEnabled);
      shareButton.setEnabled(isEnabled);
      smsButton.animate().alpha(isEnabled ? 1f : 0.5f);
      shareButton.animate().alpha(isEnabled ? 1f : 0.5f);
    }));

    updateSmsButtonText(contactsFragment.getSelectedContacts().size());

    smsCancelButton.setOnClickListener(new SmsCancelClickListener());
    smsSendButton.setOnClickListener(new SmsSendClickListener());
    contactFilter.setOnFilterChangedListener(new ContactFilterChangedListener());

    if (Util.isDefaultSmsProvider(this)) {
      shareButton.setOnClickListener(new ShareClickListener());
      smsButton.setOnClickListener(new SmsClickListener());
    } else {
      smsButton.setVisibility(View.GONE);
      shareText.setText(R.string.InviteActivity_share);
      shareButton.setOnClickListener(new ShareClickListener());
    }
  }

  private Animation loadAnimation(@AnimRes int animResId) {
    final Animation animation = AnimationUtils.loadAnimation(this, animResId);
    animation.setInterpolator(new FastOutSlowInInterpolator());
    return animation;
  }

  @Override
  public void onBeforeContactSelected(@NonNull Optional<RecipientId> recipientId, String number, @NonNull Consumer<Boolean> callback) {
    updateSmsButtonText(contactsFragment.getSelectedContacts().size() + 1);
    callback.accept(true);
  }

  @Override
  public void onContactDeselected(@NonNull Optional<RecipientId> recipientId, String number) {
    updateSmsButtonText(contactsFragment.getSelectedContacts().size());
  }

  @Override
  public void onSelectionChanged() {
  }

  private void sendSmsInvites() {
    new SendSmsInvitesAsyncTask(this, inviteText.getText().toString())
        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                           contactsFragment.getSelectedContacts()
                                           .toArray(new SelectedContact[0]));
  }

  private void updateSmsButtonText(int count) {
    smsSendButton.setText(getResources().getString(R.string.InviteActivity_send_sms, count));
    smsSendButton.setEnabled(count > 0);
  }

  @Override public void onBackPressed() {
    if (smsSendFrame.getVisibility() == View.VISIBLE) {
      cancelSmsSelection();
    } else {
      super.onBackPressed();
    }
  }

  @Override public boolean onSupportNavigateUp() {
    if (smsSendFrame.getVisibility() == View.VISIBLE) {
      cancelSmsSelection();
      return false;
    } else {
      return super.onSupportNavigateUp();
    }
  }

  private void cancelSmsSelection() {
    contactsFragment.reset();
    updateSmsButtonText(contactsFragment.getSelectedContacts().size());
    ViewUtil.animateOut(smsSendFrame, slideOutAnimation, View.GONE);
  }

  private class ShareClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      Intent sendIntent = new Intent();
      sendIntent.setAction(Intent.ACTION_SEND);
      sendIntent.putExtra(Intent.EXTRA_TEXT, inviteText.getText().toString());
      sendIntent.setType("text/plain");
      if (sendIntent.resolveActivity(getPackageManager()) != null) {
        startActivity(Intent.createChooser(sendIntent, getString(R.string.InviteActivity_invite_to_signal)));
      } else {
        Toast.makeText(InviteActivity.this, R.string.InviteActivity_no_app_to_share_to, Toast.LENGTH_LONG).show();
      }
    }
  }

  private class SmsClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      ViewUtil.animateIn(smsSendFrame, slideInAnimation);
    }
  }

  private class SmsCancelClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      cancelSmsSelection();
    }
  }

  private class SmsSendClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      new AlertDialog.Builder(InviteActivity.this)
          .setTitle(getResources().getQuantityString(R.plurals.InviteActivity_send_sms_invites,
                                                     contactsFragment.getSelectedContacts().size(),
                                                     contactsFragment.getSelectedContacts().size()))
          .setMessage(inviteText.getText().toString())
          .setPositiveButton(R.string.yes, (dialog, which) -> sendSmsInvites())
          .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
          .show();
    }
  }

  private class ContactFilterChangedListener implements OnFilterChangedListener {
    @Override
    public void onFilterChanged(String filter) {
      contactsFragment.setQueryFilter(filter);
    }
  }

  @SuppressLint("StaticFieldLeak")
  private class SendSmsInvitesAsyncTask extends ProgressDialogAsyncTask<SelectedContact,Void,Void> {
    private final String message;

    SendSmsInvitesAsyncTask(Context context, String message) {
      super(context, R.string.InviteActivity_sending, R.string.InviteActivity_sending);
      this.message = message;
    }

    @Override
    protected Void doInBackground(SelectedContact... contacts) {
      final Context context = getContext();
      if (context == null) return null;

      for (SelectedContact contact : contacts) {
        RecipientId recipientId    = contact.getOrCreateRecipientId(context);
        Recipient   recipient      = Recipient.resolved(recipientId);
        int         subscriptionId = recipient.getDefaultSubscriptionId().orElse(-1);

        MessageSender.send(context, new OutgoingTextMessage(recipient, message, subscriptionId), -1L, true, null, null);

        if (recipient.getContactUri() != null) {
          SignalDatabase.recipients().setHasSentInvite(recipient.getId());
        }
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      final Context context = getContext();
      if (context == null) return;

      ViewUtil.animateOut(smsSendFrame, slideOutAnimation, View.GONE).addListener(new Listener<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          contactsFragment.reset();
        }

        @Override
        public void onFailure(ExecutionException e) {}
      });
      Toast.makeText(context, R.string.InviteActivity_invitations_sent, Toast.LENGTH_LONG).show();
    }
  }
}
