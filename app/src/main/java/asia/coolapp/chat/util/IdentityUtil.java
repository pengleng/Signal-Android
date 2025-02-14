package asia.coolapp.chat.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.state.SessionRecord;
import org.signal.libsignal.protocol.state.SessionStore;
import asia.coolapp.chat.R;
import asia.coolapp.chat.crypto.ReentrantSessionLock;
import asia.coolapp.chat.crypto.storage.SignalIdentityKeyStore;
import asia.coolapp.chat.database.GroupDatabase;
import asia.coolapp.chat.database.IdentityDatabase;
import asia.coolapp.chat.database.MessageDatabase;
import asia.coolapp.chat.database.MessageDatabase.InsertResult;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.model.IdentityRecord;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.sms.IncomingIdentityDefaultMessage;
import asia.coolapp.chat.sms.IncomingIdentityUpdateMessage;
import asia.coolapp.chat.sms.IncomingIdentityVerifiedMessage;
import asia.coolapp.chat.sms.IncomingTextMessage;
import asia.coolapp.chat.sms.OutgoingIdentityDefaultMessage;
import asia.coolapp.chat.sms.OutgoingIdentityVerifiedMessage;
import asia.coolapp.chat.sms.OutgoingTextMessage;
import asia.coolapp.chat.util.concurrent.ListenableFuture;
import asia.coolapp.chat.util.concurrent.SettableFuture;
import asia.coolapp.chat.util.concurrent.SimpleTask;
import org.whispersystems.signalservice.api.SignalSessionLock;
import org.whispersystems.signalservice.api.messages.multidevice.VerifiedMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

import java.util.List;
import java.util.Optional;

public final class IdentityUtil {

  private IdentityUtil() {}

  private static final String TAG = Log.tag(IdentityUtil.class);

  public static ListenableFuture<Optional<IdentityRecord>> getRemoteIdentityKey(final Context context, final Recipient recipient) {
    final SettableFuture<Optional<IdentityRecord>> future      = new SettableFuture<>();
    final RecipientId                              recipientId = recipient.getId();

    SimpleTask.run(SignalExecutors.BOUNDED,
                   () -> ApplicationDependencies.getProtocolStore().aci().identities().getIdentityRecord(recipientId),
                   future::set);

    return future;
  }

  public static void markIdentityVerified(Context context, Recipient recipient, boolean verified, boolean remote)
  {
    long            time          = System.currentTimeMillis();
    MessageDatabase smsDatabase   = SignalDatabase.sms();
    GroupDatabase   groupDatabase = SignalDatabase.groups();

    try (GroupDatabase.Reader reader = groupDatabase.getGroups()) {

      GroupDatabase.GroupRecord groupRecord;

      while ((groupRecord = reader.getNext()) != null) {
        if (groupRecord.getMembers().contains(recipient.getId()) && groupRecord.isActive() && !groupRecord.isMms()) {

          if (remote) {
            IncomingTextMessage incoming = new IncomingTextMessage(recipient.getId(), 1, time, -1, time, null, Optional.of(groupRecord.getId()), 0, false, null);

            if (verified) incoming = new IncomingIdentityVerifiedMessage(incoming);
            else          incoming = new IncomingIdentityDefaultMessage(incoming);

            smsDatabase.insertMessageInbox(incoming);
          } else {
            RecipientId         recipientId    = SignalDatabase.recipients().getOrInsertFromGroupId(groupRecord.getId());
            Recipient           groupRecipient = Recipient.resolved(recipientId);
            long                threadId       = SignalDatabase.threads().getOrCreateThreadIdFor(groupRecipient);
            OutgoingTextMessage outgoing ;

            if (verified) outgoing = new OutgoingIdentityVerifiedMessage(recipient);
            else          outgoing = new OutgoingIdentityDefaultMessage(recipient);

            SignalDatabase.sms().insertMessageOutbox(threadId, outgoing, false, time, null);
            SignalDatabase.threads().update(threadId, true);
          }
        }
      }
    }

    if (remote) {
      IncomingTextMessage incoming = new IncomingTextMessage(recipient.getId(), 1, time, -1, time, null, Optional.empty(), 0, false, null);

      if (verified) incoming = new IncomingIdentityVerifiedMessage(incoming);
      else          incoming = new IncomingIdentityDefaultMessage(incoming);

      smsDatabase.insertMessageInbox(incoming);
    } else {
      OutgoingTextMessage outgoing;

      if (verified) outgoing = new OutgoingIdentityVerifiedMessage(recipient);
      else          outgoing = new OutgoingIdentityDefaultMessage(recipient);

      long threadId = SignalDatabase.threads().getOrCreateThreadIdFor(recipient);

      Log.i(TAG, "Inserting verified outbox...");
      SignalDatabase.sms().insertMessageOutbox(threadId, outgoing, false, time, null);
      SignalDatabase.threads().update(threadId, true);
    }
  }

