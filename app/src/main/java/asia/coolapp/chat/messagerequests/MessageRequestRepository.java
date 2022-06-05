package asia.coolapp.chat.messagerequests;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.signal.storageservice.protos.groups.local.DecryptedGroup;
import asia.coolapp.chat.database.GroupDatabase;
import asia.coolapp.chat.database.MessageDatabase;
import asia.coolapp.chat.database.RecipientDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.ThreadDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.groups.GroupChangeException;
import asia.coolapp.chat.groups.GroupManager;
import asia.coolapp.chat.groups.ui.GroupChangeErrorCallback;
import asia.coolapp.chat.groups.ui.GroupChangeFailureReason;
import asia.coolapp.chat.jobs.MultiDeviceMessageRequestResponseJob;
import asia.coolapp.chat.jobs.ReportSpamJob;
import asia.coolapp.chat.jobs.SendViewedReceiptJob;
import asia.coolapp.chat.notifications.MarkReadReceiver;
import asia.coolapp.chat.recipients.LiveRecipient;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.recipients.RecipientUtil;
import asia.coolapp.chat.sms.MessageSender;
import asia.coolapp.chat.util.FeatureFlags;
import asia.coolapp.chat.util.TextSecurePreferences;
import org.whispersystems.signalservice.internal.push.exceptions.GroupPatchNotAcceptedException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

final class MessageRequestRepository {

  private static final String TAG = Log.tag(MessageRequestRepository.class);

  private final Context  context;
  private final Executor executor;

  MessageRequestRepository(@NonNull Context context) {
    this.context  = context.getApplicationContext();
    this.executor = SignalExecutors.BOUNDED;
  }

  void getGroups(@NonNull RecipientId recipientId, @NonNull Consumer<List<String>> onGroupsLoaded) {
    executor.execute(() -> {
      GroupDatabase groupDatabase = SignalDatabase.groups();
      onGroupsLoaded.accept(groupDatabase.getPushGroupNamesContainingMember(recipientId));
    });
  }

  void getGroupInfo(@NonNull RecipientId recipientId, @NonNull Consumer<GroupInfo> onGroupInfoLoaded) {
    executor.execute(() -> {
      GroupDatabase                       groupDatabase = SignalDatabase.groups();
      Optional<GroupDatabase.GroupRecord> groupRecord   = groupDatabase.getGroup(recipientId);
      onGroupInfoLoaded.accept(groupRecord.map(record -> {
        if (record.isV2Group()) {
          DecryptedGroup decryptedGroup = record.requireV2GroupProperties().getDecryptedGroup();
          return new GroupInfo(decryptedGroup.getMembersCount(), decryptedGroup.getPendingMembersCount(), decryptedGroup.getDescription());
        } else {
          return new GroupInfo(record.getMembers().size(), 0, "");
        }
      }).orElse(GroupInfo.ZERO));
    });
  }

  @WorkerThread
  @NonNull MessageRequestState getMessageRequestState(@NonNull Recipient recipient, long threadId) {
    if (recipient.isBlocked()) {
      if (recipient.isGroup()) {
        return MessageRequestState.BLOCKED_GROUP;
      } else {
        return MessageRequestState.BLOCKED_INDIVIDUAL;
      }
    } else if (threadId <= 0) {
      return MessageRequestState.NONE;
    } else if (recipient.isPushV2Group()) {
      switch (getGroupMemberLevel(recipient.getId())) {
        case NOT_A_MEMBER:
          return MessageRequestState.NONE;
        case PENDING_MEMBER:
          return MessageRequestState.GROUP_V2_INVITE;
        default:
          if (RecipientUtil.isMessageRequestAccepted(context, threadId)) {
            return MessageRequestState.NONE;
          } else {
            return MessageRequestState.GROUP_V2_ADD;
          }
      }
    } else if (!RecipientUtil.isLegacyProfileSharingAccepted(recipient) && isLegacyThread(recipient)) {
      if (recipient.isGroup()) {
        return MessageRequestState.LEGACY_GROUP_V1;
      } else {
        return MessageRequestState.LEGACY_INDIVIDUAL;
      }
    } else if (recipient.isPushV1Group()) {
      if (RecipientUtil.isMessageRequestAccepted(context, threadId)) {
        if (recipient.getParticipants().size() > FeatureFlags.groupLimits().getHardLimit()) {
          return MessageRequestState.DEPRECATED_GROUP_V1_TOO_LARGE;
        } else {
          return MessageRequestState.DEPRECATED_GROUP_V1;
        }
      } else if (!recipient.isActiveGroup()) {
        return MessageRequestState.NONE;
      } else {
        return MessageRequestState.GROUP_V1;
      }
    } else {
      if (RecipientUtil.isMessageRequestAccepted(context, threadId)) {
        return MessageRequestState.NONE;
      } else {
        return MessageRequestState.INDIVIDUAL;
      }
    }
  }

