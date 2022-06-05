package asia.coolapp.chat.groups.ui.chooseadmin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import asia.coolapp.chat.groups.GroupChangeException;
import asia.coolapp.chat.groups.GroupId;
import asia.coolapp.chat.groups.GroupManager;
import asia.coolapp.chat.groups.ui.GroupChangeFailureReason;
import asia.coolapp.chat.groups.ui.GroupChangeResult;
import asia.coolapp.chat.recipients.RecipientId;

import java.io.IOException;
import java.util.List;

public final class ChooseNewAdminRepository {
  private final Application context;

  ChooseNewAdminRepository(@NonNull Application context) {
    this.context = context;
  }

  @WorkerThread
  @NonNull GroupChangeResult updateAdminsAndLeave(@NonNull GroupId.V2 groupId, @NonNull List<RecipientId> newAdminIds) {
    try {
      GroupManager.addMemberAdminsAndLeaveGroup(context, groupId, newAdminIds);
      return GroupChangeResult.SUCCESS;
    } catch (GroupChangeException | IOException e) {
      return GroupChangeResult.failure(GroupChangeFailureReason.fromException(e));
    }
  }
}