  public static void markIdentityUpdate(@NonNull Context context, @NonNull RecipientId recipientId) {
    long            time          = System.currentTimeMillis();
    MessageDatabase smsDatabase   = SignalDatabase.sms();
    GroupDatabase   groupDatabase = SignalDatabase.groups();

    try (GroupDatabase.Reader reader = groupDatabase.getGroups()) {
      GroupDatabase.GroupRecord groupRecord;

      while ((groupRecord = reader.getNext()) != null) {
        if (groupRecord.getMembers().contains(recipientId) && groupRecord.isActive()) {
          IncomingTextMessage           incoming    = new IncomingTextMessage(recipientId, 1, time, time, time, null, Optional.of(groupRecord.getId()), 0, false, null);
          IncomingIdentityUpdateMessage groupUpdate = new IncomingIdentityUpdateMessage(incoming);

          smsDatabase.insertMessageInbox(groupUpdate);
        }
      }
    }

    IncomingTextMessage           incoming         = new IncomingTextMessage(recipientId, 1, time, -1, time, null, Optional.empty(), 0, false, null);
    IncomingIdentityUpdateMessage individualUpdate = new IncomingIdentityUpdateMessage(incoming);
    Optional<InsertResult>        insertResult     = smsDatabase.insertMessageInbox(individualUpdate);

    if (insertResult.isPresent()) {
      ApplicationDependencies.getMessageNotifier().updateNotification(context, insertResult.get().getThreadId());
    }
  }

  public static void saveIdentity(String user, IdentityKey identityKey) {
    try(SignalSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      SessionStore          sessionStore     = ApplicationDependencies.getProtocolStore().aci();
      SignalProtocolAddress address          = new SignalProtocolAddress(user, SignalServiceAddress.DEFAULT_DEVICE_ID);

      if (ApplicationDependencies.getProtocolStore().aci().identities().saveIdentity(address, identityKey)) {
        if (sessionStore.containsSession(address)) {
          SessionRecord sessionRecord = sessionStore.loadSession(address);
          sessionRecord.archiveCurrentState();

          sessionStore.storeSession(address, sessionRecord);
        }
      }
    }
  }

  public static void processVerifiedMessage(Context context, VerifiedMessage verifiedMessage) {
    try(SignalSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      SignalIdentityKeyStore   identityStore  = ApplicationDependencies.getProtocolStore().aci().identities();
      Recipient                recipient      = Recipient.externalPush(verifiedMessage.getDestination());
      Optional<IdentityRecord> identityRecord = identityStore.getIdentityRecord(recipient.getId());

      if (!identityRecord.isPresent() && verifiedMessage.getVerified() == VerifiedMessage.VerifiedState.DEFAULT) {
        Log.w(TAG, "No existing record for default status");
        return;
      }

      if (verifiedMessage.getVerified() == VerifiedMessage.VerifiedState.DEFAULT              &&
          identityRecord.isPresent()                                                          &&
          identityRecord.get().getIdentityKey().equals(verifiedMessage.getIdentityKey())      &&
          identityRecord.get().getVerifiedStatus() != IdentityDatabase.VerifiedStatus.DEFAULT)
      {
        identityStore.setVerified(recipient.getId(), identityRecord.get().getIdentityKey(), IdentityDatabase.VerifiedStatus.DEFAULT);
        markIdentityVerified(context, recipient, false, true);
      }

      if (verifiedMessage.getVerified() == VerifiedMessage.VerifiedState.VERIFIED &&
          (!identityRecord.isPresent() ||
              (identityRecord.isPresent() && !identityRecord.get().getIdentityKey().equals(verifiedMessage.getIdentityKey())) ||
              (identityRecord.isPresent() && identityRecord.get().getVerifiedStatus() != IdentityDatabase.VerifiedStatus.VERIFIED)))
      {
        saveIdentity(verifiedMessage.getDestination().getIdentifier(), verifiedMessage.getIdentityKey());
        identityStore.setVerified(recipient.getId(), verifiedMessage.getIdentityKey(), IdentityDatabase.VerifiedStatus.VERIFIED);
        markIdentityVerified(context, recipient, true, true);
      }
    }
  }


  public static @Nullable String getUnverifiedBannerDescription(@NonNull Context context,
                                                                @NonNull List<Recipient> unverified)
  {
    return getPluralizedIdentityDescription(context, unverified,
                                            R.string.IdentityUtil_unverified_banner_one,
                                            R.string.IdentityUtil_unverified_banner_two,
                                            R.string.IdentityUtil_unverified_banner_many);
  }

  public static @Nullable String getUnverifiedSendDialogDescription(@NonNull Context context,
                                                                    @NonNull List<Recipient> unverified)
  {
    return getPluralizedIdentityDescription(context, unverified,
                                            R.string.IdentityUtil_unverified_dialog_one,
                                            R.string.IdentityUtil_unverified_dialog_two,
                                            R.string.IdentityUtil_unverified_dialog_many);
  }

  public static @Nullable String getUntrustedSendDialogDescription(@NonNull Context context,
                                                                   @NonNull List<Recipient> untrusted)
  {
    return getPluralizedIdentityDescription(context, untrusted,
                                            R.string.IdentityUtil_untrusted_dialog_one,
                                            R.string.IdentityUtil_untrusted_dialog_two,
                                            R.string.IdentityUtil_untrusted_dialog_many);
  }

  private static @Nullable String getPluralizedIdentityDescription(@NonNull Context context,
                                                                   @NonNull List<Recipient> recipients,
                                                                   @StringRes int resourceOne,
                                                                   @StringRes int resourceTwo,
                                                                   @StringRes int resourceMany)
  {
    if (recipients.isEmpty()) return null;

    if (recipients.size() == 1) {
      String name = recipients.get(0).getDisplayName(context);
      return context.getString(resourceOne, name);
    } else {
      String firstName  = recipients.get(0).getDisplayName(context);
      String secondName = recipients.get(1).getDisplayName(context);

      if (recipients.size() == 2) {
        return context.getString(resourceTwo, firstName, secondName);
      } else {
        int    othersCount = recipients.size() - 2;
        String nMore       = context.getResources().getQuantityString(R.plurals.identity_others, othersCount, othersCount);

        return context.getString(resourceMany, firstName, secondName, nMore);
      }
    }
  }
}
