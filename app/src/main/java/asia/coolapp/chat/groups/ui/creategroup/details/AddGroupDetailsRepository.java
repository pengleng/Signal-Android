package asia.coolapp.chat.groups.ui.creategroup.details;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.annimon.stream.Stream;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.groups.GroupChangeBusyException;
import asia.coolapp.chat.groups.GroupChangeException;
import asia.coolapp.chat.groups.GroupManager;
import asia.coolapp.chat.groups.ui.GroupMemberEntry;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AddGroupDetailsRepository {

  private static final String TAG = Log.tag(AddGroupDetailsRepository.class);

  private final Context context;

  AddGroupDetailsRepository(@NonNull Context context) {
    this.context = context;
  }

  void resolveMembers(@NonNull Collection<RecipientId> recipientIds, Consumer<List<GroupMemberEntry.NewGroupCandidate>> consumer) {
    SignalExecutors.BOUNDED.execute(() -> {
      List<GroupMemberEntry.NewGroupCandidate> members = new ArrayList<>(recipientIds.size());

      for (RecipientId id : recipientIds) {
        members.add(new GroupMemberEntry.NewGroupCandidate(Recipient.resolved(id)));
      }

      consumer.accept(members);
    });
  }

  void createGroup(@NonNull Set<RecipientId> members,
                   @Nullable byte[] avatar,
                   @Nullable String name,
                   boolean mms,
                   @Nullable Integer disappearingMessagesTimer,
                   Consumer<GroupCreateResult> resultConsumer)
  {
    SignalExecutors.BOUNDED.execute(() -> {
      Set<Recipient> recipients = new HashSet<>(Stream.of(members).map(Recipient::resolved).toList());

      try {
        GroupManager.GroupActionResult result = GroupManager.createGroup(context,
                                                                         recipients,
                                                                         avatar,
                                                                         name,
                                                                         mms,
                                                                         disappearingMessagesTimer != null ? disappearingMessagesTimer
                                                                                                           : SignalStore.settings().getUniversalExpireTimer());

        resultConsumer.accept(GroupCreateResult.success(result));
      } catch (GroupChangeBusyException e) {
        resultConsumer.accept(GroupCreateResult.error(GroupCreateResult.Error.Type.ERROR_BUSY));
      } catch (GroupChangeException e) {
        resultConsumer.accept(GroupCreateResult.error(GroupCreateResult.Error.Type.ERROR_FAILED));
      } catch (IOException e) {
        resultConsumer.accept(GroupCreateResult.error(GroupCreateResult.Error.Type.ERROR_IO));
      }
    });
  }
}