  void acceptMessageRequest(@NonNull LiveRecipient liveRecipient,
                            long threadId,
                            @NonNull Runnable onMessageRequestAccepted,
                            @NonNull GroupChangeErrorCallback error)
  {
    executor.execute(()-> {
      if (liveRecipient.get().isPushV2Group()) {
        try {
          Log.i(TAG, "GV2 accepting invite");
          GroupManager.acceptInvite(context, liveRecipient.get().requireGroupId().requireV2());

          RecipientDatabase recipientDatabase = SignalDatabase.recipients();
          recipientDatabase.setProfileSharing(liveRecipient.getId(), true);

          onMessageRequestAccepted.run();
        } catch (GroupChangeException | IOException e) {
          Log.w(TAG, e);
          error.onError(GroupChangeFailureReason.fromException(e));
        }
      } else {
        RecipientDatabase recipientDatabase = SignalDatabase.recipients();
        recipientDatabase.setProfileSharing(liveRecipient.getId(), true);

        MessageSender.sendProfileKey(context, threadId);

        List<MessageDatabase.MarkedMessageInfo> messageIds = SignalDatabase.threads().setEntireThreadRead(threadId);
        ApplicationDependencies.getMessageNotifier().updateNotification(context);
        MarkReadReceiver.process(context, messageIds);

        List<MessageDatabase.MarkedMessageInfo> viewedInfos = SignalDatabase.mms().getViewedIncomingMessages(threadId);

        SendViewedReceiptJob.enqueue(threadId, liveRecipient.getId(), viewedInfos);

        if (TextSecurePreferences.isMultiDevice(context)) {
          ApplicationDependencies.getJobManager().add(MultiDeviceMessageRequestResponseJob.forAccept(liveRecipient.getId()));
        }

        onMessageRequestAccepted.run();
      }
    });
  }

  void deleteMessageRequest(@NonNull LiveRecipient recipient,
                            long threadId,
                            @NonNull Runnable onMessageRequestDeleted,
                            @NonNull GroupChangeErrorCallback error)
  {
    executor.execute(() -> {
      Recipient resolved = recipient.resolve();

      if (resolved.isGroup() && resolved.requireGroupId().isPush()) {
        try {
          GroupManager.leaveGroupFromBlockOrMessageRequest(context, resolved.requireGroupId().requirePush());
        } catch (GroupChangeException | GroupPatchNotAcceptedException e) {
          if (SignalDatabase.groups().isCurrentMember(resolved.requireGroupId().requirePush(), Recipient.self().getId())) {
            Log.w(TAG, "Failed to leave group, and we're still a member.", e);
            error.onError(GroupChangeFailureReason.fromException(e));
            return;
          } else {
            Log.w(TAG, "Failed to leave group, but we're not a member, so ignoring.");
          }
        } catch (IOException e) {
          Log.w(TAG, e);
          error.onError(GroupChangeFailureReason.fromException(e));
          return;
        }
      }

      if (TextSecurePreferences.isMultiDevice(context)) {
        ApplicationDependencies.getJobManager().add(MultiDeviceMessageRequestResponseJob.forDelete(recipient.getId()));
      }

      ThreadDatabase threadDatabase = SignalDatabase.threads();
      threadDatabase.deleteConversation(threadId);

      onMessageRequestDeleted.run();
    });
  }

  void blockMessageRequest(@NonNull LiveRecipient liveRecipient,
                           @NonNull Runnable onMessageRequestBlocked,
                           @NonNull GroupChangeErrorCallback error)
  {
    executor.execute(() -> {
      Recipient recipient = liveRecipient.resolve();
      try {
        RecipientUtil.block(context, recipient);
      } catch (GroupChangeException | IOException e) {
        Log.w(TAG, e);
        error.onError(GroupChangeFailureReason.fromException(e));
        return;
      }
      liveRecipient.refresh();

      if (TextSecurePreferences.isMultiDevice(context)) {
        ApplicationDependencies.getJobManager().add(MultiDeviceMessageRequestResponseJob.forBlock(liveRecipient.getId()));
      }

      onMessageRequestBlocked.run();
    });
  }

  void blockAndReportSpamMessageRequest(@NonNull LiveRecipient liveRecipient,
                                        long threadId,
                                        @NonNull Runnable onMessageRequestBlocked,
                                        @NonNull GroupChangeErrorCallback error)
  {
    executor.execute(() -> {
      Recipient recipient = liveRecipient.resolve();
      try{
        RecipientUtil.block(context, recipient);
      } catch (GroupChangeException | IOException e) {
        Log.w(TAG, e);
        error.onError(GroupChangeFailureReason.fromException(e));
        return;
      }
      liveRecipient.refresh();

      ApplicationDependencies.getJobManager().add(new ReportSpamJob(threadId, System.currentTimeMillis()));

      if (TextSecurePreferences.isMultiDevice(context)) {
        ApplicationDependencies.getJobManager().add(MultiDeviceMessageRequestResponseJob.forBlockAndReportSpam(liveRecipient.getId()));
      }

      onMessageRequestBlocked.run();
    });
  }

  void unblockAndAccept(@NonNull LiveRecipient liveRecipient, long threadId, @NonNull Runnable onMessageRequestUnblocked) {
    executor.execute(() -> {
      Recipient recipient = liveRecipient.resolve();

      RecipientUtil.unblock(context, recipient);

      if (TextSecurePreferences.isMultiDevice(context)) {
        ApplicationDependencies.getJobManager().add(MultiDeviceMessageRequestResponseJob.forAccept(liveRecipient.getId()));
      }

      onMessageRequestUnblocked.run();
    });
  }

  private GroupDatabase.MemberLevel getGroupMemberLevel(@NonNull RecipientId recipientId) {
    return SignalDatabase.groups()
                          .getGroup(recipientId)
                          .map(g -> g.memberLevel(Recipient.self()))
                          .orElse(GroupDatabase.MemberLevel.NOT_A_MEMBER);
  }


  @WorkerThread
  private boolean isLegacyThread(@NonNull Recipient recipient) {
    Context context  = ApplicationDependencies.getApplication();
    Long    threadId = SignalDatabase.threads().getThreadIdFor(recipient.getId());

    return threadId != null &&
        (RecipientUtil.hasSentMessageInThread(context, threadId) || RecipientUtil.isPreMessageRequestThread(context, threadId));
  }
}
